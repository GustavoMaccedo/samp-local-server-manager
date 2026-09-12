package com.samplocal.manager.net

import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress

enum class CandidateType { HOST, SRFLX, RELAY }

data class IceCandidate(
    val type: CandidateType,
    val ip: String,
    val port: Int,
    val foundation: String,
    val priority: Long,
    val component: Int = 1
) {
    fun toJson(): JSONObject = JSONObject()
        .put("type", type.name)
        .put("ip", ip)
        .put("port", port)
        .put("foundation", foundation)
        .put("priority", priority)

    companion object {
        fun typePref(t: CandidateType): Int = when (t) {
            CandidateType.HOST -> 126
            CandidateType.SRFLX -> 100
            CandidateType.RELAY -> 0
        }

        fun priority(type: CandidateType, localPref: Int = 65535, component: Int = 1): Long =
            (1L shl 24) * typePref(type) + (1L shl 8) * localPref + (256 - component)

        fun host(ip: InetAddress, port: Int): IceCandidate = IceCandidate(
            CandidateType.HOST, ip.hostAddress ?: ip.toString(), port,
            "h${ip.hostAddress.hashCode() and 0xFFFF}",
            priority(CandidateType.HOST)
        )

        fun srflx(ip: InetAddress, port: Int, base: String): IceCandidate = IceCandidate(
            CandidateType.SRFLX, ip.hostAddress ?: ip.toString(), port,
            "s${(ip.hostAddress + base).hashCode() and 0xFFFF}",
            priority(CandidateType.SRFLX)
        )

        fun relay(ip: String, port: Int): IceCandidate = IceCandidate(
            CandidateType.RELAY, ip, port, "r0", priority(CandidateType.RELAY)
        )

        fun fromJson(o: JSONObject): IceCandidate = IceCandidate(
            CandidateType.valueOf(o.getString("type")),
            o.getString("ip"),
            o.getInt("port"),
            o.optString("foundation", "?"),
            o.optLong("priority", 0)
        )

        fun listToJson(list: List<IceCandidate>): String =
            JSONArray(list.map { it.toJson() }).toString()

        fun listFromJson(s: String): List<IceCandidate> {
            val a = JSONArray(s)
            return (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }

        fun prioritize(all: List<IceCandidate>): List<IceCandidate> =
            all.distinctBy { Triple(it.type, it.ip, it.port) }
                .sortedByDescending { it.priority }
    }
}
