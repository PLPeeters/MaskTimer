package com.plpeeters.masktimer

import android.app.Application
import android.app.NotificationManager
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import com.plpeeters.masktimer.utils.createNotificationChannels


class App : Application() {
    private fun fetchMasksFromDatabase() {
        Thread {
            Data.MASKS.addAll(MaskDatabaseSingleton(this).maskDatabaseDao()
                .getAll().map { maskEntity ->
                    Mask.fromMaskEntity(maskEntity)
                })
        }.start()
    }

    override fun onCreate() {
        super.onCreate()

        // Create the notification channels
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(this)

        // Fetch masks from the database
        fetchMasksFromDatabase()
    }
}
