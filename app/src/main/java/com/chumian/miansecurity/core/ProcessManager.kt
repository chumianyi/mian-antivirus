package com.chumian.miansecurity.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.ProcessInfo

object ProcessManager {
    private val SYSTEM_PROCESSES = setOf(
        "system", "com.android.systemui", "com.android.phone",
        "com.android.settings", "com.google.android.gms",
        "android.process.acore", "com.android.providers.media"
    )

    fun getRunningProcesses(context: Context): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = context.packageManager
            val runningApps = am.runningAppProcesses ?: return emptyList()

            for (proc in runningApps) {
                val pkg = proc.processName
                val isSystem = SYSTEM_PROCESSES.any { pkg.startsWith(it) } ||
                        pkg.startsWith("com.android.") || pkg.startsWith("android.")
                val memInfo = am.getProcessMemoryInfo(intArrayOf(proc.pid))
                val memSize = if (memInfo.isNotEmpty()) memInfo[0].totalPss * 1024L else 0L

                processes.add(
                    ProcessInfo(
                        pid = proc.pid,
                        processName = proc.processName,
                        packageName = pkg,
                        memorySize = memSize,
                        isSystem = isSystem
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return processes
    }

    fun killAllProcesses(context: Context): List<String> {
        val killed = mutableListOf<String>()
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = am.runningAppProcesses ?: return emptyList()
            val myPackage = context.packageName

            for (proc in runningApps) {
                val pkg = proc.processName
                if (pkg == myPackage) continue
                if (Prefs.isWhitelisted(pkg)) continue
                if (isSystemProtected(pkg)) continue

                // 先用ActivityManager杀
                try {
                    am.killBackgroundProcesses(pkg)
                } catch (e: Exception) {}

                // 再用Shizuku强杀
                if (ShizukuHelper.isGranted()) {
                    ShizukuHelper.killProcess(pkg)
                    ShizukuHelper.killProcessByPid(proc.pid)
                }

                killed.add(pkg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return killed
    }

    fun killProcess(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            if (ShizukuHelper.isGranted()) {
                ShizukuHelper.killProcess(packageName)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getForegroundPackage(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.runningAppProcesses
            tasks?.firstOrNull { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }?.processName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun isSystemProtected(pkg: String): Boolean {
        if (SYSTEM_PROCESSES.contains(pkg)) return true
        if (pkg.startsWith("com.android.")) return true
        if (pkg.startsWith("android.")) return true
        if (pkg.startsWith("com.google.android.gms")) return true
        if (pkg.startsWith("com.qualcomm.")) return true
        return false
    }

    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val apps = mutableListOf<Pair<String, String>>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            for (pkg in packages) {
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
                } catch (e: Exception) {
                    pkg.packageName
                }
                apps.add(Pair(pkg.packageName, appName))
            }
        } catch (e: Exception) {}
        return apps
    }
}
