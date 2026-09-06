package com.chumian.miansecurity.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MianAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MianAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 监听窗口变化，用于音量保护和进程检测
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    ProtectService::class.java.let {
                        // 转发音量键事件到守护服务
                        sendVolumeKey(event.keyCode)
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun sendVolumeKey(keyCode: Int) {
        try {
            val intent = android.content.Intent(this, ProtectService::class.java)
            intent.action = "VOLUME_KEY"
            intent.putExtra("keyCode", keyCode)
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performGlobalActionSafe(action: Int): Boolean {
        return try {
            super.performGlobalAction(action)
        } catch (e: Exception) {
            false
        }
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
