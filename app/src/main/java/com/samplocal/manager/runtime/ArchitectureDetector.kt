package com.samplocal.manager.runtime

import android.os.Build
import com.samplocal.manager.data.model.BinaryInfo
import com.samplocal.manager.data.model.GuestArchitecture
import com.samplocal.manager.data.model.HostArchitecture
import com.samplocal.manager.data.model.RuntimeBackend
import com.samplocal.manager.util.ElfParser
import java.io.File

object ArchitectureDetector {

    fun detectHostArchitecture(): HostArchitecture {
        val abis = try {
            (Build.SUPPORTED_ABIS?.toList() ?: emptyList()).map { it.lowercase() }
        } catch (_: Exception) { emptyList() }
        val primary = abis.firstOrNull()
            ?: try { Build.CPU_ABI?.lowercase() } catch (_: Exception) { null }
            ?: System.getProperty("os.arch")?.lowercase().orEmpty()
        return when {
            primary.contains("arm64") || primary.contains("aarch64") -> HostArchitecture.ARM64
            primary.startsWith("armeabi") || primary.contains("armv7") || primary == "arm" -> HostArchitecture.ARM32
            primary.contains("x86_64") -> HostArchitecture.X86_64
            primary == "x86" || primary.contains("i686") || primary.contains("i386") -> HostArchitecture.X86
            else -> HostArchitecture.UNKNOWN
        }
    }

    fun detectBinaryArchitecture(binary: File): BinaryInfo {
        val r = ElfParser.parse(binary)
        if (!r.isElf) {
            return BinaryInfo(false, 0, 0, GuestArchitecture.UNKNOWN, null, emptyList())
        }
        val guest = when (r.machine) {
            3 -> GuestArchitecture.X86_32
            62 -> GuestArchitecture.X86_64
            40 -> GuestArchitecture.ARM32
            183 -> GuestArchitecture.ARM64
            else -> GuestArchitecture.UNKNOWN
        }
        return BinaryInfo(true, r.elfClass, r.machine, guest, r.interpreter, r.needed)
    }

    fun detectRequiredRuntime(host: HostArchitecture, guest: GuestArchitecture): RuntimeBackend {
        if (guest == GuestArchitecture.UNKNOWN) return RuntimeBackend.UNSUPPORTED

        return when {
            host == HostArchitecture.ARM64 && guest == GuestArchitecture.X86_32 -> RuntimeBackend.QEMU_I386
            host == HostArchitecture.ARM64 && guest == GuestArchitecture.X86_64 -> RuntimeBackend.BOX64
            host == HostArchitecture.X86_64 && guest == GuestArchitecture.X86_32 -> RuntimeBackend.NATIVE
            host == HostArchitecture.X86_64 && guest == GuestArchitecture.X86_64 -> RuntimeBackend.NATIVE
            host == HostArchitecture.ARM64 && guest == GuestArchitecture.ARM64 -> RuntimeBackend.NATIVE
            else -> RuntimeBackend.UNSUPPORTED
        }
    }
}
