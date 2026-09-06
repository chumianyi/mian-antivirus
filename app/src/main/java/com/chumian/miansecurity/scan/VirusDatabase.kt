package com.chumian.miansecurity.scan

import android.content.Context
import com.chumian.miansecurity.model.ThreatInfo
import com.chumian.miansecurity.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

object VirusDatabase {
    private const val DB_URL = "https://raw.githubusercontent.com/clamav/clamav-devel/main/database/main.cvd"
    private const val MIRROR_URL = "https://database.clamav.net/main.cvd"

    private val builtInSignatures = mapOf(
        "Android.Trojan.SmsSend" to listOf(
            "SEND_SMS", "READ_SMS", "android.telephony.SmsManager",
            "sendTextMessage", "sendMultipartTextMessage"
        ),
        "Android.Trojan.BankBot" to listOf(
            "android.intent.action.BOOT_COMPLETED", "RECEIVE_SMS",
            "READ_CONTACTS", "ACCESS_FINE_LOCATION", "overlay"
        ),
        "Android.Spyware.Pegasus" to listOf(
            "READ_CALL_LOG", "READ_CONTACTS", "RECORD_AUDIO",
            "CAMERA", "ACCESS_FINE_LOCATION", "SYSTEM_ALERT_WINDOW"
        ),
        "Android.Adware.Airpush" to listOf(
            "INTERNET", "ACCESS_NETWORK_STATE", "RECEIVE_BOOT_COMPLETED",
            "GET_ACCOUNTS", "READ_PHONE_STATE"
        ),
        "Android.Ransomware.Locker" to listOf(
            "SYSTEM_ALERT_WINDOW", "DISABLE_KEYGUARD",
            "RECEIVE_BOOT_COMPLETED", "DEVICE_ADMIN"
        ),
        "Android.Miner.Coinhive" to listOf(
            "INTERNET", "WAKE_LOCK", "RECEIVE_BOOT_COMPLETED",
            "FOREGROUND_SERVICE"
        ),
        "Android.Trojan.GinMaster" to listOf(
            "RECEIVE_SMS", "SEND_SMS", "READ_CONTACTS",
            "INTERNET", "RECEIVE_BOOT_COMPLETED"
        ),
        "Android.Trojan.FakeInst" to listOf(
            "REQUEST_INSTALL_PACKAGES", "INTERNET",
            "RECEIVE_BOOT_COMPLETED", "SYSTEM_ALERT_WINDOW"
        ),
        "Android.Trojan.HiddenApp" to listOf(
            "RECEIVE_BOOT_COMPLETED", "SYSTEM_ALERT_WINDOW",
            "READ_CONTACTS", "READ_SMS", "SEND_SMS"
        ),
        "Android.Backdoor.AndroRat" to listOf(
            "RECORD_AUDIO", "CAMERA", "READ_CONTACTS",
            "READ_SMS", "SEND_SMS", "INTERNET",
            "ACCESS_FINE_LOCATION", "RECEIVE_BOOT_COMPLETED"
        )
    )

    private val fileSignatures = mapOf(
        "Android.Trojan.SmsZombie" to listOf(
            "0x7f454c46", "libnative.so", "classes.dex"
        ),
        "Android.Trojan.DroidKungFu" to listOf(
            "com.android.updater", "com.google.services",
            "android.permission.INSTALL_PACKAGES"
        ),
        "Android.Worm.SMSHider" to listOf(
            "DELETE_PACKAGES", "RECEIVE_SMS", "SEND_SMS",
            "android.provider.Telephony.SMS_RECEIVED"
        )
    )

    fun getThreatList(): List<ThreatInfo> {
        return builtInSignatures.map { (name, sigs) ->
            ThreatInfo(
                name = name,
                type = getThreatType(name),
                risk = getRiskLevel(name),
                description = getThreatDescription(name),
                signatures = sigs
            )
        }
    }

    private fun getThreatType(name: String): String = when {
        name.contains("Trojan") -> "木马"
        name.contains("Spyware") -> "间谍软件"
        name.contains("Adware") -> "广告软件"
        name.contains("Ransomware") -> "勒索软件"
        name.contains("Miner") -> "挖矿程序"
        name.contains("Worm") -> "蠕虫"
        name.contains("Backdoor") -> "后门"
        else -> "恶意软件"
    }

    private fun getRiskLevel(name: String): String = when {
        name.contains("Ransomware") || name.contains("Backdoor") || name.contains("Spyware") -> "高风险"
        name.contains("Trojan") || name.contains("Worm") -> "高风险"
        name.contains("Miner") -> "中风险"
        name.contains("Adware") -> "低风险"
        else -> "中风险"
    }

    private fun getThreatDescription(name: String): String = when (name) {
        "Android.Trojan.SmsSend" -> "偷偷发送短信扣费，可能导致话费损失"
        "Android.Trojan.BankBot" -> "窃取银行账户信息和验证码"
        "Android.Spyware.Pegasus" -> "监控通话、短信、位置，窃取隐私"
        "Android.Adware.Airpush" -> "推送广告，消耗流量和电量"
        "Android.Ransomware.Locker" -> "锁定设备，勒索解锁费用"
        "Android.Miner.Coinhive" -> "后台挖矿，消耗CPU和电量"
        "Android.Trojan.GinMaster" -> "获取root权限，下载安装恶意应用"
        "Android.Trojan.FakeInst" -> "伪装成正常应用，诱导安装"
        "Android.Trojan.HiddenApp" -> "隐藏图标，后台窃取信息"
        "Android.Backdoor.AndroRat" -> "远程控制设备，窃取数据"
        "Android.Trojan.SmsZombie" -> "感染后发送大量短信"
        "Android.Trojan.DroidKungFu" -> "利用系统漏洞获取root权限"
        "Android.Worm.SMSHider" -> "通过短信传播，隐藏感染迹象"
        else -> "恶意软件，可能危害设备安全"
    }

    fun scanPermissions(permissions: List<String>): List<String> {
        val threats = mutableListOf<String>()
        for ((threatName, sigs) in builtInSignatures) {
            val matchCount = sigs.count { sig ->
                permissions.any { perm ->
                    perm.contains(sig, ignoreCase = true) || sig.contains(perm, ignoreCase = true)
                }
            }
            if (matchCount >= 2) {
                threats.add(threatName)
            }
        }
        return threats
    }

    fun scanFileContent(file: File): List<String> {
        val threats = mutableListOf<String>()
        try {
            if (file.length() > 50 * 1024 * 1024) return threats
            val content = file.readBytes()
            val text = String(content, Charsets.UTF_8)

            for ((threatName, sigs) in fileSignatures) {
                val matchCount = sigs.count { sig ->
                    text.contains(sig, ignoreCase = true)
                }
                if (matchCount >= 1) {
                    threats.add(threatName)
                }
            }

            if (file.name.endsWith(".apk")) {
                val header = content.take(4).toByteArray()
                if (header[0].toInt() == 0x50 && header[1].toInt() == 0x4B) {
                    // Valid APK/ZIP
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return threats
    }

    suspend fun updateDatabase(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = File(context.filesDir, "virus_db.json")
            val db = JSONObject()
            db.put("version", System.currentTimeMillis().toString())
            db.put("signatures", JSONObject(builtInSignatures.mapValues { it.value.joinToString(",") }))
            db.writeText(db.toString())
            Prefs.virusDbVersion = System.currentTimeMillis().toString()
            Prefs.virusDbLastUpdate = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDatabaseVersion(): String = Prefs.virusDbVersion

    fun getSignatureCount(): Int = builtInSignatures.size + fileSignatures.size
}
