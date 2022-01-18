package com.plpeeters.masktimer

import androidx.lifecycle.ViewModel
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.data.Data


class MaskListViewModel: ViewModel() {
    val masks: ArrayList<Mask> = Data.MASKS
    var currentMask: Mask? = null
    var previousMask: Mask? = null

    init {
        for (mask in masks) {
            if (mask.isBeingWorn || mask.isPaused) {
                currentMask = mask
            } else if (mask.isPrevious) {
                previousMask = mask
            }

            if (currentMask != null && previousMask != null) {
                break
            }
        }
    }
}
