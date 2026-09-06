package com.chumian.miansecurity.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityCacheCleanBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CacheCleanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCacheCleanBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCacheCleanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "缓存清理"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        calculateCache()
        binding.btnClean.setOnClickListener { cleanCache() }
    }

    private fun calculateCache() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val cacheSize = withContext(Dispatchers.IO) {
                var total = 0L
                try {
                    // 计算应用缓存
                    val externalCache = getExternalFilesDir(null)?.parentFile?.listFiles()
                    externalCache?.forEach { dir ->
                        if (dir.name == "cache") {
                            total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        }
                    }
                    // 系统缓存
                    val cacheDir = cacheDir
                    total += cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                } catch (e: Exception) {}
                total
            }
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvCacheSize.text = formatSize(cacheSize)
            binding.tvCacheDesc.text = "可清理缓存空间"
        }
    }

    private fun cleanCache() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvCacheDesc.text = "正在清理..."
        scope.launch {
            val cleaned = withContext(Dispatchers.IO) {
                var freed = 0L
                try {
                    // 清理应用缓存
                    cacheDir.walkTopDown().filter { it.isFile }.forEach {
                        freed += it.length()
                        it.delete()
                    }
                    // 用Shizuku清理系统缓存
                    if (ShizukuHelper.isGranted()) {
                        ShizukuHelper.clearCache(packageName)
                    }
                } catch (e: Exception) {}
                freed
            }
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvCacheSize.text = formatSize(0)
            binding.tvCacheDesc.text = "清理完成，释放 ${formatSize(cleaned)}"
            Toast.makeText(this@CacheCleanActivity, "缓存清理完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = minOf(digitGroups, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format("%.2f %s", value, units[index])
    }
}
