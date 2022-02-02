package com.plpeeters.masktimer.data.persistence

import androidx.room.*


@Dao
interface MaskDao {
    @Query("SELECT * FROM masks")
    fun getAll(): List<MaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userDestinationEntity: MaskEntity)

    @Query("UPDATE masks SET name = :newName WHERE type = :type AND name = :oldName")
    fun updateName(type: String, oldName: String, newName: String)

    @Query("UPDATE masks SET wornTimeMillis = 0, wearingSince = NULL WHERE type = :type AND name = :name")
    fun replace(type: String, name: String)

    @Query("UPDATE masks SET wornTimeMillis = :wornTimeMillis WHERE type = :type AND name = :name")
    fun updateWornTime(type: String, name: String, wornTimeMillis: Long)

    @Query("UPDATE masks SET wearingSince = :wearingSince WHERE type = :type AND name = :name")
    fun updateWearingSince(type: String, name: String, wearingSince: Long?)

    @Query("UPDATE masks SET isPrevious = :isPrevious WHERE type = :type AND name = :name")
    fun setPrevious(type: String, name: String, isPrevious: Boolean)

    @Delete
    fun delete(maskEntity: MaskEntity)

    @Query("DELETE FROM masks")
    fun purge()
}
