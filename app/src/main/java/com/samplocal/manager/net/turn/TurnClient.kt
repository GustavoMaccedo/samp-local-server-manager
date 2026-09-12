package com.samplocal.manager.net.turn

import com.samplocal.manager.net.stun.DatagramTransport
import com.samplocal.manager.net.stun.StunClient
import com.samplocal.manager.net.stun.UdpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class TurnConfig(
    val host: String,
    val port: Int = 3478,
    val username: String,
    val password: String,
    val realm: String = ""
)

data class TurnAllocation(val relayedIp: InetAddress, val relayedPort: Int, val lifetimeSec: Int)

sealed interface AllocateOutcome {
    data class NeedAuth(val realm: String, val nonce: ByteArray) : AllocateOutcome
    data class Allocated(val allocation: TurnAllocation) : AllocateOutcome
    data class Error(val code: Int, val reason: String) : AllocateOutcome
}

class TurnClient(
    private val transport: UdpTransport = DatagramTransport,
    private val clockMs: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) {
    companion object {
        const val ALLOCATE = 0x0003
        const val ALLOCATE_SUCCESS = 0x0103
        const val ALLOCATE_ERROR = 0x0113
        const val REFRESH = 0x0004
        const val CREATE_PERMISSION = 0x0008
        const val SEND_INDICATION = 0x0016
        const val ATTR_USERNAME = 0x0006
        const val ATTR_MESSAGE_INTEGRITY = 0x0008
        const val ATTR_ERROR_CODE = 0x0009
        const val ATTR_REALM = 0x0014
        const val ATTR_NONCE = 0x0015
        const val ATTR_REQUESTED_TRANSPORT = 0x0019
        const val ATTR_LIFETIME = 0x000D
        const val ATTR_XOR_RELAYED = 0x0016
        const val ATTR_XOR_PEER = 0x0012
        const val ATTR_DATA = 0x0013
        const val DEFAULT_LIFETIME = 600

        fun newTxId(): ByteArray = ByteArray(12).also { SecureRandom().nextBytes(it) }

        fun hmacSha1(key: ByteArray, msg: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(key, "HmacSHA1"))
            return mac.doFinal(msg)
        }

        fun md5key(user: String, realm: String, pass: String): ByteArray {
            val md = java.security.MessageDigest.getInstance("MD5")
            return md.digest("$user:$realm:$pass".toByteArray(Charsets.UTF_8))
        }

        fun attr(type: Int, value: ByteArray): ByteArray {
            val pad = (-value.size % 4 + 4) % 4
            val b = ByteBuffer.allocate(4 + value.size + pad).order(ByteOrder.BIG_ENDIAN)
            b.putShort(type.toShort())
            b.putShort(value.size.toShort())
            b.put(value)
            repeat(pad) { b.put(0) }
            return b.array()
        }

        fun buildAllocate(
            txid: ByteArray,
            user: String? = null,
            realm: String? = null,
            nonce: ByteArray? = null,
            key: ByteArray? = null,
            lifetimeSec: Int = DEFAULT_LIFETIME
        ): ByteArray {
            require(txid.size == 12)
            var attrs = attr(ATTR_REQUESTED_TRANSPORT, byteArrayOf(17, 0, 0, 0))
            val lt = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(lifetimeSec).array()
            attrs += attr(ATTR_LIFETIME, lt)
            if (user != null && realm != null && nonce != null && key != null) {
                attrs += attr(ATTR_USERNAME, user.toByteArray(Charsets.UTF_8))
                attrs += attr(ATTR_REALM, realm.toByteArray(Charsets.UTF_8))
                attrs += attr(ATTR_NONCE, nonce)
                val head = header(ALLOCATE, attrs.size + 24, txid)
                val mi = hmacSha1(key, head + attrs)
                attrs += attr(ATTR_MESSAGE_INTEGRITY, mi)
            }
            return header(ALLOCATE, attrs.size, txid) + attrs
        }

        fun buildRefresh(txid: ByteArray, user: String, realm: String, nonce: ByteArray, key: ByteArray): ByteArray =
            buildAllocate(txid, user, realm, nonce, key)

        fun buildCreatePermission(
            txid: ByteArray, user: String, realm: String, nonce: ByteArray, key: ByteArray,
            peer: InetAddress
        ): ByteArray {
            val b = peer.address
            require(b.size == 4) { "relay IPv4 apenas nesta versao" }
            var attrs = attr(ATTR_XOR_PEER, peerAttrValue(b, 0, txid, forRelay = false))
            attrs += attr(ATTR_USERNAME, user.toByteArray(Charsets.UTF_8))
            attrs += attr(ATTR_REALM, realm.toByteArray(Charsets.UTF_8))
            attrs += attr(ATTR_NONCE, nonce)
            val head = header(CREATE_PERMISSION, attrs.size + 24, txid)
            attrs += attr(ATTR_MESSAGE_INTEGRITY, hmacSha1(key, head + attrs))
            return header(CREATE_PERMISSION, attrs.size, txid) + attrs
        }

        fun buildSend(peer: InetAddress, peerPort: Int, txid: ByteArray, payload: ByteArray): ByteArray {
            val b = peer.address
            require(b.size == 4)

            val attrs = attr(ATTR_XOR_PEER, peerAttrValue(b, peerPort, txid, forRelay = false)) +
                attr(ATTR_DATA, payload)
            return header(SEND_INDICATION, attrs.size, txid) + attrs
        }

        private fun peerAttrValue(ip4: ByteArray, port: Int, txid: ByteArray, forRelay: Boolean): ByteArray {
            val out = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            out.put(0)
            out.put(0x01)
            out.putShort((port xor (StunClient.COOKIE ushr 16)).toShort())
            val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(StunClient.COOKIE).array()
            out.put(ByteArray(4) { i -> (ip4[i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() })
            return out.array()
        }

        private fun header(type: Int, len: Int, txid: ByteArray): ByteArray {
            val h = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
            h.putShort(type.toShort())
            h.putShort(len.toShort())
            h.putInt(StunClient.COOKIE)
            h.put(txid)
            return h.array()
        }

        fun parseAllocateResponse(data: ByteArray, txid: ByteArray): AllocateOutcome? {
            if (data.size < 20) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val type = buf.short.toInt() and 0xFFFF
            buf.short
            if (buf.int != StunClient.COOKIE) return null
            val rx = ByteArray(12)
            buf.get(rx)
            if (!rx.contentEquals(txid)) return null
            var off = 20
            var realm: String? = null
            var nonce: ByteArray? = null
            var relayed: TurnAllocation? = null
            var errCode = 0
            var errReason = ""
            while (off + 4 <= data.size) {
                val bb = ByteBuffer.wrap(data, off, data.size - off).order(ByteOrder.BIG_ENDIAN)
                val at = bb.short.toInt() and 0xFFFF
                val al = bb.short.toInt() and 0xFFFF
                if (off + 4 + al > data.size) break
                val v = data.copyOfRange(off + 4, off + 4 + al)
                when (at) {
                    ATTR_REALM -> realm = String(v, Charsets.UTF_8)
                    ATTR_NONCE -> nonce = v
                    ATTR_XOR_RELAYED -> relayed = decodeXorRelayed(v, txid)
                    ATTR_ERROR_CODE -> if (v.size >= 4) {
                        errCode = (v[2].toInt() and 0xFF) * 100 + (v[3].toInt() and 0xFF)
                        errReason = String(v.copyOfRange(4, v.size), Charsets.UTF_8)
                    }
                }
                off += 4 + al + (-al % 4 + 4) % 4
            }
            return when (type) {
                ALLOCATE_SUCCESS -> relayed?.let { AllocateOutcome.Allocated(it) }
                ALLOCATE_ERROR -> if (errCode == 401 && realm != null && nonce != null) {
                    AllocateOutcome.NeedAuth(realm, nonce)
                } else AllocateOutcome.Error(errCode, errReason)
                else -> null
            }
        }

        private fun decodeXorRelayed(v: ByteArray, txid: ByteArray): TurnAllocation? {
            if (v.size < 8 || v[1].toInt() != 0x01) return null
            val port = ((v[2].toInt() and 0xFF) shl 8 or (v[3].toInt() and 0xFF)) xor (StunClient.COOKIE ushr 16)
            val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(StunClient.COOKIE).array()
            val ip = ByteArray(4) { i -> (v[4 + i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() }
            return TurnAllocation(InetAddress.getByAddress(ip), port, DEFAULT_LIFETIME)
        }
    }

    suspend fun allocate(cfg: TurnConfig, timeoutMs: Int = 5000): Result<TurnAllocation> =
        withContext(Dispatchers.IO) {
            try {

                var txid = newTxId()
                var resp = transport.exchange(cfg.host, cfg.port, buildAllocate(txid), timeoutMs)
                var out = parseAllocateResponse(resp, txid)
                    ?: return@withContext Result.failure(IllegalStateException("resposta TURN invalida"))
                if (out is AllocateOutcome.NeedAuth) {
                    val key = md5key(cfg.username, out.realm, cfg.password)
                    txid = newTxId()
                    val req = buildAllocate(txid, cfg.username, out.realm, out.nonce, key)
                    resp = transport.exchange(cfg.host, cfg.port, req, timeoutMs)
                    out = parseAllocateResponse(resp, txid)
                        ?: return@withContext Result.failure(IllegalStateException("resposta TURN invalida (auth)"))
                }
                when (out) {
                    is AllocateOutcome.Allocated -> Result.success(out.allocation)
                    is AllocateOutcome.Error -> Result.failure(
                        IllegalStateException("TURN ${out.code} ${out.reason}")
                    )
                    is AllocateOutcome.NeedAuth -> Result.failure(
                        IllegalStateException("TURN exigiu nova autenticacao")
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
