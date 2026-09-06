package com.chumian.miansecurity.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityBatteryBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatteryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBatteryBinding
    private val apps = mutableListOf<AppInfo>()
    private lateinit var adapter: BatteryAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadBatteryInfo()
        loadApps()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.battery_optimize)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = BatteryAppAdapter(apps) { app ->
            optimizeApp(app)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadBatteryInfo() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val batteryStatus = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (scale > 0) level * 100 / scale else 0
        val temperature = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10) ?: 0
        val voltage = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0)?.div(1000) ?: 0

        binding.tvBatteryLevel.text = "电量：$batteryPct%"
        binding.tvBatteryTemp.text = "温度：${temperature}°C"
        binding.tvBatteryVoltage.text = "电压：${voltage}V"
        binding.tvBatteryOptimization.text = if (pm.isIgnoringBatteryOptimizations(packageName)) {
            "电池优化：已忽略（推荐）"
        } else {
            "电池优化：未忽略"
        }

        binding.btnIgnoreOptimization.setOnClickListener {
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val allApps = withContext(Dispatchers.IO) {
                ProcessManager.getInstalledApps(this@BatteryActivity)
            }
            apps.clear()
            apps.addAll(allApps.filter { it.isRunning && !it.isSystemApp }.sortedByDescending { it.appSize })
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvSummary.text = "发现 ${apps.size} 个正在运行的应用"
        }
    }

    private fun optimizeApp(app: AppInfo) {
        ProcessManager.forceStopProcess(this, app.packageName)
        loadApps()
    }
}
