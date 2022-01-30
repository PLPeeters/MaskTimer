package com.plpeeters.masktimer.data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
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
): Parcelable {
    companion object {
        @JvmField
        @Suppress("unused")
        val CREATOR: Parcelable.Creator<Mask?> = object : Parcelable.Creator<Mask?> {
            override fun createFromParcel(parcel: Parcel): Mask {
                return Mask(parcel)
            }

            override fun newArray(size: Int): Array<Mask?> {
                return arrayOfNulls(size)
            }
        }

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
    var isPaused = false
    var isPrevious = false
        set(value) {
            field = value

            updatePreviousStateInDb()
        }

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

    private constructor(parcel: Parcel): this(
        parcel.readString()!!,
        parcel.readString()!!
    ) {
        wornTimeMillis = parcel.readLong()
        wearingSince = parcel.readValue(Long::class.java.classLoader) as? Long
        isPaused = parcel.readByte() != 0.toByte()
        isPrevious = parcel.readByte() != 0.toByte()
    }

    private fun updatePreviousStateInDb() {
        isPrevious.let {
            Thread {
                database.setPrevious(type, name, it)
            }.start()
        }
    }

    private fun updateWornTimeInDb() {
        wornTimeMillis.let {
            Thread {
                database.updateWornTime(type, name, it)
            }.start()
        }
    }

    fun toMaskEntity(): MaskEntity {
        return MaskEntity(type, name)
    }

    fun startWearing() {
        val now = Date().time

        wearingSince = now
        isPaused = false

        wearingSince.let {
            Thread {
                database.updateWearingSince(type, name, it)
            }.start()
        }
    }

    fun pauseWearing() {
        stopWearing()
        isPaused = true
    }

    fun stopWearing() {
        wornTimeMillis += Date().time - wearingSince!!
        wearingSince = null

        updateWornTimeInDb()
    }

    fun addWearTime(seconds: Int) {
        wornTimeMillis += seconds * 1000
        updateWornTimeInDb()
    }

    fun subtractWearTime(seconds: Int) {
        wornTimeMillis = (wornTimeMillis - seconds * 1000).coerceAtLeast(0)
        updateWornTimeInDb()
    }

    fun replace() {
        isBeingWorn.let { wasBeingWorn ->
            wornTimeMillis = 0L
            wearingSince = null

            Thread {
                database.replace(type, name)
            }.start()

            if (wasBeingWorn || isPaused) {
                startWearing()
            }
        }
    }

    fun getRemainingLifespanMillis(context: Context): Long {
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

        return maxWearingTime - totalWearTimeMillisNow
    }

    fun getExpirationTimestamp(context: Context): Long {
        return Date().time + getRemainingLifespanMillis(context)
    }

    fun isExpired(context: Context): Boolean {
        return getRemainingLifespanMillis(context) <= 0
    }

    fun nameMatchesExactly(query: CharSequence): Boolean {
        return name.normalize() == query.normalize()
    }

    fun getDisplayType(context: Context): String {
        return context.resources.getString(context.resources.getIdentifier("mask_type_${type}", "string", context.packageName))
    }

    fun delete() {
        wornTimeMillis = -1
        wearingSince = null
        isPaused = false
        isPrevious = false

        Thread {
            database.delete(toMaskEntity())
        }.start()
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(name)
        parcel.writeLong(wornTimeMillis)
        parcel.writeValue(wearingSince)
        parcel.writeByte(if (isPaused) 1 else 0)
        parcel.writeByte(if (isPrevious) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }
}
