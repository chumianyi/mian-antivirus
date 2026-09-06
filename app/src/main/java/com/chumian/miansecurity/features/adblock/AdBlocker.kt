package com.chumian.miansecurity.features.adblock

import android.content.Context
import android.content.SharedPreferences
import com.chumian.miansecurity.core.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AdBlockSource(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val enabled: Boolean
)

class AdBlocker(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("adblock", Context.MODE_PRIVATE)

    companion object {
        const val HOSTS_PATH = "/system/etc/hosts"
        const val HOSTS_BACKUP_PATH = "/data/local/tmp/hosts.mian.backup"

        // 超长免责声明
        val DISCLAIMER = """
【广告拦截功能免责声明】

请仔细阅读以下免责声明，勾选"我已阅读并同意"后方可使用广告拦截功能。

1. 功能说明
广告拦截功能通过修改系统hosts文件，将广告域名重定向到本地（127.0.0.1），从而阻止广告请求。本功能使用开源的广告域名库，包括但不限于StevenBlack/hosts、AdAway官方源等。

2. 可能的风险
(1) 广告拦截可能导致某些应用或网站无法正常使用。部分应用和网站依赖广告收入，拦截广告可能导致应用崩溃、功能受限、内容无法加载等问题。
(2) 修改系统hosts文件存在风险。hosts文件是系统重要文件，修改不当可能导致网络异常、域名解析失败、系统不稳定等问题。
(3) 可能违反某些应用的服务条款。部分应用的用户协议明确禁止使用广告拦截工具，使用本功能可能导致账号被封禁、服务被终止等后果。
(4) 可能影响应用的正常更新。部分应用通过广告域名进行版本检查和更新，拦截后可能导致应用无法正常更新。
(5) 可能导致支付功能异常。部分支付SDK通过特定域名进行通信，拦截后可能导致支付失败、订单异常等问题。
(6) 可能影响推送通知。部分推送服务通过广告相关域名进行通信，拦截后可能导致推送延迟或无法接收。

3. 开发者免责
(1) 本功能仅供学习和研究使用，使用者应自行承担使用本功能的所有风险和后果。
(2) 开发者不对使用本功能导致的任何直接或间接损失承担责任，包括但不限于数据丢失、账号封禁、应用崩溃、系统不稳定、经济损失等。
(3) 开发者不对广告域名库的准确性、完整性、及时性做任何保证。域名库可能包含误拦截，也可能遗漏某些广告域名。
(4) 开发者保留随时修改、暂停、终止本功能的权利，无需提前通知。

4. 使用建议
(1) 建议在使用前备份系统hosts文件，以便出现问题时恢复。
(2) 建议谨慎选择广告拦截源，避免使用来源不明的域名库。
(3) 如遇应用异常，可尝试关闭广告拦截或添加白名单。
(4) 建议定期更新广告域名库，以获得最新的拦截效果。
(5) 本功能不适合所有用户，如您不确定是否使用，请不要开启。

5. 开源声明
本功能使用的广告域名库均来自开源项目，遵循各自的开源协议。包括但不限于：
- StevenBlack/hosts (MIT License)
- AdAway (GPLv3 License)
- Energized Protection (MIT License)

6. 同意条款
勾选"我已阅读并同意"即表示您已完整阅读并理解以上所有条款，自愿承担使用本功能的所有风险和后果。如您不同意以上任何条款，请不要使用本功能。

【声明结束】
        """.trimIndent()
    }

    // 内置广告拦截源
    val defaultSources = listOf(
        AdBlockSource(
            id = "stevenblack",
            name = "StevenBlack hosts",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            description = "综合广告/恶意软件拦截，最受欢迎的hosts源",
            enabled = true
        ),
        AdBlockSource(
            id = "adaway",
            name = "AdAway 官方源",
            url = "https://adaway.org/hosts.txt",
            description = "AdAway官方广告拦截源",
            enabled = true
        ),
        AdBlockSource(
            id = "yoyo",
            name = "yoyo.org 广告列表",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            description = "知名广告服务器列表",
            enabled = false
        ),
        AdBlockSource(
            id = "energized",
            name = "Energized Protection",
            url = "https://block.energized.pro/ultimate/formats/hosts",
            description = "大规模广告/追踪/恶意软件拦截",
            enabled = false
        )
    )

    fun isEnabled(): Boolean {
        return prefs.getBoolean("adblock_enabled", false)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("adblock_enabled", enabled).apply()
    }

    fun hasAcceptedDisclaimer(): Boolean {
        return prefs.getBoolean("disclaimer_accepted", false)
    }

    fun acceptDisclaimer() {
        prefs.edit().putBoolean("disclaimer_accepted", true).apply()
    }

    fun getEnabledSources(): List<AdBlockSource> {
        val enabledIds = prefs.getStringSet("enabled_sources", setOf("stevenblack", "adaway")) ?: setOf()
        return defaultSources.map { source ->
            source.copy(enabled = enabledIds.contains(source.id))
        }.filter { it.enabled }
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        val enabledIds = prefs.getStringSet("enabled_sources", setOf("stevenblack", "adaway"))?.toMutableSet() ?: mutableSetOf()
        if (enabled) {
            enabledIds.add(sourceId)
        } else {
            enabledIds.remove(sourceId)
        }
        prefs.edit().putStringSet("enabled_sources", enabledIds).apply()
    }

    fun getWhitelist(): Set<String> {
        return prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
    }

    fun addToWhitelist(domain: String) {
        val whitelist = getWhitelist().toMutableSet()
        whitelist.add(domain.lowercase())
        prefs.edit().putStringSet("whitelist", whitelist).apply()
    }

    fun removeFromWhitelist(domain: String) {
        val whitelist = getWhitelist().toMutableSet()
        whitelist.remove(domain.lowercase())
        prefs.edit().putStringSet("whitelist", whitelist).apply()
    }

    fun getBlockedCount(): Int {
        return prefs.getInt("blocked_count", 0)
    }

    fun getBlockedDomains(): Set<String> {
        return prefs.getStringSet("blocked_domains", emptySet()) ?: emptySet()
    }

    suspend fun updateHosts(): Boolean = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isGranted()) {
            return@withContext false
        }

        try {
            // 备份原hosts文件
            ShizukuHelper.runCommand("cp $HOSTS_PATH $HOSTS_BACKUP_PATH")

            // 下载并合并所有启用的源
            val allDomains = mutableSetOf<String>()
            for (source in getEnabledSources()) {
                try {
                    val content = downloadUrl(source.url)
                    val domains = parseHosts(content)
                    allDomains.addAll(domains)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 移除白名单域名
            val whitelist = getWhitelist()
            allDomains.removeAll { domain ->
                whitelist.any { domain == it || domain.endsWith(".$it") }
            }

            // 生成新的hosts文件
            val hostsContent = buildString {
                append("127.0.0.1 localhost\n")
                append("::1 localhost\n")
                append("# Generated by 眠.杀毒软件 AdBlocker\n")
                append("# Total blocked domains: ${allDomains.size}\n\n")
                for (domain in allDomains.sorted()) {
                    append("0.0.0.0 $domain\n")
                }
            }

            // 写入临时文件
            val tempFile = File("/data/local/tmp/hosts.mian.new")
            tempFile.writeText(hostsContent)

            // 挂载system为可写并替换hosts
            ShizukuHelper.runCommand("mount -o remount,rw /system")
            ShizukuHelper.runCommand("cp ${tempFile.absolutePath} $HOSTS_PATH")
            ShizukuHelper.runCommand("chmod 644 $HOSTS_PATH")
            ShizukuHelper.runCommand("mount -o remount,ro /system")

            // 保存统计
            prefs.edit()
                .putInt("blocked_count", allDomains.size)
                .putStringSet("blocked_domains", allDomains)
                .putLong("last_update", System.currentTimeMillis())
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreHosts(): Boolean = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isGranted()) {
            return@withContext false
        }

        try {
            ShizukuHelper.runCommand("mount -o remount,rw /system")
            ShizukuHelper.runCommand("cp $HOSTS_BACKUP_PATH $HOSTS_PATH")
            ShizukuHelper.runCommand("chmod 644 $HOSTS_PATH")
            ShizukuHelper.runCommand("mount -o remount,ro /system")
            setEnabled(false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLastUpdateTime(): Long {
        return prefs.getLong("last_update", 0)
    }

    private fun downloadUrl(url: String): String {
        return java.net.URL(url).readText()
    }

    private fun parseHosts(content: String): Set<String> {
        val domains = mutableSetOf<String>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val ip = parts[0]
                val domain = parts[1].lowercase()
                if (ip == "0.0.0.0" || ip == "127.0.0.1") {
                    if (domain != "localhost" && domain.contains(".")) {
                        domains.add(domain)
                    }
                }
            }
        }
        return domains
    }
}
