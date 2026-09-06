package com.chumian.miansecurity.features.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.MessageDigest

data class WifiSecurityInfo(
    val ssid: String,
    val bssid: String,
    val ipAddress: String,
    val macAddress: String,
    val signalLevel: Int,
    val frequency: Int,
    val is5GHz: Boolean,
    val securityType: String,
    val isSecure: Boolean,
    val isArpSpoofingDetected: Boolean,
    val isDnsHijackingDetected: Boolean,
    val gateway: String,
    val dns1: String,
    val dns2: String,
    val riskScore: Int,
    val recommendations: List<String>
)

class WifiSecurityChecker(private val context: Context) {

    fun getCurrentWifiInfo(): WifiSecurityInfo? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return null

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) cm.activeNetwork else null
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return null

        val wifiInfo = wifiManager.connectionInfo ?: return null

        val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: "未知"
        val bssid = wifiInfo.bssid ?: "未知"
        val ipAddress = intToIp(wifiInfo.ipAddress)
        val macAddress = getMacAddress()
        val signalLevel = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)
        val frequency = wifiInfo.frequency
        val is5GHz = frequency > 4900 && frequency < 5900

        val securityType = detectSecurityType(wifiInfo)
        val isSecure = securityType != "OPEN" && securityType != "WEP"

        val gateway = getGateway()
        val dnsServers = getDnsServers()
        val dns1 = dnsServers.getOrNull(0) ?: "未知"
        val dns2 = dnsServers.getOrNull(1) ?: "未知"

        val arpSpoofing = detectArpSpoofing(gateway)
        val dnsHijacking = detectDnsHijacking(dnsServers)

        var riskScore = 0
        val recommendations = mutableListOf<String>()

        if (!isSecure) {
            riskScore += 40
            recommendations.add("当前WiFi未加密，建议使用WPA2/WPA3加密")
        }
        if (securityType == "WEP") {
            riskScore += 30
            recommendations.add("WEP加密已不安全，建议升级到WPA2/WPA3")
        }
        if (arpSpoofing) {
            riskScore += 50
            recommendations.add("检测到ARP欺骗攻击风险，可能存在中间人攻击")
        }
        if (dnsHijacking) {
            riskScore += 40
            recommendations.add("检测到DNS劫持风险，建议更换DNS服务器")
        }
        if (signalLevel < 30) {
            riskScore += 10
            recommendations.add("信号较弱，建议靠近路由器")
        }
        if (is5GHz) {
            recommendations.add("5GHz频段，速度更快但穿墙能力较弱")
        }

        riskScore = riskScore.coerceAtMost(100)

        return WifiSecurityInfo(
            ssid = ssid,
            bssid = bssid,
            ipAddress = ipAddress,
            macAddress = macAddress,
            signalLevel = signalLevel,
            frequency = frequency,
            is5GHz = is5GHz,
            securityType = securityType,
            isSecure = isSecure,
            isArpSpoofingDetected = arpSpoofing,
            isDnsHijackingDetected = dnsHijacking,
            gateway = gateway,
            dns1 = dns1,
            dns2 = dns2,
            riskScore = riskScore,
            recommendations = recommendations
        )
    }

    private fun detectSecurityType(wifiInfo: WifiInfo): String {
        return try {
            val capabilities = wifiInfo.toString()
            when {
                capabilities.contains("WPA3") -> "WPA3"
                capabilities.contains("WPA2") -> "WPA2"
                capabilities.contains("WPA") -> "WPA"
                capabilities.contains("WEP") -> "WEP"
                else -> "OPEN"
            }
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun detectArpSpoofing(gateway: String): Boolean {
        return try {
            val gatewayMac = getMacForIp(gateway)
            if (gatewayMac.isNullOrEmpty()) return false
            val knownRouters = setOf(
                "00:00:00:00:00:00",
                "ff:ff:ff:ff:ff:ff"
            )
            gatewayMac in knownRouters
        } catch (e: Exception) {
            false
        }
    }

    private fun detectDnsHijacking(dnsServers: List<String>): Boolean {
        return try {
            val suspiciousDns = setOf(
                "0.0.0.0",
                "1.1.1.1",
                "8.8.8.8"
            )
            dnsServers.any { it in suspiciousDns }
        } catch (e: Exception) {
            false
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }

    private fun getMacAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (nif.name.equals("wlan0", ignoreCase = true)) {
                    val mac = nif.hardwareAddress
                    if (mac != null) {
                        return mac.joinToString(":") { "%02x".format(it) }
                    }
                }
            }
            "02:00:00:00:00:00"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getGateway(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("ip route show default")
            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine()
            process.waitFor()
            if (line != null && line.contains("via")) {
                line.split(" ")[2]
            } else {
                "未知"
            }
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun getDnsServers(): List<String> {
        return try {
            val dnsServers = mutableListOf<String>()
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("getprop net.dns1")
            val reader = process.inputStream.bufferedReader()
            val dns1 = reader.readLine()?.trim()
            process.waitFor()
            if (!dns1.isNullOrEmpty()) dnsServers.add(dns1)

            val process2 = runtime.exec("getprop net.dns2")
            val reader2 = process2.inputStream.bufferedReader()
            val dns2 = reader2.readLine()?.trim()
            process2.waitFor()
            if (!dns2.isNullOrEmpty()) dnsServers.add(dns2)

            dnsServers
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getMacForIp(ip: String): String? {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("arp -a $ip")
            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine()
            process.waitFor()
            if (line != null) {
                val regex = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
                regex.find(line)?.value
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getRiskLevel(score: Int): String {
        return when {
            score >= 70 -> "高风险"
            score >= 40 -> "中风险"
            score >= 20 -> "低风险"
            else -> "安全"
        }
    }
}
