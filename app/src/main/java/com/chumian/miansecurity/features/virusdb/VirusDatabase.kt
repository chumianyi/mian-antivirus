package com.chumian.miansecurity.features.virusdb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VirusSignature(
    val name: String,
    val type: String,
    val riskLevel: Int,
    val signature: String
)

class VirusDatabase(private val context: Context) {
    private var signatures: List<VirusSignature> = emptyList()
    var lastUpdate: Long = 0
        private set

    suspend fun load(): Int = withContext(Dispatchers.IO) {
        val list = mutableListOf<VirusSignature>()
        try {
            context.assets.open("virusdb/malware_signatures.db").bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        val parts = line.split("|")
                        if (parts.size >= 4) {
                            list.add(
                                VirusSignature(
                                    name = parts[0],
                                    type = parts[1],
                                    riskLevel = parts[2].toIntOrNull() ?: 5,
                                    signature = parts[3]
                                )
                            )
                        }
                    }
                }
            }
            lastUpdate = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        signatures = list
        list.size
    }

    fun scanText(text: String): List<VirusSignature> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase()
        return signatures.filter { sig ->
            lower.contains(sig.signature.lowercase())
        }
    }

    fun scanPackageName(packageName: String): List<VirusSignature> {
        return scanText(packageName)
    }

    fun scanAppName(appName: String): List<VirusSignature> {
        return scanText(appName)
    }

    fun getSignatureCount(): Int = signatures.size

    fun getByType(type: String): List<VirusSignature> {
        return signatures.filter { it.type.equals(type, ignoreCase = true) }
    }

    fun getHighRisk(): List<VirusSignature> {
        return signatures.filter { it.riskLevel >= 8 }
    }

    companion object {
        const val TYPE_TROJAN = "Trojan"
        const val TYPE_RANSOMWARE = "Ransomware"
        const val TYPE_ADWARE = "Adware"
        const val TYPE_SPYWARE = "Spyware"
        const val TYPE_BACKDOOR = "Backdoor"
        const val TYPE_WORM = "Worm"
        const val TYPE_ROOTKIT = "Rootkit"
    }
}
