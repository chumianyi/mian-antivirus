package com.chumian.miansecurity.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chumian.miansecurity.R
import com.chumian.miansecurity.core.Prefs
import com.chumian.miansecurity.core.UpdateChecker
import com.chumian.miansecurity.databinding.FragmentAboutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvVersion.text = "版本 ${UpdateChecker.getCurrentVersion(requireContext())} (${UpdateChecker.getCurrentVersionCode(requireContext())})"
        binding.tvAppName.text = getString(R.string.app_name)

        binding.itemCheckUpdate.setOnClickListener { checkUpdate() }
        binding.itemTheme.setOnClickListener { showThemeSelector() }
        binding.itemGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chumianyi/mian-antivirus")))
        }
        binding.itemFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:feedback@chumian.com")
                putExtra(Intent.EXTRA_SUBJECT, "眠. 杀毒软件反馈")
            }
            startActivity(intent)
        }
        binding.itemOpenSource.setOnClickListener { showOpenSourceLicenses() }
    }

    private fun checkUpdate() {
        binding.tvUpdateStatus.text = "正在检查更新..."
        scope.launch {
            val info = withContext(Dispatchers.IO) {
                UpdateChecker.checkUpdate(requireContext())
            }
            if (info.hasUpdate) {
                binding.tvUpdateStatus.text = "发现新版本 ${info.latestVersion}"
                showUpdateDialog(info)
            } else {
                binding.tvUpdateStatus.text = "已是最新版本"
                Toast.makeText(requireContext(), "已是最新版本", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(info: com.chumian.miansecurity.model.UpdateInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("发现新版本")
            .setMessage("最新版本：${info.latestVersion}\n\n更新说明：\n${info.changelog}")
            .setPositiveButton("立即更新") { _, _ ->
                if (info.downloadUrl.isNotEmpty()) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                }
            }
            .setNegativeButton("暂不更新", null)
            .show()
    }

    private fun showThemeSelector() {
        val themes = arrayOf("跟随系统", "深色", "浅色")
        val current = when (Prefs.theme) {
            "dark" -> 1
            "light" -> 2
            else -> 0
        }
        AlertDialog.Builder(requireContext())
            .setTitle("选择主题")
            .setSingleChoiceItems(themes, current) { dialog, which ->
                Prefs.theme = when (which) {
                    1 -> "dark"
                    2 -> "light"
                    else -> "system"
                }
                dialog.dismiss()
                requireActivity().recreate()
            }
            .show()
    }

    private fun showOpenSourceLicenses() {
        AlertDialog.Builder(requireContext())
            .setTitle("开源协议")
            .setMessage(
                "本应用使用以下开源项目：\n\n" +
                "• QMUI-Android (MIT)\n" +
                "• Shizuku (MIT)\n" +
                "• Kotlin Coroutines (Apache 2.0)\n" +
                "• Material Components (Apache 2.0)\n\n" +
                "感谢所有开源贡献者。"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
