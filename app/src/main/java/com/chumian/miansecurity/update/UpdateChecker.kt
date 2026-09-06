package com.chumian.miansecurity.update

import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UpdateChecker {
    private const val SERVER_IP = "103.236.99.177"
    private const val SERVER_PORT = 24512
    private const val TIMEOUT = 5000

    suspend fun checkUpdate(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val currentVersionCode = getCurrentVersionCode(context)

            val socket = DatagramSocket()
            socket.soTimeout = TIMEOUT

            val request = "CHECK_UPDATE|$currentVersion|$currentVersionCode|android"
            val sendData = request.toByteArray()
            val address = InetAddress.getByName(SERVER_IP)
            val sendPacket = DatagramPacket(sendData, sendData.size, address, SERVER_PORT)
            socket.send(sendPacket)

            val receiveData = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)

            val response = String(receivePacket.data, 0, receivePacket.length)
            socket.close()

            parseResponse(response, currentVersionCode)
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(hasUpdate = false)
        }
    }

    private fun parseResponse(response: String, currentVersionCode: Int): UpdateInfo {
        return try {
            val parts = response.split("|")
            if (parts.size < 2) return UpdateInfo(hasUpdate = false)

            val status = parts[0]
            if (status != "UPDATE_AVAILABLE") return UpdateInfo(hasUpdate = false)

            val latestVersion = parts.getOrElse(1) { "" }
            val latestVersionCode = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
            val changelog = parts.getOrElse(3) { "" }.replace("\\n", "\n")
            val downloadUrl = parts.getOrElse(4) { "" }
            val fileSize = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0
            val forceUpdate = parts.getOrElse(6) { "false" }.toBoolean()

            UpdateInfo(
                hasUpdate = latestVersionCode > currentVersionCode,
                latestVersion = latestVersion,
                latestVersionCode = latestVersionCode,
                changelog = changelog,
                downloadUrl = downloadUrl,
                fileSize = fileSize,
                forceUpdate = forceUpdate
            )
        } catch (e: Exception) {
            UpdateInfo(hasUpdate = false)
        }
    }

    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            1
        }
    }
}
