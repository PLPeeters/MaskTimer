package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.utils.createOrUpdateMaskTimerNotification
import java.util.*


class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val alarmPendingIntent = Intent(context.applicationContext, AlarmReceiver::class.java).let { alarmIntent ->
                PendingIntent.getBroadcast(context.applicationContext, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
            }

            Thread {
                Data.load(context)

                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        notificationManager.createOrUpdateMaskTimerNotification(context, mask)

                        alarmManager.set(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + mask.getRemainingLifespanMillis(context),
                            alarmPendingIntent
                        )

                        break
                    }
                }
            }.start()
        }
    }
}
