package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

object NetworkUtils {

    /**
     * Finds the real local IPv4 address across active interfaces.
     * Prioritizes wlan0 (Wi-Fi), ap0/wlan1 (Hotspot), p2p0 (Wi-Fi Direct), then any valid site-local IP.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            // 1. First priority: Wi-Fi or Hotspot interfaces
            val preferredNames = listOf("wlan0", "wlan1", "ap0", "p2p-wlan0", "p2p0", "eth0", "rndis0")
            for (name in preferredNames) {
                val iface = interfaces.firstOrNull { it.name.equals(name, ignoreCase = true) && it.isUp }
                if (iface != null) {
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrEmpty() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            }

            // 2. Second priority: Any active non-loopback site-local IPv4
            for (iface in interfaces) {
                if (iface.isUp && !iface.isLoopback) {
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrEmpty() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.43.1" // Common Android Hotspot default fallback
    }

    fun getNetworkTypeName(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return "Hotspot / Offline AP"
            val caps = cm.getNetworkCapabilities(net) ?: return "Hotspot / Offline AP"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi LAN"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Not needed)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet LAN"
                else -> "Local Network / Hotspot"
            }
        } catch (e: Exception) {
            "Local Network"
        }
    }

    fun getWifiSsid(context: Context): String {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifi?.connectionInfo
            val ssid = info?.ssid?.replace("\"", "")
            if (ssid.isNullOrEmpty() || ssid == "<unknown ssid>") {
                "TwinGuard Hotspot / Direct"
            } else {
                ssid
            }
        } catch (e: Exception) {
            "Local Offline Network"
        }
    }

    fun generateSessionToken(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).uppercase()
    }

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("twinguard_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "TG-" + UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }
}
