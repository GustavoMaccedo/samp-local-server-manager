package com.samplocal.manager

import com.samplocal.manager.net.stun.StunClient
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StunCodecTest {

    private val txid = ByteArray(12) { it.toByte() }

    @Test
    fun requestLayoutIsExact() {
        val req = StunClient.buildBindingRequest(txid, "t")
        assertEquals(0x0001, ByteBuffer.wrap(req).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF)
        assertEquals(req.size - 20, ByteBuffer.wrap(req, 2, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF)
        assertEquals(0x2112A442, ByteBuffer.wrap(req, 4, 4).order(ByteOrder.BIG_ENDIAN).int)
        assertArrayEquals(txid, req.copyOfRange(8, 20))

        val at = ByteBuffer.wrap(req, req.size - 8, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        assertEquals(0x8028, at)
    }

    private fun successWith(mappedAttr: ByteArray): ByteArray {
        val head = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        head.putShort(0x0101)
        head.putShort(mappedAttr.size.toShort())
        head.putInt(StunClient.COOKIE)
        head.put(txid)
        return head.array() + mappedAttr
    }

    private fun xorMappedAttr(ip: ByteArray, port: Int): ByteArray {
        val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(StunClient.COOKIE).array()
        val v = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        v.put(0); v.put(0x01)
        v.putShort((port xor (StunClient.COOKIE ushr 16)).toShort())
        v.put(ByteArray(4) { i -> (ip[i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() })
        val a = v.array()
        val h = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0020); h.putShort(8)
        return h.array() + a
    }

    @Test
    fun parsesXorMapped() {
        val resp = successWith(xorMappedAttr(byteArrayOf(200.toByte(), 1, 2, 3), 45678))
        val r = StunClient.parseBindingResponse(resp, txid)!!
        assertEquals("200.1.2.3", r.ip.hostAddress)
        assertEquals(45678, r.port)
    }

    @Test
    fun wrongTxidRejected() {
        val resp = successWith(xorMappedAttr(byteArrayOf(1, 2, 3, 4), 9999))
        assertNull(StunClient.parseBindingResponse(resp, ByteArray(12) { 9 }))
    }

    @Test
    fun errorResponseRejected() {
        val head = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        head.putShort(0x0111); head.putShort(0); head.putInt(StunClient.COOKIE); head.put(txid)
        assertNull(StunClient.parseBindingResponse(head.array(), txid))
    }

    @Test
    fun truncatedRejected() {
        assertNull(StunClient.parseBindingResponse(ByteArray(10), txid))
    }

    @Test
    fun tamperedFingerprintRejected() {
        val good = successWith(xorMappedAttr(byteArrayOf(9, 9, 9, 9), 1111))

        val fpBody = good + ByteArray(0)
        val crc = java.util.zip.CRC32()
        crc.update(fpBody)
        val fp = (crc.value xor 0x5354554EL).toInt()
        val fpa = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        fpa.putShort(0x8028.toShort()); fpa.putShort(4); fpa.putInt(fp)
        val withFp = fpBody + fpa.array()
        withFp[26] = (withFp[26] + 1).toByte()
        assertNull(StunClient.parseBindingResponse(withFp, txid))
    }

    @Test
    fun mappedFallbackParsed() {
        val v = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        v.put(0); v.put(0x01); v.putShort(7777); v.put(byteArrayOf(10, 0, 0, 5))
        val h = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0001); h.putShort(8)
        val resp = successWith(h.array() + v.array())
        val r = StunClient.parseBindingResponse(resp, txid)!!
        assertEquals("10.0.0.5", r.ip.hostAddress)
        assertEquals(7777, r.port)
    }
}
