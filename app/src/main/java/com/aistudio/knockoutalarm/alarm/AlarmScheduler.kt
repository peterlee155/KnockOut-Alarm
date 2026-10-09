package com.aistudio.knockoutalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.MainActivity
import com.aistudio.knockoutalarm.model.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Checks if exact alarm permission is granted on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules an alarm using Android's highest-priority setAlarmClock() mechanism.
     * This ensures the alarm fires even in Doze mode, device lock, or idle standby.
     */
    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return
        }

        val triggerAtMillis = calculateNextTriggerTimeMillis(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_PUNCH_TARGET, alarm.punchTarget)
            putExtra(EXTRA_DIFFICULTY, alarm.difficulty.name)
            putExtra(EXTRA_SOUND_ID, alarm.soundId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show intent launched when user taps the alarm clock icon in system tray/lockscreen
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: SecurityException) {
            // If exact alarm permission missing on Android 12+, fallback to setAndAllowWhileIdle
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Cancels scheduled alarm in AlarmManager.
     */
    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules a test alarm to fire after [delaySeconds] (e.g. 5s or 10s)
     * so the user can background the app, lock the screen, and verify wake-up & ringing.
     */
    fun scheduleTestAlarm(delaySeconds: Int, punchTarget: Int = 30) {
        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, TEST_ALARM_ID)
            putExtra(EXTRA_PUNCH_TARGET, punchTarget)
            putExtra(EXTRA_DIFFICULTY, "NORMAL")
            putExtra(EXTRA_SOUND_ID, "SIREN")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_ALARM_ID.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            TEST_ALARM_ID.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelTestAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_ALARM_ID.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Computes the epoch millis for the next upcoming alarm occurrence based on repeat days.
     */
    fun calculateNextTriggerTimeMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time already passed today, advance by at least 1 day
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // If repeat days specified, find the next matching day of week
        if (alarm.repeatDays.isNotEmpty()) {
            for (i in 0 until 7) {
                val calDay = target.get(Calendar.DAY_OF_WEEK)
                // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) to ISO (Mon=1..Sun=7)
                val isoDay = when (calDay) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                if (alarm.repeatDays.contains(isoDay)) {
                    break
                }
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.aistudio.knockoutalarm.ACTION_ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PUNCH_TARGET = "extra_punch_target"
        const val EXTRA_DIFFICULTY = "extra_difficulty"
        const val EXTRA_SOUND_ID = "extra_sound_id"
        const val TEST_ALARM_ID = 999999L
    }
}
