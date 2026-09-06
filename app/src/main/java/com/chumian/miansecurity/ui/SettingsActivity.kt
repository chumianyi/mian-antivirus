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
import com.chumian.miansecurity.util.FormatUtil
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
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radio_dark -> "dark"
                R.id.radio_light -> "light"
                else -> "system"
            }
            Prefs.theme = theme
            when (theme) {
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        binding.switchScanSystem.setOnCheckedChangeListener { _, isChecked ->
            Prefs.scanSystemApps = isChecked
        }

        binding.switchScanArchive.setOnCheckedChangeListener { _, isChecked ->
            Prefs.scanArchiveFiles = isChecked
        }

        binding.switchQuarantine.setOnCheckedChangeListener { _, isChecked ->
            Prefs.quarantineThreats = isChecked
        }

        binding.switchNotifyThreat.setOnCheckedChangeListener { _, isChecked ->
            Prefs.notifyOnThreat = isChecked
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            Prefs.vibrateOnTrigger = isChecked
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            Prefs.autoStartProtect = isChecked
        }

        binding.switchIgnoreSystem.setOnCheckedChangeListener { _, isChecked ->
            Prefs.ignoreSystemApps = isChecked
        }

        binding.btnUpdateDb.setOnClickListener {
            scope.launch {
                val success = VirusDatabase.updateDatabase(this@SettingsActivity)
                if (success) {
                    updateSettings()
                    Toast.makeText(this@SettingsActivity, R.string.virus_db_updated, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, R.string.virus_db_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnCheckUpdate.setOnClickListener {
            scope.launch {
                val info = withContext(Dispatchers.IO) {
                    UpdateChecker.checkUpdate(this@SettingsActivity)
                }
                if (info.hasUpdate) {
                    showUpdateDialog(info)
                } else {
                    Toast.makeText(this@SettingsActivity, R.string.no_update, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chumianyi/mian-antivirus"))
            startActivity(intent)
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showUpdateDialog(info: com.chumian.miansecurity.model.UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.update_available)
            .setMessage("版本：${info.latestVersion}\n\n${info.changelog}")
            .setPositiveButton(R.string.update_now) { _, _ ->
                if (info.downloadUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                }
            }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    private fun showAboutDialog() {
        val version = UpdateChecker.getCurrentVersion(this)
        val versionCode = UpdateChecker.getCurrentVersionCode(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(
                "眠杀毒 v$version ($versionCode)\n\n" +
                "开发：初眠\n" +
                "GitHub：github.com/chumianyi/mian-antivirus\n\n" +
                "一个功能丰富的Android安全防护工具，支持扫毒、急救箱、实时守护等功能。"
            )
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    private fun updateSettings() {
        when (Prefs.theme) {
            "dark" -> binding.radioDark.isChecked = true
            "light" -> binding.radioLight.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }

        binding.switchScanSystem.isChecked = Prefs.scanSystemApps
        binding.switchScanArchive.isChecked = Prefs.scanArchiveFiles
        binding.switchQuarantine.isChecked = Prefs.quarantineThreats
        binding.switchNotifyThreat.isChecked = Prefs.notifyOnThreat
        binding.switchVibrate.isChecked = Prefs.vibrateOnTrigger
        binding.switchAutoStart.isChecked = Prefs.autoStartProtect
        binding.switchIgnoreSystem.isChecked = Prefs.ignoreSystemApps

        binding.tvDbVersion.text = "病毒库版本：${VirusDatabase.getDatabaseVersion()}"
        binding.tvDbUpdate.text = "上次更新：${if (Prefs.virusDbLastUpdate > 0) FormatUtil.formatTime(Prefs.virusDbLastUpdate) else "从未"}"

        val version = UpdateChecker.getCurrentVersion(this)
        binding.tvVersion.text = "版本：v$version"
    }
}
