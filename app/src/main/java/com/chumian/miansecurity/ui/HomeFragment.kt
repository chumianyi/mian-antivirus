package com.chumian.miansecurity.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chumian.miansecurity.R
import com.chumian.miansecurity.core.Prefs
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStatus()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // Shizuku状态
        val shizukuReady = ShizukuHelper.isReady(requireContext())
        binding.tvShizukuStatus.text = if (shizukuReady) "Shizuku已授权" else "Shizuku未授权"
        binding.tvShizukuStatus.setTextColor(
            if (shizukuReady) resources.getColor(R.color.success, null)
            else resources.getColor(R.color.danger, null)
        )

        // 安全评分
        val dangerCount = Prefs.lastScanDangerCount
        val score = when {
            dangerCount == 0 -> 95
            dangerCount <= 2 -> 80 - dangerCount * 5
            dangerCount <= 5 -> 60 - dangerCount * 3
            else -> 40
        }
        binding.tvSecurityScore.text = score.toString()
        binding.tvScoreDesc.text = when {
            score >= 90 -> "设备很安全"
            score >= 70 -> "基本安全，建议检查"
            score >= 50 -> "存在风险，请处理"
            else -> "危险！立即处理"
        }

        // 上次扫描
        if (Prefs.lastScanTime > 0) {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            binding.tvLastScan.text = "上次扫描：${sdf.format(Date(Prefs.lastScanTime))}，发现${Prefs.lastScanDangerCount}个风险"
        } else {
            binding.tvLastScan.text = "尚未扫描"
        }

        // 实时防护状态
        binding.tvProtectStatus.text = if (Prefs.protectEnabled) "实时防护：运行中" else "实时防护：已关闭"
    }

    private fun setupClicks() {
        binding.cardEmergency.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyActivity::class.java))
        }
        binding.cardProtect.setOnClickListener {
            startActivity(Intent(requireContext(), ProtectActivity::class.java))
        }
        binding.cardAppManager.setOnClickListener {
            startActivity(Intent(requireContext(), AppManagerActivity::class.java))
        }
        binding.cardProcessManager.setOnClickListener {
            startActivity(Intent(requireContext(), ProcessManagerActivity::class.java))
        }
        binding.cardPermissionManager.setOnClickListener {
            startActivity(Intent(requireContext(), PermissionManagerActivity::class.java))
        }
        binding.cardCacheClean.setOnClickListener {
            startActivity(Intent(requireContext(), CacheCleanActivity::class.java))
        }
        binding.btnQuickScan.setOnClickListener {
            (activity as? MainActivity)?.switchToScanTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
