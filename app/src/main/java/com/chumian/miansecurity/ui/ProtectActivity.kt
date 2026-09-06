package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityProtectBinding
import com.chumian.miansecurity.permission.PermissionHelper
import com.chumian.miansecurity.service.ProtectService
import com.chumian.miansecurity.util.Prefs

class ProtectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProtectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProtectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSwitches()
        updateStatus()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.security_protect)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSwitches() {
        binding.switchProtect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showProtectWarning()
            } else {
                ProtectService.stop(this)
                updateStatus()
            }
        }

        binding.switchMethodNotification.setOnCheckedChangeListener { _, isChecked ->
            Prefs.protectMethodNotification = isChecked
        }

        binding.switchMethodVolume.setOnCheckedChangeListener { _, isChecked ->
            Prefs.protectMethodVolume = isChecked
            if (isChecked && !PermissionHelper.hasAccessibility(this)) {
                Toast.makeText(this, "需要无障碍权限才能监听音量键", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchMethodShake.setOnCheckedChangeListener { _, isChecked ->
            Prefs.protectMethodShake = isChecked
        }

        binding.switchVolumeProtect.setOnCheckedChangeListener { _, isChecked ->
            Prefs.volumeProtectEnabled = isChecked
            if (isChecked && !Prefs.protectEnabled) {
                Toast.makeText(this, "需要先启动实时守护", Toast.LENGTH_SHORT).show()
                binding.switchVolumeProtect.isChecked = false
                Prefs.volumeProtectEnabled = false
            }
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            Prefs.vibrateOnTrigger = isChecked
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            Prefs.autoStartProtect = isChecked
        }
    }

    private fun showProtectWarning() {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.protect_warning_detail)
            .setPositiveButton(R.string.confirm) { _, _ ->
                if (!PermissionHelper.hasAccessibility(this)) {
                    PermissionHelper.requestAccessibility(this)
                    binding.switchProtect.isChecked = false
                    return@setPositiveButton
                }
                if (!PermissionHelper.hasOverlay(this)) {
                    PermissionHelper.requestOverlay(this)
                    binding.switchProtect.isChecked = false
                    return@setPositiveButton
                }
                ProtectService.start(this)
                updateStatus()
                Toast.makeText(this, "实时守护已启动", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.switchProtect.isChecked = false
            }
            .show()
    }

    private fun updateStatus() {
        val running = Prefs.protectEnabled
        binding.switchProtect.isChecked = running
        binding.tvStatus.text = if (running) getString(R.string.protect_running) else getString(R.string.protect_stopped)

        binding.switchMethodNotification.isChecked = Prefs.protectMethodNotification
        binding.switchMethodVolume.isChecked = Prefs.protectMethodVolume
        binding.switchMethodShake.isChecked = Prefs.protectMethodShake
        binding.switchVolumeProtect.isChecked = Prefs.volumeProtectEnabled
        binding.switchVibrate.isChecked = Prefs.vibrateOnTrigger
        binding.switchAutoStart.isChecked = Prefs.autoStartProtect

        if (Prefs.volumeViolationCount > 0) {
            binding.tvViolationInfo.text = "违规次数：${Prefs.volumeViolationCount}\n上次：${Prefs.lastVolumeViolation}"
        } else {
            binding.tvViolationInfo.text = getString(R.string.no_violation)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
