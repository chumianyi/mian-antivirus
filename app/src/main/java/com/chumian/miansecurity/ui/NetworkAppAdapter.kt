package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemNetworkAppBinding
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil

class NetworkAppAdapter(
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<NetworkAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNetworkAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNetworkAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName
        holder.binding.ivIcon.setImageDrawable(app.icon)
        holder.binding.tvStatus.text = if (app.isRunning) "运行中" else "未运行"
    }

    override fun getItemCount() = apps.size
}
