package com.samplocal.manager.net

import java.net.InetAddress

object NatClassifier {

    data class Verdict(val state: NatState, val detail: String)

    fun classify(
        localIp: InetAddress?,
        localPort: Int,
        reflexiveIp: InetAddress?,
        reflexivePort: Int?,
        hasNetwork: Boolean
    ): Verdict {
        if (!hasNetwork) return Verdict(NatState.UNREACHABLE, "Sem rede ativa")
        if (localIp == null) return Verdict(NatState.UNKNOWN, "IP local indeterminado")
        if (reflexiveIp == null || reflexivePort == null) {
            return Verdict(NatState.UNKNOWN, "STUN sem resposta — tipo de NAT indeterminado")
        }
        return if (reflexiveIp.hostAddress == localIp.hostAddress && reflexivePort == localPort) {
            Verdict(NatState.DIRECT, "Endereço observado igual ao local (sem NAT)")
        } else {
            Verdict(
                NatState.NAT,
                "NAT detectado (${localIp.hostAddress}:$localPort → ${reflexiveIp.hostAddress}:$reflexivePort)"
            )
        }
    }

    fun friendly(state: NatState): String = when (state) {
        NatState.DIRECT -> "Sem NAT"
        NatState.NAT -> "NAT detectado"
        NatState.UNREACHABLE -> "Sem rede"
        NatState.UNKNOWN -> "Indeterminado"
    }

    enum class Mapping { PORT_PRESERVED, SYMMETRIC, UNKNOWN }

    fun compareMappings(aPort: Int?, bPort: Int?): Mapping {
        if (aPort == null || bPort == null) return Mapping.UNKNOWN
        return if (aPort == bPort) Mapping.PORT_PRESERVED else Mapping.SYMMETRIC
    }

    fun cgnatSuspect(networkType: String?, mapping: Mapping, state: NatState): Boolean =
        state == NatState.NAT && mapping == Mapping.SYMMETRIC &&
            (networkType == "Mobile" || networkType == "VPN")
}
