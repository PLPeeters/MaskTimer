package com.plpeeters.masktimer.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import com.plpeeters.masktimer.R
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.databinding.DialogAddMaskBinding
import com.plpeeters.masktimer.databinding.DialogEditWearTimeBinding
import kotlin.math.roundToInt


typealias MaskDataCallback = (type: String, name: String) -> Unit
typealias DurationCallback = (durationSeconds: Int, adding: Boolean) -> Unit
typealias ResponsePositivityCallback = (responseWasPositive: Boolean?) -> Unit


fun AlertDialog.Builder.addMaskDialog(
    masks: List<Mask>,
    callback: MaskDataCallback
): AlertDialog {
    @SuppressLint("InflateParams")
    val binding = DialogAddMaskBinding.inflate(LayoutInflater.from(context))

    setView(binding.root)
    setNeutralButton(android.R.string.cancel) { _, _ -> }
    setPositiveButton(R.string.add_mask) { _, _ ->
        val maskTypeValues: ArrayList<String> =  context.resources.getStringArray(R.array.mask_type_values).toList() as ArrayList<String>

        callback(maskTypeValues[binding.typeField.selectedItemPosition], binding.nameField.getTrimmedText())
    }
    setTitle(R.string.add_mask)

    binding.typeField.setSelection(0)

    val dialog = create()

    val setPositiveButtonState = { addMaskButton: Button ->
        if (binding.typeField.selectedItem.toString().isBlank() ||
            binding.nameField.text.isNullOrBlank() ||
            !binding.nameField.error.isNullOrBlank()) {
            addMaskButton.isEnabled = false
            addMaskButton.isClickable = false
        } else {
            addMaskButton.isEnabled = true
            addMaskButton.isClickable = true
        }
    }

    fun checkNameIsNotTakenForType() {
        val text = binding.nameField.text
        val currentlySelectedMaskType = binding.typeField.selectedItem.toString().lowercase()

        // Display an error and disable the confirmation button if a destination with
        // this name already exists in the current list
        if (masks.any { it.type == currentlySelectedMaskType && it.nameMatchesExactly(text) }) {
            binding.nameField.error = context.resources.getString(R.string.name_is_taken)
        } else {
            binding.nameField.error = null
        }
    }

    binding.nameField.addTextChangedListener {
        checkNameIsNotTakenForType()
        setPositiveButtonState(dialog.getButton(AlertDialog.BUTTON_POSITIVE))
    }

    binding.typeField.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        private val addMaskButton by lazy { dialog.getButton(AlertDialog.BUTTON_POSITIVE) }

        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            checkNameIsNotTakenForType()
            setPositiveButtonState(addMaskButton)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            setPositiveButtonState(addMaskButton)
        }
    }

    return dialog
}

fun AlertDialog.Builder.durationDialog(mask: Mask, callback: DurationCallback): AlertDialog {
    val binding = DialogEditWearTimeBinding.inflate(LayoutInflater.from(context))

    setView(binding.root)
    setNeutralButton(android.R.string.cancel) { _, _ -> }
    setPositiveButton(R.string.add_wear_time) { _, _ ->
        callback(binding.hours.value * 3600 + binding.minutes.value * 60, binding.radioAdd.isChecked)
    }
    setTitle(R.string.adjust_wear_time)

    val maskMaxSubtractionMinutes = (mask.wornTimeMillis / 1000.0 / 60.0).roundToInt()
    val maskUsedLifespanHours = (maskMaxSubtractionMinutes / 60.0).toInt()
    val maskUsedLifespanMinutesMaxHours = maskMaxSubtractionMinutes % 60

    fun updateHoursRangeMax() {
        binding.hours.maxValue = if (!binding.radioAdd.isChecked) {
            maskUsedLifespanHours
        } else {
            12
        }
    }

    fun updateMinutesRange() {
        binding.minutes.maxValue = if (!binding.radioAdd.isChecked && binding.hours.value == maskUsedLifespanHours) {
            maskUsedLifespanMinutesMaxHours
        } else {
            59
        }

        binding.minutes.minValue = if (binding.hours.value > 0) {
            0
        } else {
            1
        }
    }

    binding.hours.minValue = 0
    updateHoursRangeMax()
    binding.hours.setFormatter {
        context.resources.getString(R.string.hours_abbr, it)
    }
    binding.hours.setOnValueChangedListener { _, _, _ ->
        updateMinutesRange()
    }

    // Necessary for the current selection to update its format
    binding.hours.getChildAt(0).let {
        if (it is EditText) {
            it.filters = arrayOfNulls(0)
        }
    }

    updateMinutesRange()
    binding.minutes.setFormatter {
        context.resources.getString(R.string.minutes_abbr, it)
    }

    // Necessary for the current selection to update its format
    binding.minutes.getChildAt(0).let {
        if (it is EditText) {
            it.filters = arrayOfNulls(0)
        }
    }

    val dialog = create()

    binding.radioAdd.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener {
        private val positiveButton by lazy { dialog.getButton(AlertDialog.BUTTON_POSITIVE) }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            updateHoursRangeMax()
            updateMinutesRange()

            if (isChecked) {
                positiveButton.setText(R.string.add_wear_time)
            } else {
                positiveButton.setText(R.string.subtract_wear_time)
            }
        }
    })

    return dialog
}

fun AlertDialog.Builder.simpleDialog(
    title: Int?,
    message: Int,
    positiveButtonTextId: Int,
    negativeButtonTextId: Int? = null,
    callback: ResponsePositivityCallback? = null
): AlertDialog {
    setMessage(message)
        .setPositiveButton(positiveButtonTextId) { _, _ ->
            if (callback != null) {
                callback(true)
            }
        }
        .setOnCancelListener {
            if (callback != null) {
                callback(null)
            }
        }

    if (title != null) {
        setTitle(title)
    }

    if (negativeButtonTextId != null) {
        setNegativeButton(negativeButtonTextId) { _, _ ->
            if (callback != null) {
                callback(false)
            }
        }
    }

    return create()
}
