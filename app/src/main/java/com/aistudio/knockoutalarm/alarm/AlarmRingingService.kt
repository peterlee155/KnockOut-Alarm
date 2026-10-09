package com.aistudio.knockoutalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.aistudio.knockoutalarm.audio.AlarmAudioEngine

class AlarmRingingService : Service() {

    private var audioEngine: AlarmAudioEngine? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioEngine = AlarmAudioEngine(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarmAndService()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L) ?: 0L
        val punchTarget = intent?.getIntExtra(AlarmScheduler.EXTRA_PUNCH_TARGET, 30) ?: 30

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("route", "arena")
            putExtra("alarm_id", alarmId)
            putExtra("punch_target", punchTarget)
        }
        val pendingFullScreen = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("KnockOut Alarm Ringing!")
            .setContentText("Complete $punchTarget boxing punches to silence!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingFullScreen, true)
            .setContentIntent(pendingFullScreen)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        audioEngine?.startAlarm()
        return START_STICKY
    }

    fun onPunchLanded() {
        audioEngine?.onPunchLanded()
    }

    fun enqueueAnnouncerCue(cue: String) {
        audioEngine?.enqueueAnnouncerCue(cue)
    }

    fun playKnockoutBell() {
        audioEngine?.playKnockoutBell()
    }

    fun stopAlarmAndService() {
        audioEngine?.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        audioEngine?.release()
        audioEngine = null
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KnockOut Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm alerts requiring punch challenge"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "knockout_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.aistudio.knockoutalarm.ACTION_STOP_SERVICE"

        @Volatile
        var instance: AlarmRingingService? = null
            private set
    }
}
