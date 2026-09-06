package com.chumian.miansecurity.emergency

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.ProcessInfo
import com.chumian.miansecurity.permission.RootHelper
import com.chumian.miansecurity.permission.ShizukuHelper
import com.chumian.miansecurity.util.Prefs

object ProcessManager {
    private val SYSTEM_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.phone",
        "com.android.settings", "com.android.providers.contacts",
        "com.android.providers.telephony", "com.android.providers.downloads",
        "com.android.providers.media", "com.android.providers.calendar",
        "com.android.calendar", "com.android.contacts",
        "com.android.dialer", "com.android.mms", "com.android.chrome",
        "com.google.android.gms", "com.google.android.gsf",
        "com.google.android.googlequicksearchbox", "com.nuance.xt9.input",
        "com.qualcomm", "com.motorola", "com.lge", "com.sec.android",
        "com.samsung", "com.huawei", "com.miui", "com.oppo",
        "com.vivo", "com.oneplus", "com.realme", "com.hihonor"
    )

    fun getRunningProcesses(context: Context): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = context.packageManager

            @Suppress("DEPRECATION")
            val runningApps = am.runningAppProcesses ?: return processes

            for (proc in runningApps) {
                val packageName = proc.processName.split(":")[0]
                val isSystem = isSystemPackage(packageName)
                val memoryInfo = am.getProcessMemoryInfo(intArrayOf(proc.pid))
                val memory = memoryInfo?.firstOrNull()?.totalPss?.toLong()?.times(1024) ?: 0

                processes.add(
                    ProcessInfo(
                        pid = proc.pid,
                        processName = proc.processName,
                        packageName = packageName,
                        memorySize = memory,
                        cpuUsage = 0f,
                        isSystem = isSystem
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return processes.sortedByDescending { it.memorySize }
    }

    fun getForegroundPackage(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                tasks[0].topActivity?.packageName ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun killBackgroundProcesses(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun forceStopProcess(context: Context, packageName: String): Boolean {
        var success = false

        if (Prefs.rootModeEnabled && RootHelper.isRootAvailable()) {
            success = RootHelper.killProcess(packageName)
        }

        if (!success && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()) {
            success = ShizukuHelper.killProcess(packageName)
        }

        if (!success) {
            success = killBackgroundProcesses(context, packageName)
        }

        return success
    }

    fun killAllProcesses(context: Context): List<String> {
        val killed = mutableListOf<String>()
        val processes = getRunningProcesses(context)
        val myPackage = context.packageName

        for (proc in processes) {
            if (proc.packageName == myPackage) continue
            if (Prefs.ignoreSystemApps && isSystemPackage(proc.packageName)) continue
            if (Prefs.isWhitelisted(proc.packageName)) continue

            if (forceStopProcess(context, proc.packageName)) {
                killed.add(proc.packageName)
            }
        }
        return killed
    }

    fun killAllAndRemoveForeground(context: Context): List<String> {
        val killed = killAllProcesses(context)
        val foreground = getForegroundPackage(context)
        if (foreground.isNotEmpty() && foreground != context.packageName) {
            forceStopProcess(context, foreground)
            if (!killed.contains(foreground)) {
                killed.add(foreground)
            }
        }
        return killed
    }

    private fun isSystemPackage(pkg: String): Boolean {
        if (SYSTEM_PACKAGES.contains(pkg)) return true
        if (pkg.startsWith("com.android.")) return true
        if (pkg.startsWith("android.")) return true
        if (pkg.startsWith("com.google.android.")) return true
        if (pkg.startsWith("com.qualcomm.")) return true
        if (pkg.startsWith("com.mediatek.")) return true
        return false
    }

    fun getInstalledApps(context: Context): List<com.chumian.miansecurity.model.AppInfo> {
        val apps = mutableListOf<com.chumian.miansecurity.model.AppInfo>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
            )
            for (pkg in packages) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val isFrozen = !pm.getApplicationEnabledSetting(pkg.packageName).let {
                        it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                                it == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    }
                    val apkSize = File(appInfo.sourceDir).length()
                    val dataSize = try {
                        File(appInfo.dataDir).walkTopDown().map { it.length() }.sum()
                    } catch (e: Exception) { 0 }
                    val cacheSize = try {
                        File(appInfo.dataDir, "cache").walkTopDown().map { it.length() }.sum()
                    } catch (e: Exception) { 0 }

                    apps.add(
                        com.chumian.miansecurity.model.AppInfo(
                            packageName = pkg.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            versionName = pkg.versionName ?: "",
                            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                pkg.longVersionCode.toInt()
                            } else {
                                @Suppress("DEPRECATION") pkg.versionCode
                            },
                            isSystemApp = isSystem,
                            isFrozen = isFrozen,
                            isRunning = isAppRunning(context, pkg.packageName),
                            appSize = apkSize,
                            dataSize = dataSize,
                            cacheSize = cacheSize,
                            installTime = pkg.firstInstallTime,
                            lastUpdateTime = pkg.lastUpdateTime,
                            targetSdk = appInfo.targetSdkVersion,
                            minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                appInfo.minSdkVersion
                            } else 0,
                            permissions = pkg.requestedPermissions?.toList() ?: emptyList(),
                            icon = pm.getApplicationIcon(appInfo)
                        )
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return apps.sortedBy { it.appName.lowercase() }
    }

    private fun isAppRunning(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            am.runningAppProcesses?.any { it.processName == packageName } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallApp(context: Context, packageName: String): Boolean {
        var success = false
        if (Prefs.rootModeEnabled && RootHelper.isRootAvailable()) {
            success = RootHelper.uninstallApp(packageName)
        }
        if (!success && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()) {
            success = ShizukuHelper.uninstallApp(packageName)
        }
        return success
    }

    fun freezeApp(context: Context, packageName: String): Boolean {
        var success = false
        if (Prefs.rootModeEnabled && RootHelper.isRootAvailable()) {
            success = RootHelper.freezeApp(packageName)
        }
        if (!success && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()) {
            success = ShizukuHelper.freezeApp(packageName)
        }
        return success
    }

    fun unfreezeApp(context: Context, packageName: String): Boolean {
        var success = false
        if (Prefs.rootModeEnabled && RootHelper.isRootAvailable()) {
            success = RootHelper.unfreezeApp(packageName)
        }
        if (!success && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()) {
            success = ShizukuHelper.unfreezeApp(packageName)
        }
        return success
    }

    fun clearAppCache(context: Context, packageName: String): Boolean {
        var success = false
        if (Prefs.rootModeEnabled && RootHelper.isRootAvailable()) {
            success = RootHelper.clearCache(packageName)
        }
        if (!success && ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()) {
            success = ShizukuHelper.clearCache(packageName)
        }
        return success
    }
}
