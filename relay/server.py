
import argparse
import asyncio
import hashlib
import hmac
import json
import secrets
import struct
import time

MAGIC = b"SLR1"
T_REGISTER, T_REGISTERED = 0x01, 0x02
T_CLOSE = 0x03
T_DATA_A2R, T_DATA_R2A = 0x10, 0x11
T_PING, T_PONG = 0x20, 0x21
T_STATUS = 0x30
HMAC_LEN = 16
SESSION_TIMEOUT = 90.0

def mac(key: bytes, body: bytes) -> bytes:
    return hmac.new(key, body, hashlib.sha256).digest()[:HMAC_LEN]

class Session:
    def __init__(self, sid: bytes, token: bytes, android: tuple, samp_port: int, expires: int):
        self.sid = sid
        self.token = token
        self.android = android
        self.samp_port = samp_port
        self.expires = expires
        self.port = 0
        self.created = time.monotonic()
        self.last_seen = self.created
        self.seq_out = 0
        self.bytes_up = 0
        self.bytes_down = 0
        self.pkts_up = 0
        self.pkts_down = 0
        self.players = set()
        self.win_start = self.created
        self.win_bytes = 0
        self.win_pkts = 0

class Relay(asyncio.DatagramProtocol):
    def __init__(self, args):
        self.args = args
        self.sessions: dict[bytes, Session] = {}
        self.by_port: dict[int, Session] = {}
        self.next_port = args.session_base
        self.started = time.monotonic()
        self.transport = None
        self.total_rx = 0
        self.total_tx = 0

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data: bytes, addr):
        self.total_rx += len(data)
        try:
            if len(data) == 1 and data[0] == T_STATUS:
                self.send_status(addr)
            elif data[:4] == MAGIC:
                self.handle_tunnel(data, addr)
            else:
                self.handle_player(data, addr)
        except Exception:
            pass

    def handle_tunnel(self, data: bytes, addr):
        if len(data) < 4 + 1 + 16 + HMAC_LEN:
            return
        typ = data[4]
        sid = data[5:21]
        s = self.sessions.get(sid)
        if typ == T_REGISTER:
            self.do_register(data, addr)
            return
        if s is None:
            return
        body, tag = data[:-HMAC_LEN], data[-HMAC_LEN:]
        if not hmac.compare_digest(mac(s.token, body), tag):
            return
        s.last_seen = time.monotonic()
        s.android = addr
        if typ == T_DATA_A2R:
            if len(body) < 5 + 16 + 4 + 4 + 2:
                return
            seq = struct.unpack("!I", body[21:25])[0]
            pip = ".".join(map(str, body[25:29]))
            pport = struct.unpack("!H", body[29:31])[0]
            payload = body[31:]
            if self.over_limit(s, len(payload)):
                return
            s.bytes_up += len(payload)
            s.pkts_up += 1
            self.transport.sendto(payload, (pip, pport))
            self.total_tx += len(payload)
        elif typ == T_PING:
            if len(body) < 5 + 16 + 4 + 8:
                return
            seq = body[21:25]
            t_send = body[25:33]
            t_recv = struct.pack("!Q", int(time.time() * 1000))
            out = MAGIC + bytes([T_PONG]) + sid + seq + t_send + t_recv
            self.transport.sendto(out + mac(s.token, out), addr)
        elif typ == T_CLOSE:
            self.drop(s)

    def do_register(self, data: bytes, addr):
        if len(data) < 5 + 16 + 32 + 2 + 2 + HMAC_LEN:
            return
        sid = data[5:21]
        token = data[21:53]
        samp_port = struct.unpack("!H", data[53:55])[0]
        if not (1 <= samp_port <= 65535):
            return
        tag = data[-HMAC_LEN:]
        if not hmac.compare_digest(mac(token, data[:-HMAC_LEN]), tag):
            return
        self.expire()
        if len(self.sessions) >= self.args.max_sessions and sid not in self.sessions:
            return
        s = self.sessions.get(sid)
        if s is None:
            s = Session(sid, token, addr, samp_port, self.args.expires)
            s.port = self.alloc_port()
            if s.port == 0:
                return
            self.sessions[sid] = s
            self.by_port[s.port] = s
        else:
            if not hmac.compare_digest(s.token, token):
                return
            s.android = addr
            s.samp_port = samp_port
        s.last_seen = time.monotonic()
        out = MAGIC + bytes([T_REGISTERED]) + sid + struct.pack("!HH", s.port, s.expires)
        self.transport.sendto(out + mac(s.token, out), addr)
        try:
            asyncio.get_running_loop().create_task(self.open_player_socket(s))
        except RuntimeError:
            pass

    async def open_player_socket(self, s: Session):
        key = f"sock_{s.port}"
        if hasattr(self, key):
            return
        try:
            loop = asyncio.get_running_loop()
            t, _ = await loop.create_datagram_endpoint(
                lambda: PlayerSide(self, s), local_addr=("0.0.0.0", s.port)
            )
            setattr(self, key, t)
        except OSError:
            pass

    def handle_player(self, data: bytes, addr):
        s = getattr(self, "bound_session", None)
        if s is None or s.sid not in self.sessions:
            return
        if self.over_limit(s, len(data)):
            return
        if len(s.players) < 1000:
            s.players.add(addr)
        s.bytes_down += len(data)
        s.pkts_down += 1
        s.last_seen = time.monotonic()
        s.seq_out += 1
        pip = bytes(map(int, addr[0].split(".")))
        pport = struct.pack("!H", addr[1])
        out = MAGIC + bytes([T_DATA_R2A]) + s.sid + struct.pack("!I", s.seq_out) + pip + pport + data
        self.transport.sendto(out + mac(s.token, out), s.android)
        self.total_tx += len(out)

    def over_limit(self, s: Session, n: int) -> bool:
        now = time.monotonic()
        if now - s.win_start > 1.0:
            s.win_start = now
            s.win_bytes = 0
            s.win_pkts = 0
        s.win_bytes += n
        s.win_pkts += 1
        return s.win_bytes > self.args.max_bps or s.win_pkts > self.args.max_pps

    def alloc_port(self) -> int:
        for _ in range(self.args.max_sessions + 8):
            p = self.next_port
            self.next_port += 1
            if self.next_port >= self.args.session_base + 4000:
                self.next_port = self.args.session_base
            if p not in self.by_port:
                return p
        return 0

    def drop(self, s: Session):
        self.sessions.pop(s.sid, None)
        self.by_port.pop(s.port, None)

    def expire(self):
        now = time.monotonic()
        for sid, s in list(self.sessions.items()):
            if now - s.last_seen > SESSION_TIMEOUT:
                self.drop(s)

    def send_status(self, addr):
        self.expire()
        info = {
            "ok": True,
            "uptime_s": int(time.monotonic() - self.started),
            "sessions": len(self.sessions),
            "bytes_rx": self.total_rx,
            "bytes_tx": self.total_tx,
        }
        self.transport.sendto(json.dumps(info).encode(), addr)

class PlayerSide(asyncio.DatagramProtocol):

    def __init__(self, relay: Relay, session: Session):
        self.relay = relay
        self.session = session

    def datagram_received(self, data: bytes, addr):
        self.relay.bound_session = self.session
        try:
            self.relay.handle_player(data, addr)
        finally:
            self.relay.bound_session = None

async def main_async(args):
    loop = asyncio.get_running_loop()
    relay = Relay(args)
    await loop.create_datagram_endpoint(lambda: relay, local_addr=("0.0.0.0", args.port))
    print(f"relay ouvindo :{args.port} (sessoes {args.session_base}+)", flush=True)
    while True:
        await asyncio.sleep(5)
        relay.expire()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=7779)
    ap.add_argument("--session-base", type=int, default=30000)
    ap.add_argument("--max-sessions", type=int, default=256)
    ap.add_argument("--expires", type=int, default=90)
    ap.add_argument("--max-pps", type=int, default=500)
    ap.add_argument("--max-bps", type=int, default=1_000_000)
    main_async_args = ap.parse_args()
    asyncio.run(main_async(main_async_args))

if __name__ == "__main__":
    main()
