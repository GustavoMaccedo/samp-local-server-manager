package com.samplocal.manager

import com.samplocal.manager.data.model.GuestArchitecture
import com.samplocal.manager.data.model.HostArchitecture
import com.samplocal.manager.data.model.RuntimeBackend
import com.samplocal.manager.runtime.ArchitectureDetector
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ArchitectureDetectorTest {

    @Test
    fun hostDetectionNeverUnknownString() {
        val host = ArchitectureDetector.detectHostArchitecture()

        assertNotNull(host)
    }

    @Test
    fun x86_32OnArm64RequiresBundledQemu() {
        val backend = ArchitectureDetector.detectRequiredRuntime(HostArchitecture.ARM64, GuestArchitecture.X86_32)
        assertEquals(RuntimeBackend.QEMU_I386, backend)
    }

    @Test
    fun x86_64OnArm64RequiresBox64() {
        val backend = ArchitectureDetector.detectRequiredRuntime(HostArchitecture.ARM64, GuestArchitecture.X86_64)
        assertEquals(RuntimeBackend.BOX64, backend)
    }

    @Test
    fun unknownGuestUnsupported() {
        val backend = ArchitectureDetector.detectRequiredRuntime(HostArchitecture.ARM64, GuestArchitecture.UNKNOWN)
        assertEquals(RuntimeBackend.UNSUPPORTED, backend)
    }

    @Test
    fun detectsRealSampBinaryIfPresent() {
        val candidates = listOf(File("/root/teste/samp03svr"), File("/tmp/samp03svr"))
        val found = candidates.firstOrNull { it.exists() }
        if (found == null) return
        val info = ArchitectureDetector.detectBinaryArchitecture(found)
        assertTrue(info.isElf)
        assertEquals(GuestArchitecture.X86_32, info.guestArch)
        assertEquals(3, info.machine)
        assertEquals(1, info.elfClass)
    }

    @Test
    fun nonElfDetected() {
        val tmp = File.createTempFile("notelf", ".bin")
        try {
            tmp.writeText("hello world")
            val info = ArchitectureDetector.detectBinaryArchitecture(tmp)
            assertFalse(info.isElf)
        } finally {
            tmp.delete()
        }
    }
}
