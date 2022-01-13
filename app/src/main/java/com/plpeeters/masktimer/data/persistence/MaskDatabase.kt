package com.plpeeters.masktimer.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [MaskEntity::class], version = 1, exportSchema = true)
abstract class MaskDatabase: RoomDatabase() {
    abstract fun maskDatabaseDao(): MaskDao
}
