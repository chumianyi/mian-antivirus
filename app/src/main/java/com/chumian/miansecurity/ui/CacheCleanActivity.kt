package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityCacheCleanBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CacheCleanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCacheCleanBinding
    private val apps = mutableListOf<AppInfo>()
    private val selected = mutableSetOf<String>()
    private lateinit var adapter: CacheAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCacheCleanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        scanCache()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.cache_clean)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = CacheAppAdapter(apps, selected) { pkg, isChecked ->
            if (isChecked) selected.add(pkg) else selected.remove(pkg)
            updateSummary()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnSelectAll.setOnClickListener {
            if (selected.size == apps.size) {
                selected.clear()
            } else {
                selected.addAll(apps.map { it.packageName })
            }
            adapter.notifyDataSetChanged()
            updateSummary()
        }
        binding.btnClean.setOnClickListener { cleanSelected() }
    }

    private fun scanCache() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvStatus.text = "正在扫描缓存..."
        lifecycleScope.launch {
            val allApps = withContext(Dispatchers.IO) {
                ProcessManager.getInstalledApps(this@CacheCleanActivity)
            }
            apps.clear()
            apps.addAll(allApps.filter { it.cacheSize > 0 }.sortedByDescending { it.cacheSize })
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvStatus.text = "扫描完成"
            updateSummary()
        }
    }

    private fun updateSummary() {
        val totalCache = apps.sumOf { it.cacheSize }
        val selectedCache = apps.filter { selected.contains(it.packageName) }.sumOf { it.cacheSize }
        binding.tvTotalCache.text = "总缓存：${FormatUtil.formatFileSize(totalCache)}"
        binding.tvSelectedCache.text = "已选：${FormatUtil.formatFileSize(selectedCache)} (${selected.size}项)"
        binding.btnClean.isEnabled = selected.isNotEmpty()
    }

    private fun cleanSelected() {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage("确定清理选中的 ${selected.size} 个应用缓存？")
            .setPositiveButton(R.string.confirm) { _, _ ->
                var cleaned = 0
                for (pkg in selected) {
                    if (ProcessManager.clearAppCache(this, pkg)) cleaned++
                }
                Toast.makeText(this, "已清理 $cleaned 个应用缓存", Toast.LENGTH_SHORT).show()
                selected.clear()
                scanCache()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
