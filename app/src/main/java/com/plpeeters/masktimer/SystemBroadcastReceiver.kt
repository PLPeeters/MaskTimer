package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
        } else if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Thread {
                    Data.load(context)

                    for (mask in Data.MASKS) {
                        if (mask.isBeingWorn) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setAlarmForMask(context, mask)
                            } else {
                                Toast.makeText(context, R.string.will_not_be_able_to_notify, Toast.LENGTH_LONG).show()
                            }

                            break
                        }
                    }
                }.start()
            }
        }
    }
}
