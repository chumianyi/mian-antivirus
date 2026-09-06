package com.chumian.miansecurity.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.chumian.miansecurity.features.virusdb.VirusDatabase
import com.chumian.miansecurity.model.AppScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.security.MessageDigest

object AppScanner {
    // 只有这两个权限算危险权限
    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    // 盗版/破解特征
    private val PIRACY_SIGNATURES = mapOf(
        "Lucky Patcher" to listOf("com.android.vending.billing.InAppBillingService", "lucky", "patcher"),
        "破解版特征" to listOf("cracked", "patched", "mod", "hack", "cheat", "unlock", "premium", "pro"),
        "盗版市场" to listOf("blackmart", "mobogenie", "acmarket", "appvn", "happymod", "revdl"),
        "Google Play校验失败" to listOf("com.android.vending.INSTALL_REFERRER", "license_check")
    )

    // 已知官方应用签名（简化版，实际应该从官方获取）
    private val OFFICIAL_SIGNATURES = mapOf(
        "com.tencent.mm" to "MD5_SIGNATURE_WECHAT",
        "com.tencent.mobileqq" to "MD5_SIGNATURE_QQ",
        "com.eg.android.AlipayGphone" to "MD5_SIGNATURE_ALIPAY"
    )

    data class ScanProgress(
        val currentApp: String,
        val scanned: Int,
        val total: Int,
        val dangerCount: Int,
        val skippedSystemCount: Int
    )

    data class PiracyInfo(
        val isPiracy: Boolean,
        val reasons: List<String>,
        val signatureMatch: Boolean
    )

    fun scanApps(context: Context): Flow<Any> = flow {
        val pm = context.packageManager
        val virusDb = VirusDatabase(context)
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)

        // 只扫描用户安装的应用，跳过系统应用
        val userPackages = packages.filter { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            } catch (e: Exception) {
                false
            }
        }

        val total = userPackages.size
        val skippedSystemCount = packages.size - userPackages.size
        var scanned = 0
        var dangerCount = 0
        val results = mutableListOf<AppScanResult>()

        for (pkg in userPackages) {
            scanned++
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            emit(ScanProgress(appName, scanned, total, dangerCount, skippedSystemCount))

            val allPerms = pkg.requestedPermissions?.toList() ?: emptyList()
            val dangerousPerms = allPerms.filter { it in DANGEROUS_PERMISSIONS }

            // 病毒库扫描
            val virusMatches = virusDb.scanApp(pkg.packageName, appName, allPerms)

            // 盗版检测
            val piracyInfo = checkPiracy(context, pkg.packageName, appName, allPerms, pkg.signatures)

            // 危险判定：有危险权限 或 病毒匹配 或 盗版
            val isDangerous = dangerousPerms.isNotEmpty() || virusMatches.isNotEmpty() || piracyInfo.isPiracy

            if (isDangerous) dangerCount++

            // 构建危险原因列表
            val dangerReasons = mutableListOf<String>()
            dangerReasons.addAll(dangerousPerms.map { getDangerousPermissionLabel(it) })
            dangerReasons.addAll(virusMatches.map { "${it.name} (${it.type})" })
            if (piracyInfo.isPiracy) {
                dangerReasons.addAll(piracyInfo.reasons.map { "盗版特征: $it" })
            }

            results.add(
                AppScanResult(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "",
                    isSystemApp = false,
                    isDangerous = isDangerous,
                    dangerousPermissions = dangerousPerms,
                    allPermissions = allPerms,
                    installTime = pkg.firstInstallTime,
                    sourceDir = try {
                        pm.getApplicationInfo(pkg.packageName, 0).sourceDir
                    } catch (e: Exception) { "" }
                )
            )
        }

        Prefs.lastScanTime = System.currentTimeMillis()
        Prefs.lastScanDangerCount = dangerCount
        emit(results.toList())
    }.flowOn(Dispatchers.IO)

    fun checkPiracy(
        context: Context,
        packageName: String,
        appName: String,
        permissions: List<String>,
        signatures: Array<Signature>?
    ): PiracyInfo {
        val reasons = mutableListOf<String>()

        // 检查应用名是否包含破解关键词
        val appNameLower = appName.lowercase()
        for ((type, keywords) in PIRACY_SIGNATURES) {
            for (keyword in keywords) {
                if (appNameLower.contains(keyword.lowercase())) {
                    reasons.add("$type: $keyword")
                    break
                }
            }
        }

        // 检查包名是否包含破解关键词
        val pkgLower = packageName.lowercase()
        for ((type, keywords) in PIRACY_SIGNATURES) {
            for (keyword in keywords) {
                if (pkgLower.contains(keyword.lowercase()) && !reasons.any { it.contains(keyword) }) {
                    reasons.add("$type(包名): $keyword")
                    break
                }
            }
        }

        // 检查是否有Lucky Patcher相关权限
        val hasLuckyPatcher = permissions.any { it.contains("lucky") || it.contains("patcher") }
        if (hasLuckyPatcher) {
            reasons.add("Lucky Patcher权限痕迹")
        }

        // 检查签名是否与官方一致
        var signatureMatch = true
        if (OFFICIAL_SIGNATURES.containsKey(packageName)) {
            val currentSig = getSignatureMD5(signatures)
            val officialSig = OFFICIAL_SIGNATURES[packageName]
            if (currentSig != null && officialSig != null && currentSig != officialSig) {
                signatureMatch = false
                reasons.add("签名与官方不一致")
            }
        }

        // 检查是否安装在非标准位置（可能是破解版）
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (appInfo.sourceDir.contains("/data/app/") && !appInfo.sourceDir.contains("base.apk")) {
                // 非标准安装路径
            }
        } catch (e: Exception) {}

        return PiracyInfo(
            isPiracy = reasons.isNotEmpty(),
            reasons = reasons,
            signatureMatch = signatureMatch
        )
    }

    private fun getSignatureMD5(signatures: Array<Signature>?): String? {
        return try {
            if (signatures == null || signatures.isEmpty()) return null
            val md = MessageDigest.getInstance("MD5")
            md.update(signatures[0].toByteArray())
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun getDangerousPermissionLabel(perm: String): String = when (perm) {
        "android.permission.BIND_ACCESSIBILITY_SERVICE" -> "无障碍服务"
        "android.permission.SYSTEM_ALERT_WINDOW" -> "悬浮窗"
        else -> perm
    }

    fun getSkippedSystemCount(context: Context): Int {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            packages.count { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            0
        }
    }
}
