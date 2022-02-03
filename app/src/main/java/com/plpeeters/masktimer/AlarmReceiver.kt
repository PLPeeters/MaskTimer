package com.plpeeters.masktimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.plpeeters.masktimer.utils.sendMaskTimerExpiredNotification


class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager = NotificationManagerCompat.from(it)

            notificationManager.sendMaskTimerExpiredNotification(it)
        }
    }
}
