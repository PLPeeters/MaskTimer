package com.plpeeters.masktimer.data

import android.content.Context
import com.plpeeters.masktimer.Constants.MAX_WEARING_TIME_MILLIS
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import com.plpeeters.masktimer.data.persistence.MaskEntity
import com.plpeeters.masktimer.utils.normalize
import java.util.*


class Mask(
    val type: String,
    val name: String
) {
    companion object {
        fun fromMaskEntity(maskEntity: MaskEntity): Mask {
            return Mask(maskEntity.type, maskEntity.name).apply {
                wearingSince = maskEntity.wearingSince
                wornTimeMillis = maskEntity.wornTimeMillis
            }
        }

        private val database = MaskDatabaseSingleton().maskDatabaseDao()
    }

    var wornTimeMillis: Long = 0L
    var wearingSince: Long? = null

    fun toMaskEntity(): MaskEntity {
        return MaskEntity(type, name)
    }

    fun startWearing() {
        val now = Date().time

        wearingSince = now

        Thread {
            database.updateWearingSince(type, name, now)
        }.start()
    }

    fun stopWearing() {
        wornTimeMillis += Date().time - wearingSince!!
        wearingSince = null

        val wornTime = wornTimeMillis

        Thread {
            database.updateWornTime(type, name, wornTime)
        }.start()
    }

    fun replace() {
        wornTimeMillis = 0L
        wearingSince = null

        Thread {
            database.replace(type, name)
        }.start()
    }

    fun getExpirationTimestamp(): Long {
        val now = Date().time

        val wearingForMillis = wearingSince.let {
            if (it != null) {
                now - it
            } else {
                0
            }
        }
        val wearTimeRemaining = MAX_WEARING_TIME_MILLIS - wornTimeMillis - wearingForMillis

        return now + wearTimeRemaining
    }

    fun isExpired(): Boolean {
        return Date().time >= getExpirationTimestamp()
    }

    fun nameMatchesExactly(query: CharSequence): Boolean {
        return name.normalize() == query.normalize()
    }

    fun getDisplayType(context: Context): String {
        return context.resources.getString(context.resources.getIdentifier("mask_type_${type}", "string", context.packageName))
    }

    override fun toString(): String {
        return "$name ($type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mask

        if (type != other.type) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()

        result = 31 * result + name.hashCode()

        return result
    }
}
