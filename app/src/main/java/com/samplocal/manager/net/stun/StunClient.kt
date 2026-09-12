package com.samplocal.manager.net.stun

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.zip.CRC32

data class StunResult(val ip: InetAddress, val port: Int, val rttMs: Long)

interface UdpTransport {
    fun exchange(host: String, port: Int, req: ByteArray, timeoutMs: Int): ByteArray
}

object DatagramTransport : UdpTransport {
    override fun exchange(host: String, port: Int, req: ByteArray, timeoutMs: Int): ByteArray {
        val addr = InetAddress.getByName(host)
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            socket.send(DatagramPacket(req, req.size, addr, port))
            val buf = ByteArray(2048)
            val pkt = DatagramPacket(buf, buf.size)
            socket.receive(pkt)
            return pkt.data.copyOf(pkt.length)
        }
    }
}

class StunClient(
    private val transport: UdpTransport = DatagramTransport,
    private val clockMs: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) {
    companion object {
        const val DEFAULT_HOST = "stun.l.google.com"
        const val DEFAULT_PORT = 19302
        const val ALT_HOST = "stun1.l.google.com"
        const val COOKIE = 0x2112A442
        const val BINDING_REQUEST = 0x0001
        const val BINDING_SUCCESS = 0x0101
        const val ATTR_XOR_MAPPED = 0x0020
        const val ATTR_MAPPED = 0x0001
        const val ATTR_SOFTWARE = 0x8022
        const val ATTR_FINGERPRINT = 0x8028
        const val FINGERPRINT_XOR = 0x5354554E

        fun newTxId(): ByteArray = ByteArray(12).also { SecureRandom().nextBytes(it) }

        fun buildBindingRequest(txid: ByteArray, software: String = "samp-local"): ByteArray {
            require(txid.size == 12)
            val name = software.toByteArray(Charsets.UTF_8)
            val attrLen = 4 + name.size + (-name.size % 4 + 4) % 4
            val totalWithoutFp = 20 + attrLen
            val head = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
            head.putShort(BINDING_REQUEST.toShort())
            head.putShort((attrLen + 8).toShort())
            head.putInt(COOKIE)
            head.put(txid)
            val hb = head.array()
            val withAttr = hb + attr(ATTR_SOFTWARE, name)
            val crc = CRC32()
            crc.update(withAttr)
            val fp = (crc.value xor FINGERPRINT_XOR.toLong()).toInt()
            val fpAttr = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            fpAttr.putShort(ATTR_FINGERPRINT.toShort())
            fpAttr.putShort(4)
            fpAttr.putInt(fp)
            val full = withAttr + fpAttr.array()

            val len = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            len.putShort((full.size - 20).toShort())
            len.array().copyInto(full, 2)
            return full
        }

        private fun attr(type: Int, value: ByteArray): ByteArray {
            val pad = (-value.size % 4 + 4) % 4
            val b = ByteBuffer.allocate(4 + value.size + pad).order(ByteOrder.BIG_ENDIAN)
            b.putShort(type.toShort())
            b.putShort(value.size.toShort())
            b.put(value)
            repeat(pad) { b.put(0) }
            return b.array()
        }

        fun parseBindingResponse(data: ByteArray, txid: ByteArray): StunResult? {
            if (data.size < 20) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            if (buf.short.toInt() and 0xFFFF != BINDING_SUCCESS) return null
            buf.short
            if (buf.int != COOKIE) return null
            val rxTx = ByteArray(12)
            buf.get(rxTx)
            if (!rxTx.contentEquals(txid)) return null
            var off = 20
            var mapped: Pair<InetAddress, Int>? = null
            var fpOk: Boolean? = null
            while (off + 4 <= data.size) {
                val bb = ByteBuffer.wrap(data, off, data.size - off).order(ByteOrder.BIG_ENDIAN)
                val type = bb.short.toInt() and 0xFFFF
                val len = bb.short.toInt() and 0xFFFF
                if (off + 4 + len > data.size) break
                val v = data.copyOfRange(off + 4, off + 4 + len)
                when (type) {
                    ATTR_XOR_MAPPED -> mapped = mapped ?: decodeXorMapped(v, txid)
                    ATTR_MAPPED -> if (mapped == null) mapped = decodeMapped(v)
                    ATTR_FINGERPRINT -> {
                        if (len == 4) {
                            val crc = CRC32()
                            crc.update(data, 0, off)
                            fpOk = (crc.value xor FINGERPRINT_XOR.toLong()).toInt() ==
                                ByteBuffer.wrap(v).order(ByteOrder.BIG_ENDIAN).int
                        }
                    }
                }
                off += 4 + len + (-len % 4 + 4) % 4
            }
            if (fpOk == false) return null
            val (ip, port) = mapped ?: return null
            return StunResult(ip, port, 0L)
        }

        private fun decodeXorMapped(v: ByteArray, txid: ByteArray): Pair<InetAddress, Int>? {
            if (v.size < 8) return null
            val port = ((v[2].toInt() and 0xFF) shl 8 or (v[3].toInt() and 0xFF)) xor (COOKIE ushr 16)
            return when (v[1].toInt()) {
                0x01 -> {
                    if (v.size < 8) return null
                    val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(COOKIE).array()
                    val ip = ByteArray(4) { i -> (v[4 + i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() }
                    Pair(InetAddress.getByAddress(ip), port)
                }
                0x02 -> {
                    if (v.size < 20) return null
                    val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(COOKIE).array() + txid
                    val ip = ByteArray(16) { i -> (v[4 + i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() }
                    Pair(InetAddress.getByAddress(ip), port)
                }
                else -> null
            }
        }

        private fun decodeMapped(v: ByteArray): Pair<InetAddress, Int>? {
            if (v.size < 8) return null
            val port = (v[2].toInt() and 0xFF) shl 8 or (v[3].toInt() and 0xFF)
            return when (v[1].toInt()) {
                0x01 -> Pair(InetAddress.getByAddress(v.copyOfRange(4, 8)), port)
                0x02 -> if (v.size >= 20) Pair(
                    InetAddress.getByAddress(v.copyOfRange(4, 20)), port
                ) else null
                else -> null
            }
        }
    }

    suspend fun discover(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        timeoutMs: Int = 4000
    ): Result<StunResult> = withContext(Dispatchers.IO) {
        try {
            val txid = newTxId()
            val req = buildBindingRequest(txid)
            var last: Exception? = null
            repeat(2) { attempt ->
                try {
                    val t0 = clockMs()
                    val resp = transport.exchange(host, port, req, timeoutMs)
                    val parsed = parseBindingResponse(resp, txid)
                        ?: throw IllegalStateException("resposta STUN invalida (tentativa ${attempt + 1})")
                    return@withContext Result.success(parsed.copy(rttMs = clockMs() - t0))
                } catch (e: Exception) {
                    last = e
                }
            }
            Result.failure(last ?: IllegalStateException("STUN sem resposta"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
