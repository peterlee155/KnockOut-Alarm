package com.aistudio.knockoutalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.knockoutalarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val repository = AlarmRepository(context)
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val enabledAlarms = repository.getEnabledAlarms()
                for (alarm in enabledAlarms) {
                    scheduler.scheduleAlarm(alarm)
                }
            }
        }
    }
}
