package com.samplocal.manager.net.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

data class HeartbeatStats(
    val lastRttMs: Long? = null,
    val lossPct: Float? = null,
    val jitterMs: Float? = null,
    val lastHeartbeatMs: Long = 0L
) {
    companion object {

        fun compute(rtts: List<Long>, sent: Int): HeartbeatStats {
            if (rtts.isEmpty()) return HeartbeatStats()
            val loss = ((sent - rtts.size).coerceAtLeast(0) * 100f / sent.coerceAtLeast(1))
            var jitter = 0f
            for (i in 1 until rtts.size) jitter += abs(rtts[i] - rtts[i - 1]).toFloat()
            if (rtts.size > 1) jitter /= (rtts.size - 1)
            return HeartbeatStats(rtts.last(), loss, jitter, System.currentTimeMillis())
        }
    }
}

enum class SessionState { IDLE, REGISTERING, ACTIVE, DEGRADED, RECONNECTING, CLOSED, FAILED }

class RelaySessionManager(
    private val relayHost: String,
    private val relayPort: Int,
    private val sessionId: ByteArray,
    private val token: ByteArray,
    private val sampPort: Int,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val heartbeatPeriodMs: Long = 15_000L
) {
    @Volatile var state: SessionState = SessionState.IDLE
        private set
    @Volatile var sessionPort: Int = 0
        private set
    @Volatile var stats: HeartbeatStats = HeartbeatStats()
        private set

    val hbSent = AtomicLong(0)
    val hbAcked = AtomicLong(0)

    private var socket: DatagramSocket? = null
    private var jobs = listOf<Job>()
    private val rtts = ArrayDeque<Long>()
    private var seq = 0

    fun socket(): DatagramSocket? = socket

    suspend fun register(timeoutMs: Int = 8000): Result<Int> = withContext(Dispatchers.IO) {
        state = SessionState.REGISTERING
        try {
            val s = DatagramSocket()
            s.soTimeout = timeoutMs
            socket = s
            val relay = InetAddress.getByName(relayHost)
            val req = RelayProtocol.buildRegister(sessionId, token, sampPort)
            var attempt = 0
            var lastErr: Exception? = null
            while (attempt < 3) {
                try {
                    s.send(DatagramPacket(req, req.size, relay, relayPort))
                    val buf = ByteArray(256)
                    val pkt = DatagramPacket(buf, buf.size)
                    s.receive(pkt)
                    val reg = RelayProtocol.parseRegistered(
                        pkt.data.copyOf(pkt.length), sessionId, token
                    ) ?: throw IllegalStateException("resposta de registro invalida")
                    sessionPort = reg.sessionPort
                    state = SessionState.ACTIVE
                    return@withContext Result.success(reg.sessionPort)
                } catch (e: Exception) {
                    lastErr = e
                    attempt++
                }
            }
            state = SessionState.FAILED
            Result.failure(lastErr ?: IllegalStateException("registro sem resposta"))
        } catch (e: Exception) {
            state = SessionState.FAILED
            Result.failure(e)
        }
    }

    fun startHeartbeat(scope: CoroutineScope) {
        if (jobs.any { it.isActive }) return
        jobs = listOf(scope.launch(Dispatchers.IO) {
            var backoff = heartbeatPeriodMs
            while (isActive) {
                delay(backoff)
                val ok = pingOnce()
                if (ok) {
                    backoff = heartbeatPeriodMs
                    if (state == SessionState.DEGRADED || state == SessionState.RECONNECTING) {
                        state = SessionState.ACTIVE
                    }
                } else {
                    if (state == SessionState.ACTIVE) state = SessionState.DEGRADED

                    val r = register()
                    state = if (r.isSuccess) SessionState.ACTIVE else SessionState.RECONNECTING
                    backoff = (backoff * 2).coerceAtMost(120_000L)
                }
            }
        })
    }

    fun pingOnce(timeoutMs: Int = 5000): Boolean {
        return try {
            val s = socket ?: return false
            val relay = InetAddress.getByName(relayHost)
            val q = ++seq
            val t0 = clockMs()
            val req = RelayProtocol.buildPing(sessionId, token, q, t0)
            s.send(DatagramPacket(req, req.size, relay, relayPort))
            s.soTimeout = timeoutMs
            val buf = ByteArray(256)
            val pkt = DatagramPacket(buf, buf.size)
            s.receive(pkt)
            val pong = RelayProtocol.parsePong(pkt.data.copyOf(pkt.length), sessionId, token)
                ?: return false
            if (pong.seq != q) return false
            hbSent.incrementAndGet()
            hbAcked.incrementAndGet()
            rtts.addLast(clockMs() - t0)
            while (rtts.size > 20) rtts.removeFirst()
            stats = HeartbeatStats.compute(rtts.toList(), hbSent.get().toInt())
            true
        } catch (_: Exception) {
            hbSent.incrementAndGet()
            stats = HeartbeatStats.compute(rtts.toList(), hbSent.get().toInt())
            false
        }
    }

    fun close() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        try {
            val s = socket
            if (s != null && !s.isClosed && state == SessionState.ACTIVE) {
                val relay = InetAddress.getByName(relayHost)
                val req = RelayProtocol.buildClose(sessionId, token)
                s.send(DatagramPacket(req, req.size, relay, relayPort))
            }
        } catch (_: Exception) { }
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        state = SessionState.CLOSED
    }
}
