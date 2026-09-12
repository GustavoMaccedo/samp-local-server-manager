package com.samplocal.manager.net.relay

object ProviderSelector {

    data class Candidate(
        val provider: RelayProvider,
        val health: ProviderHealth
    )

    data class Selection(
        val provider: RelayProvider?,
        val reason: String
    )

    fun order(candidates: List<Candidate>): List<Candidate> =
        candidates.sortedWith(
            compareByDescending<Candidate> { it.health.ok }
                .thenBy { it.provider.priority }
                .thenBy { it.health.latencyMs ?: Long.MAX_VALUE }
        )

    fun selectForVanilla(candidates: List<Candidate>): Selection {
        for (c in order(candidates)) {
            if (!c.provider.supportsVanillaSamp) continue
            if (!c.health.ok) continue
            return Selection(c.provider, "selecionado ${c.provider.id} (${c.health.detail})")
        }
        val tried = candidates.joinToString { "${it.provider.id}:${if (it.health.ok) "ok" else "falhou"}" }
        return Selection(null, "nenhum provider viavel [$tried]")
    }
}
