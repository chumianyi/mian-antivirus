package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemBatteryAppBinding
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil

class BatteryAppAdapter(
    private val apps: List<AppInfo>,
    private val onOptimize: (AppInfo) -> Unit
) : RecyclerView.Adapter<BatteryAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBatteryAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBatteryAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvUsage.text = "${app.packageName} | ${FormatUtil.formatFileSize(app.appSize)}"
        try {
            holder.binding.ivIcon.setImageDrawable(app.icon)
        } catch (e: Exception) {
            holder.binding.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        holder.binding.root.setOnClickListener { onOptimize(app) }
    }

    override fun getItemCount() = apps.size
}
