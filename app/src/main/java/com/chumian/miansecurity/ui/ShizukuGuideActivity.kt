package com.chumian.miansecurity.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityShizukuGuideBinding
import rikka.shizuku.Shizuku

class ShizukuGuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShizukuGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShizukuGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateStatus()

        binding.btnInstallShizuku.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                startActivity(intent)
            }
        }

        binding.btnStartShizuku.setOnClickListener {
            // 尝试启动Shizuku
            try {
                val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                    Toast.makeText(this, "请在Shizuku中启动服务", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "请先安装Shizuku", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "启动Shizuku失败", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAuthorize.setOnClickListener {
            if (!ShizukuHelper.isInstalled(this)) {
                Toast.makeText(this, "请先安装Shizuku", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!ShizukuHelper.isRunning()) {
                Toast.makeText(this, "Shizuku未运行，请先启动", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 请求授权
            ShizukuHelper.requestPermission()
            Toast.makeText(this, "正在请求Shizuku授权...", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckAgain.setOnClickListener {
            updateStatus()
            if (ShizukuHelper.isReady(this)) {
                enterMain()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        if (ShizukuHelper.isReady(this)) {
            enterMain()
        }
    }

    private fun updateStatus() {
        val installed = ShizukuHelper.isInstalled(this)
        val running = installed && ShizukuHelper.isRunning()
        val granted = running && ShizukuHelper.isGranted()

        binding.tvStatusInstall.text = if (installed) "✓ 已安装" else "✗ 未安装"
        binding.tvStatusRunning.text = if (running) "✓ 已运行" else "✗ 未运行"
        binding.tvStatusGranted.text = if (granted) "✓ 已授权" else "✗ 未授权"

        binding.btnInstallShizuku.isEnabled = !installed
        binding.btnStartShizuku.isEnabled = installed && !running
        binding.btnAuthorize.isEnabled = running && !granted
        binding.btnCheckAgain.isEnabled = true
    }

    private fun enterMain() {
        Toast.makeText(this, "Shizuku已授权，进入主界面", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
