package com.chumian.miansecurity.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityPermissionGuideBinding
import com.chumian.miansecurity.permission.PermissionHelper
import com.chumian.miansecurity.permission.ShizukuHelper

class PermissionGuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
        updateStatus()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.permission_guide)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupButtons() {
        binding.btnAccessibility.setOnClickListener {
            PermissionHelper.requestAccessibility(this)
        }

        binding.btnOverlay.setOnClickListener {
            PermissionHelper.requestOverlay(this)
        }

        binding.btnStorage.setOnClickListener {
            PermissionHelper.requestManageStorage(this)
        }

        binding.btnUsageStats.setOnClickListener {
            PermissionHelper.requestUsageStats(this)
        }

        binding.btnBattery.setOnClickListener {
            PermissionHelper.requestBatteryOptimization(this)
        }

        binding.btnShizuku.setOnClickListener {
            if (!ShizukuHelper.isInstalled(this)) {
                Toast.makeText(this, "请先安装Shizuku应用", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!ShizukuHelper.isAvailable()) {
                Toast.makeText(this, "请先启动Shizuku服务", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ShizukuHelper.requestPermission()
        }

        binding.btnDone.setOnClickListener {
            if (PermissionHelper.hasAccessibility(this) && PermissionHelper.hasOverlay(this)) {
                finish()
            } else {
                Toast.makeText(this, "请至少授予无障碍和悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus() {
        val hasAcc = PermissionHelper.hasAccessibility(this)
        val hasOv = PermissionHelper.hasOverlay(this)
        val hasStorage = PermissionHelper.hasManageStorage(this)
        val hasUsage = PermissionHelper.hasUsageStats(this)
        val hasBattery = PermissionHelper.hasBatteryOptimization(this)
        val hasShizuku = ShizukuHelper.isAvailable() && ShizukuHelper.isGranted()

        binding.tvAccessibilityStatus.text = if (hasAcc) "已授予" else "未授予"
        binding.tvAccessibilityStatus.setTextColor(
            if (hasAcc) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.tvOverlayStatus.text = if (hasOv) "已授予" else "未授予"
        binding.tvOverlayStatus.setTextColor(
            if (hasOv) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.tvStorageStatus.text = if (hasStorage) "已授予" else "未授予"
        binding.tvStorageStatus.setTextColor(
            if (hasStorage) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.tvUsageStatus.text = if (hasUsage) "已授予" else "未授予"
        binding.tvUsageStatus.setTextColor(
            if (hasUsage) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.tvBatteryStatus.text = if (hasBattery) "已忽略" else "未忽略"
        binding.tvBatteryStatus.setTextColor(
            if (hasBattery) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.tvShizukuStatus.text = if (hasShizuku) "已授权" else "未授权"
        binding.tvShizukuStatus.setTextColor(
            if (hasShizuku) getColor(R.color.success) else getColor(R.color.danger)
        )

        binding.btnDone.isEnabled = hasAcc && hasOv
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
