package com.chumian.miansecurity.features.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.chumian.miansecurity.features.deviceinfo.DeviceInfo
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FeedbackInfo(
    val type: String,
    val description: String,
    val deviceInfo: String,
    val appVersion: String,
    val timestamp: Long
)

class FeedbackManager(private val context: Context) {
    companion object {
        const val FEEDBACK_EMAIL = "feedback@miansecurity.app"
        const val GITHUB_ISSUES_URL = "https://github.com/chumianyi/mian-antivirus/issues"
    }

    val feedbackTypes = listOf(
        "Bug报告",
        "功能建议",
        "性能问题",
        "界面问题",
        "病毒误报",
        "病毒漏报",
        "其他问题"
    )

    fun sendEmailFeedback(type: String, description: String): Boolean {
        return try {
            val feedbackInfo = buildFeedbackInfo(type, description)
            val subject = "[眠.杀毒] $type - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            val body = buildEmailBody(feedbackInfo)

            // 保存日志到文件
            val logFile = saveLogFile(feedbackInfo)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                if (logFile != null) {
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 检查是否有邮件应用
            val packageManager = context.packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            if (activities.isNotEmpty()) {
                context.startActivity(Intent.createChooser(intent, "选择邮件应用").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openGithubIssues() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ISSUES_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildFeedbackInfo(type: String, description: String): FeedbackInfo {
        val deviceInfo = DeviceInfo.getDeviceSummary(context)
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        return FeedbackInfo(
            type = type,
            description = description,
            deviceInfo = deviceInfo,
            appVersion = appVersion,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun buildEmailBody(info: FeedbackInfo): String {
        return buildString {
            append("【问题类型】\n")
            append(info.type)
            append("\n\n")
            append("【问题描述】\n")
            append(info.description)
            append("\n\n")
            append("【设备信息】\n")
            append(info.deviceInfo)
            append("\n\n")
            append("【应用版本】\n")
            append(info.appVersion)
            append("\n\n")
            append("【系统版本】\n")
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append("\n\n")
            append("【提交时间】\n")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.timestamp)))
            append("\n\n")
            append("---\n")
            append("此邮件由眠.杀毒软件自动生成\n")
        }
    }

    private fun saveLogFile(info: FeedbackInfo): File? {
        return try {
            val logDir = File(context.getExternalFilesDir(null), "feedback_logs")
            if (!logDir.exists()) logDir.mkdirs()

            val fileName = "feedback_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val logFile = File(logDir, fileName)

            FileWriter(logFile).use { writer ->
                writer.write("=== 眠.杀毒软件 反馈日志 ===\n\n")
                writer.write("问题类型: ${info.type}\n")
                writer.write("问题描述: ${info.description}\n")
                writer.write("设备信息: ${info.deviceInfo}\n")
                writer.write("应用版本: ${info.appVersion}\n")
                writer.write("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                writer.write("提交时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.timestamp))}\n\n")

                // 写入详细设备信息
                writer.write("=== 详细设备信息 ===\n")
                val allInfo = DeviceInfo.getAllInfo(context)
                for (item in allInfo) {
                    writer.write("[${item.category}] ${item.label}: ${item.value}\n")
                }

                // 写入应用日志
                writer.write("\n=== 应用日志 ===\n")
                writer.write("Shizuku状态: ${com.chumian.miansecurity.core.ShizukuHelper.isGranted()}\n")
                writer.write("上次扫描时间: ${com.chumian.miansecurity.core.Prefs.lastScanTime}\n")
                writer.write("上次扫描危险数: ${com.chumian.miansecurity.core.Prefs.lastScanDangerCount}\n")
            }

            logFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFeedbackHistory(): List<FeedbackInfo> {
        // 从SharedPreferences读取历史
        val prefs = context.getSharedPreferences("feedback_history", Context.MODE_PRIVATE)
        val count = prefs.getInt("count", 0)
        val history = mutableListOf<FeedbackInfo>()
        for (i in 0 until count) {
            val type = prefs.getString("type_$i", "") ?: ""
            val description = prefs.getString("description_$i", "") ?: ""
            val timestamp = prefs.getLong("timestamp_$i", 0)
            if (type.isNotEmpty()) {
                history.add(
                    FeedbackInfo(
                        type = type,
                        description = description,
                        deviceInfo = "",
                        appVersion = "",
                        timestamp = timestamp
                    )
                )
            }
        }
        return history.reversed()
    }

    fun saveFeedbackToHistory(info: FeedbackInfo) {
        val prefs = context.getSharedPreferences("feedback_history", Context.MODE_PRIVATE)
        val count = prefs.getInt("count", 0)
        prefs.edit()
            .putString("type_$count", info.type)
            .putString("description_$count", info.description)
            .putLong("timestamp_$count", info.timestamp)
            .putInt("count", count + 1)
            .apply()
    }
}
