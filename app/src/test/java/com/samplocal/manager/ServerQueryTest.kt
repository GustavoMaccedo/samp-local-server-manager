package com.samplocal.manager

import com.samplocal.manager.samp.ServerQueryManager
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ServerQueryTest {

    private fun cannedInfo(): ByteArray {
        val host = "TEDTE".toByteArray(Charsets.UTF_8)
        val gm = "GM test".toByteArray(Charsets.UTF_8)
        val lang = "Portuguese".toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(64 + host.size + gm.size + lang.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("SAMP".toByteArray())
        buf.put(byteArrayOf(127, 0, 0, 1))
        buf.put((7777 and 0xFF).toByte())
        buf.put(((7777 shr 8) and 0xFF).toByte())
        buf.put('i'.code.toByte())
        buf.put(1)
        buf.putShort(23)
        buf.putShort(50)
        buf.putInt(host.size); buf.put(host)
        buf.putInt(gm.size); buf.put(gm)
        buf.putInt(lang.size); buf.put(lang)
        return buf.array().copyOf(buf.position())
    }

    @Test
    fun requestBytesAreExact() {
        val req = ServerQueryManager.buildInfoRequest(InetAddress.getByName("127.0.0.1"), 7777)
        assertEquals(11, req.size)
        assertEquals("SAMP", String(req, 0, 4))
        assertArrayEquals(byteArrayOf(127, 0, 0, 1), req.copyOfRange(4, 8))
        assertEquals(0x61, req[8].toInt() and 0xFF)
        assertEquals(0x1E, req[9].toInt() and 0xFF)
        assertEquals('i'.code.toByte(), req[10])
    }

    @Test
    fun parsesRealInfoResponse() {
        val info = ServerQueryManager.parseInfoResponse(cannedInfo())
        assertTrue(info.passworded)
        assertEquals(23, info.players)
        assertEquals(50, info.maxPlayers)
        assertEquals("TEDTE", info.hostname)
        assertEquals("GM test", info.gamemode)
        assertEquals("Portuguese", info.language)
    }

    @Test
    fun latin1HostnameDecoded() {

        val name = byteArrayOf('S'.code.toByte(), 0xE3.toByte(), 'o'.code.toByte())
        val buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("SAMP".toByteArray())
        buf.put(byteArrayOf(127, 0, 0, 1))
        buf.putShort(7777)
        buf.put('i'.code.toByte())
        buf.put(0); buf.putShort(0); buf.putShort(10)
        buf.putInt(name.size); buf.put(name)
        buf.putInt(0); buf.putInt(0)
        val info = ServerQueryManager.parseInfoResponse(buf.array().copyOf(buf.position()))
        assertEquals("S\u00E3o", info.hostname)
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedResponseRejected() {
        ServerQueryManager.parseInfoResponse(ByteArray(11) { 'S'.code.toByte() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun badSignatureRejected() {
        val bad = cannedInfo()
        bad[0] = 'X'.code.toByte()
        ServerQueryManager.parseInfoResponse(bad)
    }
}
