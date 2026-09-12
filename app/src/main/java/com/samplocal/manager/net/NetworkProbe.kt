package com.samplocal.manager.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

data class LocalNetwork(
    val iface: String,
    val networkType: String,
    val ipv4: String?,
    val ipv6: String?,
    val gateway: String?
)

class NetworkProbe(private val context: Context) {

    fun probe(): LocalNetwork? {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            val active = cm.activeNetwork
            val caps = active?.let { cm.getNetworkCapabilities(it) }
            val type = when {
                caps == null -> "Sem rede"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Outra"
            }
            if (active == null) return LocalNetwork("—", type, null, null, null)
            val link = cm.getLinkProperties(active)
            val ifaceName = link?.interfaceName ?: "—"
            val gateway = link?.routes
                ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                ?.gateway?.hostAddress
            val addrs = collectAddrs(ifaceName)
            LocalNetwork(
                iface = ifaceName,
                networkType = type,
                ipv4 = selectLanIpv4(addrs),
                ipv6 = addrs.filterIsInstance<Inet6Address>()
                    .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                    ?.hostAddress,
                gateway = gateway
            )
        } catch (_: Exception) { null }
    }

    private fun collectAddrs(ifaceName: String): List<InetAddress> {
        return try {
            val out = mutableListOf<InetAddress>()
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            (ifaces.filter { it.name == ifaceName } + ifaces.filter { it.name != ifaceName })
                .filter { it.isUp && !it.isLoopback }
                .forEach { ni -> ni.inetAddresses?.toList()?.let { out.addAll(it) } }
            out
        } catch (_: Exception) { emptyList() }
    }

    companion object {

        fun selectLanIpv4(addrs: List<InetAddress>): String? {
            return addrs.filterIsInstance<Inet4Address>()
                .firstOrNull { a ->
                    !a.isLoopbackAddress && !a.isLinkLocalAddress && isPrivateV4(a.address)
                }?.hostAddress
        }

        fun isPrivateV4(b: ByteArray): Boolean {
            if (b.size != 4) return false
            val o = b.map { it.toInt() and 0xFF }
            return o[0] == 10 ||
                (o[0] == 172 && o[1] in 16..31) ||
                (o[0] == 192 && o[1] == 168)
        }

        fun isValidSampPort(port: Int): Boolean = port in 1..65535
    }
}
