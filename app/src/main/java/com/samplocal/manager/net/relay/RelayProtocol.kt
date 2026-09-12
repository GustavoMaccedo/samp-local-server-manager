package com.samplocal.manager.net.relay

import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object RelayProtocol {
    const val MAGIC = "SLR1"
    const val T_REGISTER = 0x01
    const val T_REGISTERED = 0x02
    const val T_CLOSE = 0x03
    const val T_DATA_A2R = 0x10
    const val T_DATA_R2A = 0x11
    const val T_PING = 0x20
    const val T_PONG = 0x21
    const val HMAC_LEN = 16

    fun newSessionId(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }
    fun newToken(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun hmac(token: ByteArray, body: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(token, "HmacSHA256"))
        return mac.doFinal(body).copyOf(HMAC_LEN)
    }

    fun verify(token: ByteArray, packet: ByteArray): Boolean {
        if (packet.size < 4 + 1 + 16 + HMAC_LEN) return false
        if (packet.copyOfRange(0, 4).toString(Charsets.US_ASCII) != MAGIC) return false
        val body = packet.copyOfRange(0, packet.size - HMAC_LEN)
        val tag = packet.copyOfRange(packet.size - HMAC_LEN, packet.size)
        return hmac(token, body).contentEquals(tag)
    }

    fun buildRegister(sid: ByteArray, token: ByteArray, sampPort: Int, expiresSec: Int = 90): ByteArray {
        require(sid.size == 16 && token.size == 32)
        val body = MAGIC.toByteArray() + byteArrayOf(T_REGISTER.toByte()) + sid + token +
            (ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(sampPort.toShort()).array()) +
            (ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(expiresSec.toShort()).array())
        return body + hmac(token, body)
    }

    data class Registered(val sessionPort: Int, val expiresSec: Int)

    fun parseRegistered(pkt: ByteArray, sid: ByteArray, token: ByteArray): Registered? {
        if (!verify(token, pkt) || pkt[4].toInt() != T_REGISTERED) return null
        if (!pkt.copyOfRange(5, 21).contentEquals(sid)) return null
        val bb = ByteBuffer.wrap(pkt, 21, 4).order(ByteOrder.BIG_ENDIAN)
        return Registered(bb.short.toInt() and 0xFFFF, bb.short.toInt() and 0xFFFF)
    }

    fun buildDataA2R(
        sid: ByteArray, token: ByteArray, seq: Int,
        playerIp: InetAddress, playerPort: Int, payload: ByteArray
    ): ByteArray {
        val ip4 = playerIp.address
        require(ip4.size == 4) { "relay IPv4 nesta versao" }
        val body = MAGIC.toByteArray() + byteArrayOf(T_DATA_A2R.toByte()) + sid +
            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(seq).array() +
            ip4 +
            ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(playerPort.toShort()).array() +
            payload
        return body + hmac(token, body)
    }

    data class DataR2A(val seq: Int, val playerIp: InetAddress, val playerPort: Int, val payload: ByteArray)

    fun buildDataR2A(
        sid: ByteArray, token: ByteArray, seq: Int,
        playerIp: InetAddress, playerPort: Int, payload: ByteArray
    ): ByteArray {
        val ip4 = playerIp.address
        require(ip4.size == 4)
        val body = MAGIC.toByteArray() + byteArrayOf(T_DATA_R2A.toByte()) + sid +
            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(seq).array() +
            ip4 +
            ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(playerPort.toShort()).array() +
            payload
        return body + hmac(token, body)
    }

    fun parseDataR2A(pkt: ByteArray, sid: ByteArray, token: ByteArray): DataR2A? {
        if (!verify(token, pkt) || pkt[4].toInt() != T_DATA_R2A) return null
        if (!pkt.copyOfRange(5, 21).contentEquals(sid)) return null
        if (pkt.size < 5 + 16 + 4 + 4 + 2 + HMAC_LEN) return null
        val bb = ByteBuffer.wrap(pkt, 21, 10).order(ByteOrder.BIG_ENDIAN)
        val seq = bb.int
        val ip = ByteArray(4)
        bb.get(ip)
        val port = bb.short.toInt() and 0xFFFF
        return DataR2A(seq, InetAddress.getByAddress(ip), port, pkt.copyOfRange(31, pkt.size - HMAC_LEN))
    }

    fun buildPing(sid: ByteArray, token: ByteArray, seq: Int, tSendMs: Long): ByteArray {
        val body = MAGIC.toByteArray() + byteArrayOf(T_PING.toByte()) + sid +
            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(seq).array() +
            ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(tSendMs).array()
        return body + hmac(token, body)
    }

    data class Pong(val seq: Int, val tSendMs: Long, val tRecvMs: Long)

    fun parsePong(pkt: ByteArray, sid: ByteArray, token: ByteArray): Pong? {
        if (!verify(token, pkt) || pkt[4].toInt() != T_PONG) return null
        if (!pkt.copyOfRange(5, 21).contentEquals(sid)) return null
        if (pkt.size < 5 + 16 + 4 + 8 + 8 + HMAC_LEN) return null
        val bb = ByteBuffer.wrap(pkt, 21, 20).order(ByteOrder.BIG_ENDIAN)
        return Pong(bb.int, bb.long, bb.long)
    }

    fun buildClose(sid: ByteArray, token: ByteArray): ByteArray {
        val body = MAGIC.toByteArray() + byteArrayOf(T_CLOSE.toByte()) + sid
        return body + hmac(token, body)
    }
}
