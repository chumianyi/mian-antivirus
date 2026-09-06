package com.chumian.miansecurity.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.chumian.miansecurity.R
import com.chumian.miansecurity.databinding.ItemAppBinding
import com.chumian.miansecurity.model.AppInfo
import com.chumian.miansecurity.util.FormatUtil

class AppAdapter(
    private val apps: List<AppInfo>,
    private val onAction: (AppInfo, String) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.tvAppName.text = app.appName
        holder.binding.tvPackageName.text = app.packageName
        holder.binding.tvVersion.text = "v${app.versionName}"
        holder.binding.tvSize.text = FormatUtil.formatFileSize(app.appSize)
        holder.binding.ivIcon.setImageDrawable(app.icon)

        holder.binding.tvStatus.text = when {
            app.isFrozen -> holder.binding.root.context.getString(R.string.frozen)
            app.isRunning -> holder.binding.root.context.getString(R.string.running)
            else -> holder.binding.root.context.getString(R.string.not_running)
        }

        holder.binding.ivSystem.visibility = if (app.isSystemApp) View.VISIBLE else View.GONE

        holder.binding.btnMenu.setOnClickListener { v ->
            val popup = PopupMenu(v.context, v)
            popup.menuInflater.inflate(R.menu.app_item_menu, popup.menu)
            popup.menu.findItem(R.id.action_freeze)?.title =
                if (app.isFrozen) v.context.getString(R.string.unfreeze)
                else v.context.getString(R.string.freeze)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_uninstall -> onAction(app, "uninstall")
                    R.id.action_freeze -> onAction(app, "freeze")
                    R.id.action_info -> onAction(app, "info")
                    R.id.action_permissions -> onAction(app, "permissions")
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = apps.size
}
