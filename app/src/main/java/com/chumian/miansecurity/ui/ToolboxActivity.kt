package com.chumian.miansecurity.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ActivityToolboxBinding

data class ToolItem(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val category: String
)

class ToolboxActivity : AppCompatActivity() {
    private lateinit var binding: ActivityToolboxBinding

    private val tools = listOf(
        ToolItem("app_manager", "应用管理", "卸载/冻结/查看权限", R.drawable.ic_app_manager, "应用"),
        ToolItem("process_manager", "进程管理", "查看/结束运行进程", R.drawable.ic_process, "系统"),
        ToolItem("permission_manager", "权限管理", "查看各应用权限", R.drawable.ic_permission, "应用"),
        ToolItem("cache_clean", "缓存清理", "清理应用/系统缓存", R.drawable.ic_cache, "系统"),
        ToolItem("traffic_monitor", "流量监控", "应用流量统计", R.drawable.ic_traffic, "网络"),
        ToolItem("battery_monitor", "电池监控", "耗电/温度/电压", R.drawable.ic_battery, "系统"),
        ToolItem("wifi_check", "WiFi检测", "WiFi安全检测", R.drawable.ic_wifi, "网络"),
        ToolItem("app_lock", "应用锁", "给应用加锁", R.drawable.ic_app_lock, "安全"),
        ToolItem("privacy_vault", "隐私保险箱", "加密存储文件", R.drawable.ic_vault, "安全"),
        ToolItem("link_checker", "链接检测", "URL安全性检测", R.drawable.ic_link, "安全"),
        ToolItem("notification_manager", "通知管理", "垃圾通知拦截", R.drawable.ic_notification, "系统"),
        ToolItem("device_info", "设备信息", "CPU/内存/存储详情", R.drawable.ic_device, "系统"),
        ToolItem("apk_extractor", "APK提取器", "提取已安装APK", R.drawable.ic_apk, "应用"),
        ToolItem("ad_block", "广告拦截", "hosts广告拦截", R.drawable.ic_adblock, "网络"),
        ToolItem("license", "开源许可证", "查看开源协议", R.drawable.ic_license, "其他"),
        ToolItem("feedback", "意见反馈", "提交问题/建议", R.drawable.ic_feedback, "其他"),
        ToolItem("sensor_test", "传感器检测", "所有传感器数据", R.drawable.ic_sensor, "系统"),
        ToolItem("screen_test", "屏幕测试", "坏点/触摸/色彩", R.drawable.ic_screen, "系统"),
        ToolItem("dns_switch", "DNS切换", "切换DNS服务器", R.drawable.ic_dns, "网络"),
        ToolItem("speed_test", "网速测试", "下载/上传/延迟", R.drawable.ic_speed, "网络")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "工具箱"

        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = ToolAdapter(tools) { tool ->
            onToolClick(tool)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter

        binding.toolCount.text = "共 ${tools.size} 个工具"
    }

    private fun onToolClick(tool: ToolItem) {
        when (tool.id) {
            "app_manager" -> startActivity(Intent(this, AppManagerActivity::class.java))
            "process_manager" -> startActivity(Intent(this, ProcessManagerActivity::class.java))
            "permission_manager" -> startActivity(Intent(this, PermissionManagerActivity::class.java))
            "cache_clean" -> startActivity(Intent(this, CacheCleanActivity::class.java))
            "traffic_monitor" -> startActivity(Intent(this, TrafficMonitorActivity::class.java))
            "battery_monitor" -> startActivity(Intent(this, BatteryMonitorActivity::class.java))
            "wifi_check" -> startActivity(Intent(this, WifiCheckActivity::class.java))
            "app_lock" -> startActivity(Intent(this, AppLockActivity::class.java))
            "privacy_vault" -> startActivity(Intent(this, PrivacyVaultActivity::class.java))
            "link_checker" -> startActivity(Intent(this, LinkCheckerActivity::class.java))
            "notification_manager" -> startActivity(Intent(this, NotificationManagerActivity::class.java))
            "device_info" -> startActivity(Intent(this, DeviceInfoActivity::class.java))
            "apk_extractor" -> startActivity(Intent(this, ApkExtractorActivity::class.java))
            "ad_block" -> startActivity(Intent(this, AdBlockActivity::class.java))
            "license" -> startActivity(Intent(this, LicenseActivity::class.java))
            "feedback" -> startActivity(Intent(this, FeedbackActivity::class.java))
            "sensor_test" -> startActivity(Intent(this, SensorTestActivity::class.java))
            "screen_test" -> startActivity(Intent(this, ScreenTestActivity::class.java))
            "dns_switch" -> startActivity(Intent(this, DnsSwitchActivity::class.java))
            "speed_test" -> startActivity(Intent(this, SpeedTestActivity::class.java))
        }
    }
}

class ToolAdapter(
    private val items: List<ToolItem>,
    private val onClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.toolName)
        val desc: TextView = view.findViewById(R.id.toolDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.desc.text = item.description
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
