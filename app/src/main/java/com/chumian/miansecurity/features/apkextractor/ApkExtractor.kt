package com.chumian.miansecurity.features.apkextractor

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class ApkInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val installTime: Long
)

class ApkExtractor(private val context: Context) {
    fun getInstalledApps(): List<ApkInfo> {
        val apps = mutableListOf<ApkInfo>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            for (pkg in packages) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val sourceDir = appInfo.sourceDir
                    val apkFile = File(sourceDir)
                    val apkSize = if (apkFile.exists()) apkFile.length() else 0L
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    apps.add(
                        ApkInfo(
                            packageName = pkg.packageName,
                            appName = appName,
                            versionName = pkg.versionName ?: "",
                            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                pkg.longVersionCode
                            } else {
                                @Suppress("DEPRECATION")
                                pkg.versionCode.toLong()
                            },
                            sourceDir = sourceDir,
                            apkSize = apkSize,
                            isSystemApp = isSystem,
                            installTime = pkg.firstInstallTime
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return apps.sortedBy { it.appName.lowercase() }
    }

    fun getUserApps(): List<ApkInfo> {
        return getInstalledApps().filter { !it.isSystemApp }
    }

    fun getSystemApps(): List<ApkInfo> {
        return getInstalledApps().filter { it.isSystemApp }
    }

    suspend fun extractApk(apkInfo: ApkInfo, outputDir: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFileName = "${apkInfo.appName.replace("[^a-zA-Z0-9]".toRegex(), "_")}_v${apkInfo.versionName}.apk"
            val outputFile = File(outputDir, outputFileName)

            val sourceFile = File(apkInfo.sourceDir)
            if (!sourceFile.exists()) {
                return@withContext null
            }

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun extractMultipleApks(apkList: List<ApkInfo>, outputDir: File): List<File> = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<File>()
        for (apkInfo in apkList) {
            val file = extractApk(apkInfo, outputDir)
            if (file != null) {
                extractedFiles.add(file)
            }
        }
        extractedFiles
    }

    fun getDefaultOutputDir(): File {
        val dir = File(context.getExternalFilesDir(null), "extracted_apks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExtractedApks(): List<File> {
        val dir = getDefaultOutputDir()
        return if (dir.exists()) {
            dir.listFiles { file -> file.extension == "apk" }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun deleteExtractedApk(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun shareApk(file: File) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享APK").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getApkSizeFormatted(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${"%.2f".format(size / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(size / (1024.0 * 1024 * 1024))} GB"
        }
    }
}
