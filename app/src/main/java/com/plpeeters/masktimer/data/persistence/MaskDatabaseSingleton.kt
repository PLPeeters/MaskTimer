package com.plpeeters.masktimer.data.persistence

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration


object MaskDatabaseSingleton {
    private var instance: MaskDatabase? = null
    private val MIGRATION_1_2 = Migration(1, 2) {
        it.execSQL("ALTER TABLE masks ADD COLUMN isPrevious INTEGER NOT NULL DEFAULT 0")
    }

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
            ).addMigrations(MIGRATION_1_2).build()
        }

        return instance as MaskDatabase
    }
}
