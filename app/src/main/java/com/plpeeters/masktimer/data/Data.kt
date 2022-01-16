package com.plpeeters.masktimer.data

import android.content.Context
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import java.util.concurrent.locks.ReentrantLock


object Data {
    private val dataLoadLock = ReentrantLock()
    private var dataLoaded = false

    val MASKS = ArrayList<Mask>()

    fun load(context: Context) {
        synchronized(dataLoadLock) {
            if (!dataLoaded) {
                MASKS.addAll(MaskDatabaseSingleton(context).maskDatabaseDao()
                    .getAll().map { maskEntity ->
                        Mask.fromMaskEntity(maskEntity)
                    })

                dataLoaded = true
            }
        }
    }
}
