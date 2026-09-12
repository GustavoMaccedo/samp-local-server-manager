package com.samplocal.manager.samp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class ServerQueryManager {

    data class QueryInfo(
        val passworded: Boolean,
        val players: Int,
        val maxPlayers: Int,
        val hostname: String,
        val gamemode: String,
        val language: String
    )

    companion object {
        const val OPCODE_INFO: Byte = 'i'.code.toByte()
        const val OPCODE_PING: Byte = 'p'.code.toByte()
        private val SAMP = byteArrayOf('S'.code.toByte(), 'A'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte())
        private val TEXT = Charset.forName("windows-1252")

        fun buildInfoRequest(ip: InetAddress, port: Int): ByteArray {
            val buf = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(SAMP)
            buf.put(ip.address.copyOf(4))
            buf.put((port and 0xFF).toByte())
            buf.put(((port shr 8) and 0xFF).toByte())
            buf.put(OPCODE_INFO)
            return buf.array()
        }

        fun parseInfoResponse(data: ByteArray): QueryInfo {
            require(data.size >= 12) { "resposta curta" }
            require(data[0] == SAMP[0] && data[1] == SAMP[1] && data[2] == SAMP[2] && data[3] == SAMP[3]) {
                "assinatura invalida"
            }
            require(data[10] == OPCODE_INFO) { "opcode inesperado" }
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buf.position(11)
            fun need(n: Int) {
                if (buf.remaining() < n) throw IllegalArgumentException("resposta truncada")
            }
            need(1); val passworded = buf.get().toInt() != 0
            need(2); val players = buf.short.toInt() and 0xFFFF
            need(2); val maxPlayers = buf.short.toInt() and 0xFFFF
            fun text(max: Int): String {
                need(4)
                val len = buf.int
                require(len in 0..max) { "tamanho invalido: $len" }
                need(len)
                val bytes = ByteArray(len)
                buf.get(bytes)
                return String(bytes, TEXT)
            }
            val hostname = text(255)
            val gamemode = text(255)
            val language = text(255)
            return QueryInfo(passworded, players, maxPlayers, hostname, gamemode, language)
        }
    }

    suspend fun ping(host: String, port: Int, timeoutMs: Int = 2000): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val addr = InetAddress.getByName(host)
                val payload = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((System.currentTimeMillis() and 0xFFFFFFFFL).toInt()).array()
                val head = buildInfoRequest(addr, port).copyOf(10) +
                    byteArrayOf(OPCODE_PING) + payload
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMs
                    val t0 = android.os.SystemClock.elapsedRealtime()
                    socket.send(DatagramPacket(head, head.size, addr, port))
                    val buf = ByteArray(64)
                    val pkt = DatagramPacket(buf, buf.size)
                    socket.receive(pkt)
                    val resp = pkt.data.copyOf(pkt.length)
                    if (resp.size < 15 || resp[10] != OPCODE_PING) {
                        return@withContext Result.failure(IllegalStateException("eco invalido"))
                    }
                    if (!resp.copyOfRange(11, 15).contentEquals(payload)) {
                        return@withContext Result.failure(IllegalStateException("eco divergente"))
                    }
                    Result.success(android.os.SystemClock.elapsedRealtime() - t0)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun queryInfo(host: String, port: Int, timeoutMs: Int = 2000): Result<QueryInfo> =
        withContext(Dispatchers.IO) {
            try {
                val addr = InetAddress.getByName(host)
                val socket = DatagramSocket()
                try {
                    socket.soTimeout = timeoutMs
                    val req = buildInfoRequest(addr, port)
                    socket.send(DatagramPacket(req, req.size, addr, port))
                    val buf = ByteArray(4096)
                    val pkt = DatagramPacket(buf, buf.size)
                    socket.receive(pkt)
                    Result.success(parseInfoResponse(pkt.data.copyOf(pkt.length)))
                } finally {
                    socket.close()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
