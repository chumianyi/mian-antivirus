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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.chumian.miansecurity.MianApp
import com.chumian.miansecurity.R
import com.chumian.miansecurity.emergency.ProcessManager
import com.chumian.miansecurity.ui.MainActivity
import com.chumian.miansecurity.util.Prefs

class ProtectService : Service(), SensorEventListener {
    companion object {
        const val ACTION_KILL_ALL = "com.chumian.miansecurity.KILL_ALL"
        const val ACTION_START = "com.chumian.miansecurity.START_PROTECT"
        const val ACTION_STOP = "com.chumian.miansecurity.STOP_PROTECT"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ProtectService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ProtectService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private var shakeCount = 0

    private var volumeKeySequence = mutableListOf<Int>()
    private var lastVolumeKeyTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    private var lastMediaVolume = -1
    private var lastRingVolume = -1
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_KILL_ALL -> {
                executeKillAll()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            "VOLUME_KEY" -> {
                val keyCode = intent.getIntExtra("keyCode", 0)
                if (keyCode != 0) {
                    onVolumeKeyPressed(keyCode)
                }
                return START_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        registerSensors()
        startVolumeMonitoring()
        Prefs.protectEnabled = true

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val killIntent = Intent(this, ProtectService::class.java)
        killIntent.action = ACTION_KILL_ALL
        val killPendingIntent = PendingIntent.getService(
            this, 1, killIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, MianApp.CHANNEL_PROTECT)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.protect_notification_title))
            .setContentText(getString(R.string.protect_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.action_kill_all),
                killPendingIntent
            )

        return builder.build()
    }

    private fun registerSensors() {
        if (Prefs.protectMethodShake) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    private fun startVolumeMonitoring() {
        if (Prefs.volumeProtectEnabled) {
            lastMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            lastRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            handler.post(volumeMonitorRunnable)
        }
    }

    private val volumeMonitorRunnable = object : Runnable {
        override fun run() {
            checkVolumeChanges()
            handler.postDelayed(this, 500)
        }
    }

    private fun checkVolumeChanges() {
        try {
            val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val currentMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)

            if (lastMediaVolume >= 0 && currentMedia == maxMedia && lastMediaVolume != maxMedia) {
                val foreground = ProcessManager.getForegroundPackage(this)
                if (foreground.isNotEmpty() && foreground != packageName) {
                    handleVolumeViolation(foreground, "媒体音量被拉满")
                }
            }

            if (lastRingVolume >= 0 && currentRing == maxRing && lastRingVolume != maxRing) {
                val foreground = ProcessManager.getForegroundPackage(this)
                if (foreground.isNotEmpty() && foreground != packageName) {
                    handleVolumeViolation(foreground, "铃声音量被拉满")
                }
            }

            lastMediaVolume = currentMedia
            lastRingVolume = currentRing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleVolumeViolation(packageName: String, reason: String) {
        Prefs.volumeViolationCount = Prefs.volumeViolationCount + 1
        Prefs.lastVolumeViolation = "$packageName - $reason"

        ProcessManager.forceStopProcess(this, packageName)

        if (Prefs.vibrateOnTrigger) {
            vibrate()
        }

        sendVolumeViolationNotification(packageName, reason)
    }

    private fun sendVolumeViolationNotification(packageName: String, reason: String) {
        try {
            val notification = NotificationCompat.Builder(this, MianApp.CHANNEL_VOLUME)
                .setSmallIcon(R.drawable.ic_volume_off)
                .setContentTitle("音量保护")
                .setContentText("$packageName 因${reason}被禁止")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(2001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onVolumeKeyPressed(keyCode: Int) {
        if (!Prefs.protectMethodVolume) return

        val now = System.currentTimeMillis()
        if (now - lastVolumeKeyTime > 2000) {
            volumeKeySequence.clear()
        }
        lastVolumeKeyTime = now

        volumeKeySequence.add(keyCode)
        if (volumeKeySequence.size > 5) {
            volumeKeySequence.removeAt(0)
        }

        if (checkVolumeSequence()) {
            volumeKeySequence.clear()
            executeKillAll()
        }
    }

    private fun checkVolumeSequence(): Boolean {
        if (volumeKeySequence.size != 5) return false
        return volumeKeySequence[0] == KeyEvent.KEYCODE_VOLUME_UP &&
                volumeKeySequence[1] == KeyEvent.KEYCODE_VOLUME_DOWN &&
                volumeKeySequence[2] == KeyEvent.KEYCODE_VOLUME_DOWN &&
                volumeKeySequence[3] == KeyEvent.KEYCODE_VOLUME_UP &&
                volumeKeySequence[4] == KeyEvent.KEYCODE_VOLUME_UP
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!Prefs.protectMethodShake) return
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble())
        val threshold = 25.0

        if (acceleration > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 100) {
                shakeCount++
                lastShakeTime = now
                if (shakeCount >= 3) {
                    shakeCount = 0
                    executeKillAll()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun executeKillAll() {
        Thread {
            try {
                val killed = ProcessManager.killAllAndRemoveForeground(this)
                if (Prefs.vibrateOnTrigger) {
                    vibrate()
                }
                showKillResultNotification(killed.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showKillResultNotification(count: Int) {
        try {
            val notification = NotificationCompat.Builder(this, MianApp.CHANNEL_PROTECT)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("实时守护")
                .setContentText("已禁止 $count 个进程")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(1002, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(volumeMonitorRunnable)
        Prefs.protectEnabled = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
