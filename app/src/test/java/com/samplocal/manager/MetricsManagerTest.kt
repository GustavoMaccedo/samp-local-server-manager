package com.samplocal.manager

import com.samplocal.manager.core.MetricsManager
import com.samplocal.manager.core.ProcReader
import com.samplocal.manager.core.ProcStat
import org.junit.Assert.*
import org.junit.Test

class MetricsManagerTest {

    private class FakeProc(
        var ticks: Long = 1000L,
        var rss: Long = 204800L,
        var pids: List<Int> = listOf(4242),
        var cmd: String = "/app/files/servers/TEDTE/samp03svr"
    ) : ProcReader {
        override fun listPids(): List<Int> = pids
        override fun cmdline(pid: Int): String? = if (pid in pids) cmd else null
        override fun procStat(pid: Int): ProcStat? =
            if (pid in pids) ProcStat(ticks, ticks / 4) else null
        override fun rssKb(pid: Int): Long? = if (pid in pids) rss else null
    }

    @Test
    fun findsPidByMarker() {
        val m = MetricsManager(FakeProc(), clockMs = { 0L })
        assertEquals(4242, m.findPid("servers/TEDTE/samp03svr"))
        assertNull(m.findPid("servers/OUTRO/samp03svr"))
    }

    @Test
    fun firstSampleIsZeroNeverFake() {
        val m = MetricsManager(FakeProc(), clockMs = { 60_000L })
        val p = m.sample(4242)!!
        assertEquals(0f, p.cpuPct)
        assertEquals(200f, p.rssMb, 0.01f)
    }

    @Test
    fun cpuFromRealDelta() {
        var now = 60_000L
        val proc = FakeProc(ticks = 1000L)
        val m = MetricsManager(proc, clockMs = { now })
        m.sample(4242)

        proc.ticks = 1100L
        now = 62_000L
        val p = m.sample(4242)!!
        assertEquals(62.5f, p.cpuPct, 0.01f)
    }

    @Test
    fun historyKeepsLast60() {
        var now = 0L
        val m = MetricsManager(FakeProc(), clockMs = { now })
        repeat(80) {
            m.sample(4242)
            now += 2000
        }
        assertEquals(60, m.history.size)
    }

    @Test
    fun unknownPidYieldsNull() {
        val m = MetricsManager(FakeProc(pids = emptyList()), clockMs = { 0L })
        assertNull(m.sample(9999))
        assertNull(m.findPid("x"))
    }

    @Test
    fun uptimeFormat() {
        assertEquals("00:00:00", com.samplocal.manager.ui.viewmodel.DashboardViewModel.formatUptime(0))
        assertEquals("00:00:07", com.samplocal.manager.ui.viewmodel.DashboardViewModel.formatUptime(7))
        assertEquals("02:31:48", com.samplocal.manager.ui.viewmodel.DashboardViewModel.formatUptime(2 * 3600 + 31 * 60 + 48))
        assertEquals("27:00:00", com.samplocal.manager.ui.viewmodel.DashboardViewModel.formatUptime(27 * 3600))
    }

    @Test
    fun versionParsedFromRealLog() {
        val dir = createTempDir("srvlog")
        try {
            java.io.File(dir, "server_log.txt").writeText(
                "----------\nSA-MP Dedicated Server\n----------------------\nv0.3.7-R2, (C)2005-2015 SA-MP Team\n"
            )
            assertEquals(
                "0.3.7-R2",
                com.samplocal.manager.ui.viewmodel.DashboardViewModel.parseServerVersion(dir)
            )
            assertNull(com.samplocal.manager.ui.viewmodel.DashboardViewModel.parseServerVersion(createTempDir("empty")))
        } finally {
            dir.deleteRecursively()
        }
    }
}
