package com.plpeeters.masktimer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.plpeeters.masktimer.utils.sendMaskTimerExpiredNotification


class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager = it.getSystemService(NotificationManager::class.java)

            notificationManager.sendMaskTimerExpiredNotification(it)
        }
    }
}
