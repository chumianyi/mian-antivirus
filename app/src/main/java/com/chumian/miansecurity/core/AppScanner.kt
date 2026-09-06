package com.chumian.miansecurity.core

import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.AppScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object AppScanner {
    // 只有这两个权限算危险权限
    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    // 系统重要包，默认信任
    private val TRUSTED_SYSTEM_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.phone",
        "com.android.settings", "com.android.providers.contacts",
        "com.android.providers.telephony", "com.android.providers.downloads",
        "com.android.providers.media", "com.google.android.gms",
        "com.google.android.gsf", "com.android.chrome"
    )

    data class ScanProgress(
        val currentApp: String,
        val scanned: Int,
        val total: Int,
        val dangerCount: Int
    )

    fun scanApps(context: Context): Flow<Any> = flow {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val total = packages.size
        var scanned = 0
        var dangerCount = 0
        val results = mutableListOf<AppScanResult>()

        for (pkg in packages) {
            scanned++
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            emit(ScanProgress(appName, scanned, total, dangerCount))

            val isSystem = isSystemPackage(pkg.packageName)
            val allPerms = pkg.requestedPermissions?.toList() ?: emptyList()
            val dangerousPerms = allPerms.filter { it in DANGEROUS_PERMISSIONS }

            // 系统应用且在信任列表中，不算危险
            val isTrustedSystem = isSystem && TRUSTED_SYSTEM_PACKAGES.any { pkg.packageName.startsWith(it) }
            val isDangerous = !isTrustedSystem && dangerousPerms.isNotEmpty()

            if (isDangerous) dangerCount++

            results.add(
                AppScanResult(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "",
                    isSystemApp = isSystem,
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

    private fun isSystemPackage(pkg: String): Boolean {
        if (TRUSTED_SYSTEM_PACKAGES.contains(pkg)) return true
        if (pkg.startsWith("com.android.")) return true
        if (pkg.startsWith("android.")) return true
        if (pkg.startsWith("com.google.android.")) return true
        if (pkg.startsWith("com.qualcomm.")) return true
        if (pkg.startsWith("com.mediatek.")) return true
        if (pkg.startsWith("com.huawei.")) return true
        if (pkg.startsWith("com.miui.")) return true
        if (pkg.startsWith("com.oppo.")) return true
        if (pkg.startsWith("com.vivo.")) return true
        return false
    }

    fun getDangerousPermissionLabel(perm: String): String = when (perm) {
        "android.permission.BIND_ACCESSIBILITY_SERVICE" -> "无障碍服务"
        "android.permission.SYSTEM_ALERT_WINDOW" -> "悬浮窗"
        else -> perm
    }
}
