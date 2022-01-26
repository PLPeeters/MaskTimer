package com.plpeeters.masktimer

import android.app.Application
import android.app.NotificationManager
import androidx.preference.PreferenceManager
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
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

        // Set the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true)

        // Create the notification channels
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(this)

        // Initialise the database and load masks
        MaskDatabaseSingleton(applicationContext)
        loadData()
    }
}
