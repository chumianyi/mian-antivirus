package com.chumian.miansecurity.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.databinding.ItemKilledProcessBinding

class KilledProcessAdapter(
    private val processes: List<String>
) : RecyclerView.Adapter<KilledProcessAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemKilledProcessBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKilledProcessBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pkg = processes[position]
        holder.binding.tvProcessName.text = pkg
        holder.binding.tvPid.text = "PID: -"
        try {
            val pm = holder.binding.root.context.packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            holder.binding.tvProcessName.text = "$appName ($pkg)"
            holder.binding.tvStatus.text = "已禁止"
        } catch (e: PackageManager.NameNotFoundException) {
            holder.binding.tvStatus.text = "已禁止"
        }
    }

    override fun getItemCount() = processes.size
}
