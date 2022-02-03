package com.plpeeters.masktimer

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.data.Mask
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.plpeeters.masktimer.utils.*


class NotificationActionBroadcastReceiver: BroadcastReceiver() {
    private fun getCurrentAndPreviousMask(considerPausedCurrent: Boolean = false): List<Mask?> {
        var currentMask: Mask? = null
        var previousMask: Mask? = null

        for (mask in Data.MASKS) {
            if (mask.isBeingWorn || (considerPausedCurrent && mask.isPaused)) {
                currentMask = mask

                if (previousMask != null) {
                    break
                }
            } else if (mask.isPrevious) {
                previousMask = mask

                if (currentMask != null) {
                    break
                }
            }
        }

        return listOf(currentMask, previousMask)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val notificationManager = NotificationManagerCompat.from(context)
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            val alarmManager = context.getSystemService(AlarmManager::class.java)

            if (intent?.getBooleanExtra(STOP_WEARING_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        mask.stopWearing()



                        break
                    } else if (mask.isPaused) {
                        mask.isPaused = false

                        break
                    }
                }

                notificationManager.dismissMaskTimerExpiredNotification()
                notificationManager.dismissMaskTimerNotification()
                alarmManager.cancelMaskAlarm(context)

                localBroadcastManager.sendBroadcast(Intent(ACTION_STOP_WEARING))
            } else if (intent?.getBooleanExtra(PAUSE_OR_RESUME_WEARING_EXTRA, false) == true) {
                val (currentMask, previousMask) = getCurrentAndPreviousMask(true)

                if (currentMask != null) {
                    if (currentMask.isBeingWorn) {
                        currentMask.pauseWearing()
                        notificationManager.createOrUpdateMaskTimerNotification(context, currentMask, previousMask)
                        alarmManager.cancelMaskAlarm(context)
                    } else if (currentMask.isPaused) {
                        currentMask.startWearing()
                        notificationManager.createOrUpdateMaskTimerNotification(context, currentMask, previousMask)
                        alarmManager.setAlarmForMask(context, currentMask)
                    }
                }
            } else if (intent?.getBooleanExtra(REPLACE_EXTRA, false) == true) {
                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn || mask.isPaused) {
                        mask.replace()
                        notificationManager.dismissMaskTimerExpiredNotification()
                        notificationManager.createOrUpdateMaskTimerNotification(context, mask)
                        alarmManager.setAlarmForMask(context, mask)

                        Toast.makeText(context, R.string.mask_replaced, Toast.LENGTH_SHORT).show()

                        localBroadcastManager.sendBroadcast(Intent(ACTION_REPLACE))

                        break
                    }
                }
            } else if (intent?.getBooleanExtra(SWAP_MASK_EXTRA, false) == true) {
                val (currentMask, previousMask) = getCurrentAndPreviousMask()

                currentMask?.let {
                    currentMask.stopWearing()
                    currentMask.isPrevious = true

                    notificationManager.dismissMaskTimerExpiredNotification()
                    alarmManager.cancelMaskAlarm(context)
                }

                previousMask?.let {
                    previousMask.startWearing()
                    previousMask.isPrevious = false

                    notificationManager.dismissMaskTimerExpiredNotification()
                    notificationManager.createOrUpdateMaskTimerNotification(context, previousMask, currentMask)
                    alarmManager.setAlarmForMask(context, previousMask)
                }
            }
        }
    }
}
