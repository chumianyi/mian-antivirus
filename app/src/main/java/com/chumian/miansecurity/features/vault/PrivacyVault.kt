package com.chumian.miansecurity.features.vault

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

data class VaultFile(
    val name: String,
    val originalPath: String,
    val encryptedPath: String,
    val size: Long,
    val encryptedSize: Long,
    val mimeType: String,
    val encryptTime: Long,
    val md5: String
)

class PrivacyVault(private val context: Context) {
    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { mkdirs() }
    }
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun getVaultFiles(): List<VaultFile> {
        return vaultDir.listFiles()?.map { file ->
            VaultFile(
                name = file.name.removeSuffix(".enc"),
                originalPath = "",
                encryptedPath = file.absolutePath,
                size = file.length(),
                encryptedSize = file.length(),
                mimeType = "application/octet-stream",
                encryptTime = file.lastModified(),
                md5 = calculateMD5(file)
            )
        } ?: emptyList()
    }

    suspend fun encryptFile(uri: Uri, fileName: String): VaultFile? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val outputFile = File(vaultDir, "$fileName.enc")
            val encryptedFile = EncryptedFile.Builder(
                context,
                outputFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val outputStream = encryptedFile.openFileOutput()
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            VaultFile(
                name = fileName,
                originalPath = uri.toString(),
                encryptedPath = outputFile.absolutePath,
                size = outputFile.length(),
                encryptedSize = outputFile.length(),
                mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                encryptTime = System.currentTimeMillis(),
                md5 = calculateMD5(outputFile)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun decryptFile(vaultFile: VaultFile, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(vaultFile.encryptedPath)
            val encryptedFile = EncryptedFile.Builder(
                context,
                inputFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream = encryptedFile.openFileInput()
            val outputStream = context.contentResolver.openOutputStream(outputUri) ?: return@withContext false

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFile(vaultFile: VaultFile): Boolean = withContext(Dispatchers.IO) {
        try {
            File(vaultFile.encryptedPath).delete()
        } catch (e: Exception) {
            false
        }
    }

    fun getVaultSize(): Long {
        return vaultDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    fun getVaultFileCount(): Int {
        return vaultDir.listFiles()?.size ?: 0
    }

    fun clearVault(): Boolean {
        return vaultDir.listFiles()?.all { it.delete() } ?: true
    }

    private fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = minOf(digitGroups, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format("%.2f %s", value, units[index])
    }
}
