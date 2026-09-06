package com.chumian.miansecurity.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.core.ProcessManager
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityEmergencyBinding
import com.chumian.miansecurity.ui.adapter.ProcessAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var killedProcesses: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "急救箱"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnStartEmergency.setOnClickListener { startEmergency() }
    }

    private fun startEmergency() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在强扫系统并清除进程..."
        binding.btnStartEmergency.isEnabled = false

        scope.launch {
            killedProcesses = withContext(Dispatchers.IO) {
                ProcessManager.killAllProcesses(this@EmergencyActivity)
            }
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.btnStartEmergency.isEnabled = true
                binding.tvStatus.text = "急救完成！已禁止 ${killedProcesses.size} 个进程"
                showKilledProcesses()
                Toast.makeText(this@EmergencyActivity, "急救箱执行完毕", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showKilledProcesses() {
        binding.killedListLayout.visibility = View.VISIBLE
        binding.tvKilledCount.text = "已禁止的进程（${killedProcesses.size}）："
        val adapter = ProcessAdapter(
            processes = killedProcesses.map { pkg ->
                com.chumian.miansecurity.model.ProcessInfo(
                    pid = 0,
                    processName = pkg,
                    packageName = pkg,
                    memorySize = 0,
                    isSystem = false
                )
            },
            onKill = {}
        )
        // 隐藏kill按钮，用简单列表
        binding.recyclerViewKilled.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewKilled.adapter = adapter
    }
}
