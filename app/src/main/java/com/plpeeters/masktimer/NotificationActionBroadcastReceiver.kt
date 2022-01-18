package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.plpeeters.masktimer.data.Data
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.plpeeters.masktimer.utils.*


class NotificationActionBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)

            if (intent?.getBooleanExtra(STOP_WEARING_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        mask.stopWearing()

                        notificationManager.dismissMaskTimerExpiredNotification()
                        notificationManager.dismissMaskTimerNotification()
                        alarmManager.cancelMaskAlarm(context)

                        localBroadcastManager.sendBroadcast(Intent(ACTION_STOP_WEARING))

                        break
                    }
                }
            } else if (intent?.getBooleanExtra(REPLACE_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        mask.replace()
                        mask.startWearing()
                        notificationManager.dismissMaskTimerExpiredNotification()
                        notificationManager.createOrUpdateMaskTimerNotification(context, mask)
                        alarmManager.setAlarmForMask(context, mask)

                        Toast.makeText(it, R.string.mask_replaced, Toast.LENGTH_SHORT).show()

                        localBroadcastManager.sendBroadcast(Intent(ACTION_REPLACE))

                        break
                    }
                }
            }
        }
    }
}
