package com.plpeeters.masktimer.data.persistence

import android.content.Context
import androidx.room.Room


object MaskDatabaseSingleton {
    private var instance: MaskDatabase? = null

    @Synchronized
    operator fun invoke(context: Context? = null): MaskDatabase {
        if (instance == null) {
            if (context == null) {
                throw RuntimeException("${this::class.java.name} singleton has no instance but no context was passed, cannot initialize.")
            }

            instance = Room.databaseBuilder(
                context.applicationContext,
                MaskDatabase::class.java,
                "masks"
            ).build()
        }

        return instance as MaskDatabase
    }
}
