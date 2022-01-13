package com.plpeeters.masktimer.data.persistence

import android.app.Application
import androidx.room.Room


object MaskDatabaseSingleton {
    private var instance: MaskDatabase? = null

    @Synchronized
    operator fun invoke(application: Application? = null): MaskDatabase {
        if (instance == null) {
            if (application == null) {
                throw RuntimeException("${this::class.java.name} singleton has no instance but no application was passed, cannot initialize.")
            }

            instance = Room.databaseBuilder(
                application.applicationContext,
                MaskDatabase::class.java,
                "masks"
            ).build()
        }

        return instance as MaskDatabase
    }
}
