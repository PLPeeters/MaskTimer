package com.plpeeters.masktimer.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import com.google.android.material.internal.TextWatcherAdapter
import com.plpeeters.masktimer.R
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.databinding.DialogAddMaskBinding


typealias MaskDataCallback = (type: String, name: String) -> Unit


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

    binding.nameField.addTextChangedListener(object : TextWatcherAdapter() {
        private val addMaskButton by lazy { dialog.getButton(AlertDialog.BUTTON_POSITIVE) }

        override fun afterTextChanged(s: Editable) {
            checkNameIsNotTakenForType()
            setPositiveButtonState(addMaskButton)
        }
    })

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
