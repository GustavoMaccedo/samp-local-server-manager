package com.samplocal.manager.net.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

class UdpBridge(
    private val relayHost: String,
    private val relayPort: Int,
    private val token: ByteArray,
    private val sessionId: ByteArray,
    private val sampPort: Int,
    private val maxPps: Int = 500,
    private val maxBps: Long = 1_000_000L
) {
    val bytesUp = AtomicLong(0)
    val bytesDown = AtomicLong(0)
    val pktsUp = AtomicLong(0)
    val pktsDown = AtomicLong(0)

    @Volatile private var seq = 0
    private var jobs = listOf<Job>()
    private val running = java.util.concurrent.atomic.AtomicBoolean(false)
    private var relayAddr: InetAddress? = null
    private var tunnel: DatagramSocket? = null
    private var windowStart = System.currentTimeMillis()
    private var windowBytes = 0L
    private var windowPkts = 0

    fun start(scope: CoroutineScope, tunnelSocket: DatagramSocket) {
        if (!running.compareAndSet(false, true)) return
        tunnel = tunnelSocket
        relayAddr = InetAddress.getByName(relayHost)
        val loop = DatagramSocket()
        jobs = listOf(
            scope.launch(Dispatchers.IO) { pumpTunnelToSamp(loop) },
            scope.launch(Dispatchers.IO) { pumpSampToTunnel(loop) }
        )
    }

    fun stop() {
        running.set(false)
        jobs.forEach { it.cancel() }
        jobs = emptyList()
    }

    private suspend fun pumpTunnelToSamp(loop: DatagramSocket) {
        val tun = tunnel ?: return
        try {
            tun.soTimeout = 1000
            val buf = ByteArray(65535)
            while (running.get()) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    tun.receive(pkt)
                    val from = pkt.address ?: continue
                    if (from.hostAddress != relayAddr?.hostAddress) continue
                    val data = pkt.data.copyOf(pkt.length)
                    val msg = RelayProtocol.parseDataR2A(data, sessionId, token) ?: continue
                    if (!withinLimits(msg.payload.size)) continue
                    bytesDown.addAndGet(msg.payload.size.toLong())
                    pktsDown.incrementAndGet()
                    lastPlayer.set(msg.playerIp to msg.playerPort)
                    loop.send(DatagramPacket(msg.payload, msg.payload.size,
                        InetAddress.getByName("127.0.0.1"), sampPort))
                } catch (_: java.net.SocketTimeoutException) {
                    if (!running.get()) break
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        } catch (_: Exception) {

        } finally {
            try { loop.close() } catch (_: Exception) { }
        }
    }

    private suspend fun pumpSampToTunnel(loop: DatagramSocket) {
        val tun = tunnel ?: return
        val relay = InetAddress.getByName(relayHost)
        try {
            loop.soTimeout = 1000
            val buf = ByteArray(65535)
            while (running.get()) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    loop.receive(pkt)
                    if (pkt.address?.hostAddress != "127.0.0.1") continue
                    val last = lastPlayer.get() ?: continue
                    val payload = pkt.data.copyOf(pkt.length)
                    if (!withinLimits(payload.size)) continue
                    val s = ++seq
                    val out = RelayProtocol.buildDataA2R(
                        sessionId, token, s, last.first, last.second, payload
                    )
                    tun.send(DatagramPacket(out, out.size, relay, relayPort))
                    bytesUp.addAndGet(payload.size.toLong())
                    pktsUp.incrementAndGet()
                } catch (_: java.net.SocketTimeoutException) {
                    if (!running.get()) break
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        } catch (_: Exception) {

        }
    }

    private val lastPlayer = java.util.concurrent.atomic.AtomicReference<Pair<InetAddress, Int>?>(null)

    @Synchronized
    private fun withinLimits(n: Int): Boolean {
        val now = System.currentTimeMillis()
        if (now - windowStart > 1000) {
            windowStart = now
            windowBytes = 0
            windowPkts = 0
        }
        windowBytes += n
        windowPkts += 1
        return windowBytes <= maxBps && windowPkts <= maxPps
    }
}
