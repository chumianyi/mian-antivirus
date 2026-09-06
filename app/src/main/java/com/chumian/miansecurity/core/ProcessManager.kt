package com.chumian.miansecurity.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.ProcessInfo

object ProcessManager {
    private val SYSTEM_PROCESSES = setOf(
        "system", "com.android.systemui", "com.android.phone",
        "com.android.settings", "com.google.android.gms",
        "android.process.acore", "com.android.providers.media",
        "com.android.nfc", "com.android.bluetooth", "com.android.server.telecom"
    )

    fun getRunningProcesses(context: Context): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val pm = context.packageManager

        // 优先使用Shizuku执行ps -A获取所有进程
        if (ShizukuHelper.isGranted()) {
            try {
                val output = ShizukuHelper.runCommand("ps -A -o PID,USER,STAT,%CPU,RSS,NAME")
                val lines = output.lines()
                if (lines.size > 1) {
                    for (i in 1 until lines.size) {
                        val line = lines[i].trim()
                        if (line.isEmpty()) continue
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size < 5) continue

                        try {
                            val pid = parts[0].toInt()
                            val user = parts[1]
                            val state = parts[2]
                            val cpuPercent = parts[3].toFloatOrNull() ?: 0f
                            val rssKb = parts[4].toLongOrNull() ?: 0L
                            val memorySize = rssKb * 1024L
                            val processName = if (parts.size > 5) parts[5] else "unknown"
                            val packageName = extractPackageName(processName)

                            val isSystem = isSystemProcess(processName, user)

                            processes.add(
                                ProcessInfo(
                                    pid = pid,
                                    processName = processName,
                                    packageName = packageName,
                                    memorySize = memorySize,
                                    cpuPercent = cpuPercent,
                                    isSystem = isSystem,
                                    user = user,
                                    state = state
                                )
                            )
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 如果Shizuku获取失败或为空，使用ActivityManager补充
        if (processes.isEmpty()) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningApps = am.runningAppProcesses ?: return emptyList()

                for (proc in runningApps) {
                    val pkg = proc.processName
                    val isSystem = isSystemProcess(pkg, "")
                    val memInfo = am.getProcessMemoryInfo(intArrayOf(proc.pid))
                    val memSize = if (memInfo.isNotEmpty()) memInfo[0].totalPss * 1024L else 0L

                    processes.add(
                        ProcessInfo(
                            pid = proc.pid,
                            processName = proc.processName,
                            packageName = pkg,
                            memorySize = memSize,
                            cpuPercent = 0f,
                            isSystem = isSystem,
                            user = "",
                            state = ""
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return processes.sortedByDescending { it.memorySize }
    }

    fun getUserProcesses(context: Context): List<ProcessInfo> {
        return getRunningProcesses(context).filter { !it.isSystem }
    }

    fun getSystemProcesses(context: Context): List<ProcessInfo> {
        return getRunningProcesses(context).filter { it.isSystem }
    }

    fun killAllProcesses(context: Context): List<String> {
        val killed = mutableListOf<String>()
        val myPackage = context.packageName

        val processes = getRunningProcesses(context)
        for (proc in processes) {
            if (proc.packageName == myPackage) continue
            if (proc.processName == myPackage) continue
            if (Prefs.isWhitelisted(proc.packageName)) continue
            if (isSystemProtected(proc.packageName)) continue
            if (isSystemProtected(proc.processName)) continue

            // 先用ActivityManager杀
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses(proc.packageName)
            } catch (e: Exception) {}

            // 再用Shizuku强杀
            if (ShizukuHelper.isGranted()) {
                ShizukuHelper.killProcessByPid(proc.pid)
                ShizukuHelper.killProcess(proc.packageName)
            }

            killed.add(proc.processName)
        }
        return killed
    }

    fun killProcess(context: Context, pid: Int, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            if (ShizukuHelper.isGranted()) {
                ShizukuHelper.killProcessByPid(pid)
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

    fun getTotalMemory(context: Context): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (e: Exception) {
            0L
        }
    }

    fun getAvailableMemory(context: Context): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (e: Exception) {
            0L
        }
    }

    private fun extractPackageName(processName: String): String {
        // 进程名通常就是包名，或者包名:子进程
        return if (processName.contains(":")) {
            processName.substringBefore(":")
        } else {
            processName
        }
    }

    private fun isSystemProcess(processName: String, user: String): Boolean {
        if (user == "root" || user == "system") return true
        if (SYSTEM_PROCESSES.any { processName.startsWith(it) }) return true
        if (processName.startsWith("com.android.")) return true
        if (processName.startsWith("android.")) return true
        if (processName.startsWith("com.google.android.")) return true
        if (processName.startsWith("com.qualcomm.")) return true
        if (processName.startsWith("com.mediatek.")) return true
        if (processName.startsWith("vendor.")) return true
        if (processName.startsWith("/")) return true // 内核进程
        if (processName.startsWith("[")) return true // 内核线程
        return false
    }

    private fun isSystemProtected(pkg: String): Boolean {
        if (SYSTEM_PROCESSES.contains(pkg)) return true
        if (pkg.startsWith("com.android.")) return true
        if (pkg.startsWith("android.")) return true
        if (pkg.startsWith("com.google.android.gms")) return true
        if (pkg.startsWith("com.qualcomm.")) return true
        if (pkg.startsWith("com.mediatek.")) return true
        if (pkg.startsWith("vendor.")) return true
        if (pkg.startsWith("system")) return true
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

    fun getUserInstalledApps(context: Context): List<Pair<String, String>> {
        val apps = mutableListOf<Pair<String, String>>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            for (pkg in packages) {
                val appInfo = try {
                    pm.getApplicationInfo(pkg.packageName, 0)
                } catch (e: Exception) {
                    continue
                }
                // 只返回用户安装的应用（非系统应用）
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    apps.add(Pair(pkg.packageName, appName))
                }
            }
        } catch (e: Exception) {}
        return apps
    }
}
