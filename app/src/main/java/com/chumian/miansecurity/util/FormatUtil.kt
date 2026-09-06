package com.chumian.miansecurity.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtil {
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "从未"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%d时%d分%d秒", hours, minutes, secs)
            minutes > 0 -> String.format("%d分%d秒", minutes, secs)
            else -> String.format("%d秒", secs)
        }
    }

    fun formatNumber(num: Int): String {
        return String.format("%,d", num)
    }

    fun getRiskColor(risk: String): String = when (risk) {
        "高风险" -> "#FF5252"
        "中风险" -> "#FFB74D"
        "低风险" -> "#69F0AE"
        else -> "#40C4FF"
    }

    fun truncatePath(path: String, maxLength: Int = 50): String {
        return if (path.length > maxLength) {
            "..." + path.substring(path.length - maxLength + 3)
        } else path
    }
}
