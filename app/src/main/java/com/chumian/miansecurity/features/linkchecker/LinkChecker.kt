package com.chumian.miansecurity.features.linkchecker

import android.content.Context
import android.content.SharedPreferences
import com.chumian.miansecurity.db.AppDatabase
import com.chumian.miansecurity.db.BehaviorLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class LinkCheckResult(
    val url: String,
    val isSafe: Boolean,
    val riskLevel: Int, // 0=安全, 1=低风险, 2=中风险, 3=高风险
    val riskLevelText: String,
    val reasons: List<String>,
    val domain: String,
    val isShortLink: Boolean,
    val expandedUrl: String?,
    val sslValid: Boolean?,
    val checkTime: Long
)

class LinkChecker(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("link_checker", Context.MODE_PRIVATE)

    // 内置恶意域名库（钓鱼/恶意/广告）
    private val MALICIOUS_DOMAINS = setOf(
        "phishing.com", "malware.com", "virus.com", "trojan.com",
        "fake-bank.com", "login-verify.com", "account-update.com",
        "free-gift.com", "prize-claim.com", "winner-notice.com",
        "bitcoin-miner.com", "crypto-scam.com", "investment-fraud.com",
        "adware-server.com", "popup-ads.com", "redirect-ads.com",
        "ransomware.com", "spyware.com", "keylogger.com",
        "crack-download.com", "keygen-site.com", "pirate-software.com",
        "fake-update.com", "flash-update.com", "browser-update.org",
        "tech-support-scam.com", "microsoft-support-fake.com",
        "apple-id-verify.com", "google-account-check.com"
    )

    // 短链接服务
    private val SHORT_LINK_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly",
        "is.gd", "buff.ly", "rebrand.ly", "shorturl.at", "cutt.ly"
    )

    // 高风险TLD
    private val RISKY_TLDS = setOf(
        ".xyz", ".top", ".club", ".work", ".click", ".link",
        ".science", ".party", ".gq", ".tk", ".ml", ".cf"
    )

    suspend fun checkUrl(url: String): LinkCheckResult = withContext(Dispatchers.IO) {
        val reasons = mutableListOf<String>()
        var riskLevel = 0
        val checkTime = System.currentTimeMillis()

        // 规范化URL
        val normalizedUrl = normalizeUrl(url)
        val domain = extractDomain(normalizedUrl)

        // 检查是否为短链接
        val isShortLink = SHORT_LINK_DOMAINS.any { domain == it || domain.endsWith(".$it") }
        var expandedUrl: String? = null
        if (isShortLink) {
            reasons.add("短链接服务: $domain")
            riskLevel = maxOf(riskLevel, 1)
            expandedUrl = expandShortLink(normalizedUrl)
            if (expandedUrl != null) {
                val expandedDomain = extractDomain(expandedUrl)
                if (isMaliciousDomain(expandedDomain)) {
                    reasons.add("展开后域名在恶意库中: $expandedDomain")
                    riskLevel = 3
                }
            }
        }

        // 检查域名是否在恶意库中
        if (isMaliciousDomain(domain)) {
            reasons.add("域名在恶意软件库中")
            riskLevel = 3
        }

        // 检查TLD风险
        val tld = domain.substringAfterLast(".", "")
        if (RISKY_TLDS.contains(".$tld")) {
            reasons.add("高风险顶级域名: .$tld")
            riskLevel = maxOf(riskLevel, 2)
        }

        // 检查URL中是否包含可疑关键词
        val urlLower = normalizedUrl.lowercase()
        val suspiciousKeywords = listOf(
            "login", "signin", "verify", "account", "password",
            "update", "confirm", "secure", "bank", "paypal",
            "apple", "google", "microsoft", "amazon", "facebook"
        )
        val hasSuspiciousKeywords = suspiciousKeywords.any { urlLower.contains(it) }
        if (hasSuspiciousKeywords && !isKnownSafeDomain(domain)) {
            reasons.add("URL包含敏感关键词（可能是钓鱼）")
            riskLevel = maxOf(riskLevel, 2)
        }

        // 检查SSL证书
        var sslValid: Boolean? = null
        if (normalizedUrl.startsWith("https://")) {
            sslValid = checkSslCertificate(normalizedUrl)
            if (sslValid == false) {
                reasons.add("SSL证书无效或过期")
                riskLevel = maxOf(riskLevel, 2)
            }
        } else if (normalizedUrl.startsWith("http://")) {
            reasons.add("使用不安全的HTTP协议")
            riskLevel = maxOf(riskLevel, 1)
        }

        // 检查域名长度（异常长的域名可能是钓鱼）
        if (domain.length > 30) {
            reasons.add("域名异常长（可能是钓鱼）")
            riskLevel = maxOf(riskLevel, 1)
        }

        // 检查是否包含IP地址而非域名
        if (isIpAddress(domain)) {
            reasons.add("使用IP地址而非域名（可疑）")
            riskLevel = maxOf(riskLevel, 2)
        }

        val riskLevelText = when (riskLevel) {
            0 -> "安全"
            1 -> "低风险"
            2 -> "中风险"
            else -> "高风险"
        }

        val result = LinkCheckResult(
            url = normalizedUrl,
            isSafe = riskLevel == 0,
            riskLevel = riskLevel,
            riskLevelText = riskLevelText,
            reasons = reasons,
            domain = domain,
            isShortLink = isShortLink,
            expandedUrl = expandedUrl,
            sslValid = sslValid,
            checkTime = checkTime
        )

        // 保存到历史记录
        saveToHistory(result)

        result
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized
    }

    private fun extractDomain(url: String): String {
        return try {
            val urlObj = URL(url)
            urlObj.host.lowercase()
        } catch (e: Exception) {
            url.substringAfter("://").substringBefore("/").lowercase()
        }
    }

    private fun isMaliciousDomain(domain: String): Boolean {
        return MALICIOUS_DOMAINS.any { domain == it || domain.endsWith(".$it") }
    }

    private fun isKnownSafeDomain(domain: String): Boolean {
        val safeDomains = setOf(
            "google.com", "youtube.com", "facebook.com", "twitter.com",
            "instagram.com", "baidu.com", "qq.com", "taobao.com",
            "tmall.com", "jd.com", "weibo.com", "zhihu.com",
            "bilibili.com", "douyin.com", "github.com", "stackoverflow.com",
            "apple.com", "microsoft.com", "amazon.com", "netflix.com"
        )
        return safeDomains.any { domain == it || domain.endsWith(".$it") }
    }

    private fun isIpAddress(domain: String): Boolean {
        val parts = domain.split(".")
        if (parts.size != 4) return false
        return parts.all { it.toIntOrNull() in 0..255 }
    }

    private fun expandShortLink(shortUrl: String): String? {
        return try {
            val url = URL(shortUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                connection.getHeaderField("Location")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkSslCertificate(url: String): Boolean {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpsURLConnection
            connection.sslSocketFactory = sslContext.socketFactory
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            // 检查证书是否过期
            val certs = connection.serverCertificates
            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate
                cert.checkValidity()
                true
            } else {
                false
            }
        } catch (e: CertificateException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun saveToHistory(result: LinkCheckResult) = withContext(Dispatchers.IO) {
        try {
            db.behaviorLogDao().insert(
                BehaviorLogEntity(
                    packageName = result.domain,
                    behaviorType = "link_check",
                    description = "${result.url} - ${result.riskLevelText} - ${result.reasons.joinToString("; ")}",
                    riskLevel = result.riskLevel,
                    timestamp = result.checkTime
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(): Flow<List<BehaviorLogEntity>> {
        return db.behaviorLogDao().getAll()
    }

    fun clearHistory() {
        prefs.edit().clear().apply()
    }

    fun addMaliciousDomain(domain: String) {
        val domains = prefs.getStringSet("custom_malicious", emptySet())?.toMutableSet() ?: mutableSetOf()
        domains.add(domain.lowercase())
        prefs.edit().putStringSet("custom_malicious", domains).apply()
    }

    fun addSafeDomain(domain: String) {
        val domains = prefs.getStringSet("custom_safe", emptySet())?.toMutableSet() ?: mutableSetOf()
        domains.add(domain.lowercase())
        prefs.edit().putStringSet("custom_safe", domains).apply()
    }
}
