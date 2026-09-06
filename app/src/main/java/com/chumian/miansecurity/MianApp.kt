package com.chumian.miansecurity

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.chumian.miansecurity.core.Prefs
import com.google.android.material.color.DynamicColors
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MianApp : Application() {
    companion object {
        lateinit var instance: MianApp
            private set
        const val CHANNEL_PROTECT = "protect"
        const val CHANNEL_SCAN = "scan"
        const val CHANNEL_EMERGENCY = "emergency"
        const val CHANNEL_VOLUME = "volume"
        const val CHANNEL_CRASH = "crash"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        createNotificationChannels()
        setupCrashHandler()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(thread, throwable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // 调用默认处理器（让系统显示崩溃对话框或静默退出）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        try {
            val crashDir = File(getExternalFilesDir(null), "crash_logs")
            if (!crashDir.exists()) crashDir.mkdirs()

            val fileName = "crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val crashFile = File(crashDir, fileName)

            PrintWriter(FileWriter(crashFile)).use { writer ->
                writer.println("=== 眠.杀毒软件 崩溃日志 ===")
                writer.println("崩溃时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.println("线程: ${thread.name} (ID: ${thread.id})")
                writer.println("进程ID: ${Process.myPid()}")
                writer.println()

                // 设备信息
                writer.println("=== 设备信息 ===")
                writer.println("品牌: ${Build.BRAND}")
                writer.println("型号: ${Build.MODEL}")
                writer.println("Android版本: ${Build.VERSION.RELEASE}")
                writer.println("API等级: ${Build.VERSION.SDK_INT}")
                writer.println("构建号: ${Build.DISPLAY}")
                writer.println()

                // 应用信息
                writer.println("=== 应用信息 ===")
                try {
                    val pkgInfo = packageManager.getPackageInfo(packageName, 0)
                    writer.println("版本名: ${pkgInfo.versionName}")
                    writer.println("版本号: ${pkgInfo.versionCode}")
                } catch (e: Exception) {
                    writer.println("版本信息: 未知")
                }
                writer.println("Shizuku授权: ${com.chumian.miansecurity.core.ShizukuHelper.isGranted()}")
                writer.println()

                // 崩溃堆栈
                writer.println("=== 崩溃堆栈 ===")
                throwable.printStackTrace(writer)
                writer.println()

                // 原因链
                var cause = throwable.cause
                var depth = 1
                while (cause != null && depth < 10) {
                    writer.println("=== 原因 $depth ===")
                    cause.printStackTrace(writer)
                    cause = cause.cause
                    depth++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCrashLogs(): List<File> {
        val crashDir = File(getExternalFilesDir(null), "crash_logs")
        return if (crashDir.exists()) {
            crashDir.listFiles { file -> file.extension == "txt" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun clearCrashLogs() {
        val crashDir = File(getExternalFilesDir(null), "crash_logs")
        if (crashDir.exists()) {
            crashDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(CHANNEL_PROTECT, "实时守护", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_SCAN, "扫描通知", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_EMERGENCY, "急救箱", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_VOLUME, "音量保护", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_CRASH, "崩溃报告", NotificationManager.IMPORTANCE_HIGH)
            ).forEach { nm.createNotificationChannel(it) }
        }
    }
}
