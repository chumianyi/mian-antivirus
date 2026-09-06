package com.chumian.miansecurity.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chumian.miansecurity.service.ProtectService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.autoStartProtect && Prefs.protectEnabled) {
                ProtectService.start(context)
            }
        }
    }
}
