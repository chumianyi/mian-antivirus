package com.chumian.miansecurity.core

import android.content.Context
import android.content.pm.PackageManager
import com.chumian.miansecurity.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UpdateChecker {
    private const val SERVER_HOST = "103.236.99.177"
    private const val SERVER_PORT = 24512
    private const val TIMEOUT_MS = 5000

    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION") pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun checkUpdate(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val version = getCurrentVersion(context)
            val versionCode = getCurrentVersionCode(context)
            val message = "CHECK_UPDATE|$version|$versionCode|android"
            val socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS
            val address = InetAddress.getByName(SERVER_HOST)
            val sendData = message.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, address, SERVER_PORT)
            socket.send(sendPacket)

            val receiveData = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)
            val response = String(receivePacket.data, 0, receivePacket.length)
            socket.close()

            parseResponse(response)
        } catch (e: Exception) {
            UpdateInfo(false, "", 0, "", "", 0, false)
        }
    }

    private fun parseResponse(response: String): UpdateInfo {
        return try {
            val parts = response.split("|")
            if (parts[0] == "UPDATE_AVAILABLE" && parts.size >= 7) {
                UpdateInfo(
                    hasUpdate = true,
                    latestVersion = parts[1],
                    latestVersionCode = parts[2].toIntOrNull() ?: 0,
                    changelog = parts[3],
                    downloadUrl = parts[4],
                    fileSize = parts[5].toLongOrNull() ?: 0,
                    forceUpdate = parts[6] == "true"
                )
            } else {
                UpdateInfo(false, "", 0, "", "", 0, false)
            }
        } catch (e: Exception) {
            UpdateInfo(false, "", 0, "", "", 0, false)
        }
    }
}
