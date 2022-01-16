package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import com.plpeeters.masktimer.Constants.REPLACE_EXTRA
import com.plpeeters.masktimer.Constants.STOP_WEARING_EXTRA
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.utils.dismissMaskTimerExpiredNotification
import com.plpeeters.masktimer.utils.dismissMaskTimerNotification
import com.plpeeters.masktimer.utils.createOrUpdateMaskTimerNotification
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.plpeeters.masktimer.Constants.ACTION_REPLACE
import com.plpeeters.masktimer.Constants.ACTION_STOP_WEARING


class NotificationActionBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val alarmPendingIntent = Intent(context, AlarmReceiver::class.java).let { alarmIntent ->
                PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
            }

            if (intent?.getBooleanExtra(STOP_WEARING_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.wearingSince != null) {
                        mask.stopWearing()

                        notificationManager.dismissMaskTimerExpiredNotification()
                        notificationManager.dismissMaskTimerNotification()
                        alarmManager.cancel(alarmPendingIntent)

                        localBroadcastManager.sendBroadcast(Intent(ACTION_STOP_WEARING))

                        break
                    }
                }
            } else if (intent?.getBooleanExtra(REPLACE_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.wearingSince != null) {
                        mask.replace()
                        mask.startWearing()
                        notificationManager.dismissMaskTimerExpiredNotification()
                        notificationManager.createOrUpdateMaskTimerNotification(it, mask)

                        alarmManager.cancel(alarmPendingIntent)
                        alarmManager.set(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + (mask.getExpirationTimestamp() - Date().time),
                            alarmPendingIntent
                        )

                        Toast.makeText(it, R.string.mask_replaced, Toast.LENGTH_SHORT).show()

                        localBroadcastManager.sendBroadcast(Intent(ACTION_REPLACE))

                        break
                    }
                }
            }
        }
    }
}
