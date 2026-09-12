package com.samplocal.manager.data.model

enum class HostArchitecture { ARM64, ARM32, X86_64, X86, UNKNOWN }
enum class GuestArchitecture { X86_32, X86_64, ARM64, ARM32, UNKNOWN }
enum class RuntimeBackend { QEMU_I386, BOX86, BOX64, BOX86_BOX64, NATIVE, UNSUPPORTED }

data class BinaryInfo(
    val isElf: Boolean,
    val elfClass: Int,
    val machine: Int,
    val guestArch: GuestArchitecture,
    val interpreter: String?,
    val neededLibs: List<String>
)

data class RuntimeStatus(
    val hostArch: HostArchitecture,
    val guestArch: GuestArchitecture,
    val backend: RuntimeBackend,
    val box86Available: Boolean,
    val box64Available: Boolean,
    val ready: Boolean,
    val details: String,
    val backendAvailable: Boolean = false,
    val backendPath: String? = null,
    val runtimeInstalled: Boolean = false,
    val runtimeVersion: String? = null
)
