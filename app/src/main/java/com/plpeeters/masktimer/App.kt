package com.plpeeters.masktimer

import android.app.Application
import android.app.NotificationManager
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.utils.createNotificationChannels


class App : Application() {
    private fun loadData() {
        val dataLoadThread = Thread {
            Data.load(this)
        }

        dataLoadThread.start()
        dataLoadThread.join()
    }

    override fun onCreate() {
        super.onCreate()

        // Create the notification channels
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(this)

        // Load masks from the database
        loadData()
    }
}
