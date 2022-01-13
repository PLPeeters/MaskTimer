package com.plpeeters.masktimer.data.persistence

import androidx.room.Entity


@Entity(tableName = "masks", primaryKeys = ["type", "name"])
data class MaskEntity(
    val type: String,
    val name: String,
    val wornTimeMillis: Long = 0L,
    val wearingSince: Long? = null
)
