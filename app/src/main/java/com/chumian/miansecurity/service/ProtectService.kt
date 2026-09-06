package com.chumian.miansecurity.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.chumian.miansecurity.MianApp
import com.chumian.miansecurity.R
import com.chumian.miansecurity.core.Prefs
import com.chumian.miansecurity.core.ProcessManager
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProtectService : Service(), SensorEventListener {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var sensorManager: SensorManager? = null
    private var audioManager: AudioManager? = null
    private var lastVolume = 0
    private var volumeKeySequence = mutableListOf<Int>()
    private var lastShakeTime = 0L
    private var shakeCount = 0

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        lastVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_KILL_ALL -> killAllProcesses()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val killIntent = Intent(this, ProtectService::class.java).apply { action = ACTION_KILL_ALL }
        val killPendingIntent = PendingIntent.getService(this, 1, killIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, MianApp.CHANNEL_PROTECT)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("眠. 实时守护")
            .setContentText("守护运行中，点击禁止所有进程")
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "禁止所有进程", killPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun killAllProcesses() {
        scope.launch {
            val killed = ProcessManager.killAllProcesses(this@ProtectService)
            // 发送通知
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val notif = NotificationCompat.Builder(this@ProtectService, MianApp.CHANNEL_EMERGENCY)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("急救完成")
                .setContentText("已禁止 ${killed.size} 个进程")
                .setAutoCancel(true)
                .build()
            nm.notify(2002, notif)
        }
    }

    // 音量键序列检测：上-下下-上上
    fun onVolumeKey(keyCode: Int) {
        if (Prefs.protectMethod != "volume") return
        volumeKeySequence.add(keyCode)
        if (volumeKeySequence.size > 5) volumeKeySequence.removeAt(0)
        // 检测序列：上(24)-下(25)-下(25)-上(24)-上(24)
        val target = listOf(24, 25, 25, 24, 24)
        if (volumeKeySequence.size == 5 && volumeKeySequence == target) {
            killAllProcesses()
            volumeKeySequence.clear()
        }
    }

    // 摇晃检测
    override fun onSensorChanged(event: SensorEvent?) {
        if (Prefs.protectMethod != "shake") return
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = Math.sqrt((x*x + y*y + z*z).toDouble())
            if (acceleration > 25.0) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 100) {
                    shakeCount++
                    lastShakeTime = now
                    if (shakeCount >= 3) {
                        killAllProcesses()
                        shakeCount = 0
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // 音量保护
    fun checkVolumeProtection() {
        if (!Prefs.volumeProtectEnabled) return
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        // 如果音量突然拉满
        if (currentVolume == maxVolume && lastVolume < maxVolume - 2) {
            val foreground = ProcessManager.getForegroundPackage(this)
            if (foreground.isNotEmpty() && foreground != packageName) {
                ShizukuHelper.killProcess(foreground)
                // 发送通知
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val notif = NotificationCompat.Builder(this, MianApp.CHANNEL_VOLUME)
                    .setSmallIcon(R.drawable.ic_volume_off)
                    .setContentTitle("音量保护")
                    .setContentText("$foreground 因强制拉满音量被禁止")
                    .setAutoCancel(true)
                    .build()
                nm.notify(2001, notif)
            }
        }
        lastVolume = currentVolume
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_KILL_ALL = "com.chumian.miansecurity.KILL_ALL"
        const val ACTION_STOP = "com.chumian.miansecurity.STOP"

        fun start(context: Context) {
            val intent = Intent(context, ProtectService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProtectService::class.java))
        }
    }
}
