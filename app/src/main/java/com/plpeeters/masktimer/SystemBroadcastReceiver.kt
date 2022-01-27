package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.utils.createOrUpdateMaskTimerNotification
import com.plpeeters.masktimer.utils.setAlarmForMask


class SystemBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val alarmManager = context.getSystemService(AlarmManager::class.java)

            Thread {
                Data.load(context)

                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        notificationManager.createOrUpdateMaskTimerNotification(context, mask)
                        alarmManager.setAlarmForMask(context, mask)

                        break
                    }
                }
            }.start()
        }
    }
}
