package com.chumian.miansecurity.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityNetworkMonitorBinding
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface

class NetworkMonitorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNetworkMonitorBinding
    private val apps = mutableListOf<AppInfo>()
    private lateinit var adapter: NetworkAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadNetworkInfo()
        loadApps()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.network_monitor)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NetworkAppAdapter(apps)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadNetworkInfo() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动数据"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
            else -> "未连接"
        }

        binding.tvNetworkType.text = "网络类型：$networkType"

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            val ipBuilder = StringBuilder()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        ipBuilder.append("${iface.name}: ${addr.hostAddress}\n")
                    }
                }
            }
            binding.tvIpAddress.text = if (ipBuilder.isEmpty()) "IP地址：无法获取" else "IP地址：\n$ipBuilder"
        } catch (e: Exception) {
            binding.tvIpAddress.text = "IP地址：无法获取"
        }

        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            val operator = tm.networkOperatorName
            binding.tvOperator.text = "运营商：${operator.ifEmpty { "未知" }}"
        } catch (e: Exception) {
            binding.tvOperator.text = "运营商：未知"
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val allApps = withContext(Dispatchers.IO) {
                ProcessManager.getInstalledApps(this@NetworkMonitorActivity)
            }
            apps.clear()
            apps.addAll(allApps.filter {
                it.permissions.any { perm ->
                    perm.contains("INTERNET", ignoreCase = true)
                }
            }.sortedBy { it.appName })
            adapter.notifyDataSetChanged()
            binding.progressBar.visibility = android.view.View.GONE
            binding.tvSummary.text = "发现 ${apps.size} 个使用网络的应用"
        }
    }
}
