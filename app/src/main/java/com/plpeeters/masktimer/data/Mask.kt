package com.plpeeters.masktimer.data

import android.content.Context
import com.plpeeters.masktimer.Preferences
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import com.plpeeters.masktimer.data.persistence.MaskEntity
import com.plpeeters.masktimer.data.persistence.MaskTypes
import com.plpeeters.masktimer.utils.getSharedPreferences
import com.plpeeters.masktimer.utils.normalize
import java.lang.RuntimeException
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

    val isBeingWorn: Boolean
        get() {
            return wearingSince != null
        }
    private val totalWearTimeMillisNow: Long
        get() {
            val wearingForMillis = wearingSince.let {
                if (it != null) {
                    Date().time - it
                } else {
                    0
                }
            }

            return wearingForMillis + wornTimeMillis
        }

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

    fun getExpirationTimestamp(context: Context): Long {
        val now = Date().time

        val maxWearingTimeHours = when (type) {
            MaskTypes.SURGICAL -> context.getSharedPreferences().getString(Preferences.SURGICAL_MASK_EXPIRATION_HOURS, null)?.toInt()
            MaskTypes.FFP -> context.getSharedPreferences().getString(Preferences.FFP_MASK_EXPIRATION_HOURS, null)?.toInt()
            else -> throw RuntimeException("Unknown mask type: $type")
        }

        val maxWearingTime = if (maxWearingTimeHours != null) {
            maxWearingTimeHours * 3600 * 1000L
        } else {
            -1
        }

        return now + maxWearingTime - totalWearTimeMillisNow
    }

    fun isExpired(context: Context): Boolean {
        return Date().time >= getExpirationTimestamp(context)
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
