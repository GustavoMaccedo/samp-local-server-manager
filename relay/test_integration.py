
import hashlib
import hmac
import json
import secrets
import socket
import struct
import subprocess
import sys
import time

MAGIC = b"SLR1"

def mac(key, body):
    return hmac.new(key, body, hashlib.sha256).digest()[:16]

def recv(s, timeout=5):
    s.settimeout(timeout)
    return s.recvfrom(65535)

def main():
    relay = subprocess.Popen(
        [sys.executable, "server.py", "--port", "17779", "--session-base", "31000"],
        stdout=subprocess.PIPE,
    )
    try:
        time.sleep(1.0)
        R = ("127.0.0.1", 17779)
        sid = secrets.token_bytes(16)
        token = secrets.token_bytes(32)

        android = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        android.bind(("127.0.0.1", 0))
        android.settimeout(5)

        body = MAGIC + bytes([0x01]) + sid + token + struct.pack("!HH", 7777, 90)
        android.sendto(body + mac(token, body), R)
        data, _ = recv(android)
        assert data[4] == 0x02, f"esperava REGISTERED, veio {data[4]}"
        assert data[5:21] == sid
        sport = struct.unpack("!H", data[21:23])[0]
        assert 31000 <= sport < 35000, sport
        assert mac(token, data[:-16]) == data[-16:]
        print(f"1. REGISTER ok, porta da sessao={sport}")

        samp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        samp.bind(("127.0.0.1", 0))
        samp_port = samp.getsockname()[1]

        player = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        player.bind(("127.0.0.1", 0))
        player.settimeout(5)
        player.sendto(b"SAMP?", ("127.0.0.1", sport))
        data, src = recv(android)
        assert data[:4] == MAGIC and data[4] == 0x11 and data[5:21] == sid, "tunel R2A"
        assert mac(token, data[:-16]) == data[-16:], "HMAC tunel"
        assert data[25:29] == socket.inet_aton("127.0.0.1"), "ip do jogador"
        assert struct.unpack("!H", data[29:31])[0] == player.getsockname()[1], "porta do jogador"
        payload = data[31:-16]
        assert payload == b"SAMP?", payload
        print("2. player->relay->android ok (HMAC valido)")

        samp.sendto(payload, ("127.0.0.1", samp_port))
        samp.settimeout(5)
        echo, _ = samp.recvfrom(65535)

        seq = struct.pack("!I", 7)
        pip = socket.inet_aton("127.0.0.1")
        pport = struct.pack("!H", player.getsockname()[1])
        out = MAGIC + bytes([0x10]) + sid + seq + pip + pport + echo
        android.sendto(out + mac(token, out), R)
        data, _ = recv(player)
        assert data == b"SAMP?", data
        print("3. android->relay->player ok (ida e volta completa)")

        bad = bytearray(out)
        bad[30] ^= 0xFF
        android.sendto(bytes(bad) + mac(token, bytes(bad))[:0] + b"\x00" * 16, R)
        player.settimeout(1.0)
        try:
            d, _ = player.recvfrom(65535)
            raise SystemExit(f"FALHA: pacote falsificado entregue: {d!r}")
        except socket.timeout:
            print("4. HMAC falsificado descartado ok")

        loner = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        loner.bind(("127.0.0.1", 0))
        loner.settimeout(1.0)
        loner.sendto(b"oi?", ("127.0.0.1", 30999))
        try:
            android.settimeout(1.0)
            d, _ = android.recvfrom(65535)
            raise SystemExit(f"FALHA: porta sem sessao encaminhou: {d!r}")
        except socket.timeout:
            print("5. porta sem sessao ignorada ok")

        t0 = int(time.time() * 1000)
        ping = MAGIC + bytes([0x20]) + sid + struct.pack("!I", 3) + struct.pack("!Q", t0)
        android.sendto(ping + mac(token, ping), R)
        android.settimeout(5)
        data, _ = recv(android)
        assert data[4] == 0x21
        rtt = int(time.time() * 1000) - t0
        assert rtt < 2000, rtt
        print(f"6. heartbeat ok RTT={rtt}ms")

        st = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        st.sendto(bytes([0x30]), R)
        data, _ = recv(st)
        info = json.loads(data.decode())
        assert info["ok"] and info["sessions"] >= 1
        assert "token" not in data.decode()
        print(f"7. STATUS ok sessoes={info['sessions']}")

        print("INTEGRACAO RELAY: TUDO OK")
    finally:
        relay.terminate()
        relay.wait(timeout=5)

if __name__ == "__main__":
    main()
