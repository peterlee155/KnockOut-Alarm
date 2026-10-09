package com.aistudio.knockoutalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        val punchTarget = intent.getIntExtra(AlarmScheduler.EXTRA_PUNCH_TARGET, 30)
        val difficulty = intent.getStringExtra(AlarmScheduler.EXTRA_DIFFICULTY) ?: "NORMAL"
        val soundId = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND_ID) ?: "SIREN"

        // Acquire temporary wake lock to ensure CPU remains awake for the user to wake up
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "KnockOutAlarm:AlarmWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max safety limit

        // Start foreground ringing service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_PUNCH_TARGET, punchTarget)
            putExtra(AlarmScheduler.EXTRA_DIFFICULTY, difficulty)
            putExtra(AlarmScheduler.EXTRA_SOUND_ID, soundId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Launch full-screen fight arena activity
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("route", "arena")
            putExtra("alarm_id", alarmId)
            putExtra("punch_target", punchTarget)
            putExtra("difficulty", difficulty)
        }
        context.startActivity(activityIntent)
    }
}
