package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivitySettingsBinding
import com.chumian.miansecurity.scan.VirusDatabase
import com.chumian.miansecurity.update.UpdateChecker
import com.chumian.miansecurity.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSettings()
        updateSettings()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.settings)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettings() {
        binding.itemTheme.setOnClickListener {
            val themes = arrayOf("跟随系统", "深色", "浅色")
            val current = when (Prefs.theme) {
                "dark" -> 1
                "light" -> 2
                else -> 0
            }
            AlertDialog.Builder(this)
                .setTitle("选择主题")
                .setSingleChoiceItems(themes, current) { dialog, which ->
                    val theme = when (which) {
                        1 -> "dark"
                        2 -> "light"
                        else -> "system"
                    }
                    Prefs.theme = theme
                    when (theme) {
                        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    updateSettings()
                    dialog.dismiss()
                }
                .show()
        }

        binding.switchScanSystem.setOnCheckedChangeListener { _, isChecked ->
            Prefs.scanSystemApps = isChecked
        }

        binding.switchScanApk.setOnCheckedChangeListener { _, isChecked ->
            Prefs.scanArchiveFiles = isChecked
        }

        binding.itemUpdateDb.setOnClickListener {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    VirusDatabase.updateDatabase(this@SettingsActivity)
                }
                if (success) {
                    updateSettings()
                    Toast.makeText(this@SettingsActivity, "病毒库已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "病毒库更新失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemCheckUpdate.setOnClickListener {
            scope.launch {
                val info = withContext(Dispatchers.IO) {
                    UpdateChecker.checkUpdate(this@SettingsActivity)
                }
                if (info.hasUpdate) {
                    showUpdateDialog(info)
                } else {
                    Toast.makeText(this@SettingsActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.itemAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showUpdateDialog(info: com.chumian.miansecurity.model.UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage("版本：${info.latestVersion}\n\n${info.changelog}")
            .setPositiveButton("立即更新") { _, _ ->
                if (info.downloadUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                }
            }
            .setNegativeButton("暂不更新", null)
            .show()
    }

    private fun showAboutDialog() {
        val version = UpdateChecker.getCurrentVersion(this)
        val versionCode = UpdateChecker.getCurrentVersionCode(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(
                "眠. v$version ($versionCode)\n\n" +
                "开发：初眠\n" +
                "GitHub：github.com/chumianyi/mian-antivirus\n\n" +
                "一个功能丰富的Android安全防护工具，支持扫毒、急救箱、实时守护等功能。"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    private fun updateSettings() {
        binding.tvThemeValue.text = when (Prefs.theme) {
            "dark" -> "深色"
            "light" -> "浅色"
            else -> "跟随系统"
        }

        binding.switchScanSystem.isChecked = Prefs.scanSystemApps
        binding.switchScanApk.isChecked = Prefs.scanArchiveFiles

        binding.tvDbVersion.text = "v${VirusDatabase.getDatabaseVersion()}"

        val version = UpdateChecker.getCurrentVersion(this)
        binding.tvVersion.text = "v$version"
    }
}
