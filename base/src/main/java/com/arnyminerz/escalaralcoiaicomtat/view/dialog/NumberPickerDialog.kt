package com.arnyminerz.escalaralcoiaicomtat.view.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R
import timber.log.Timber


class NumberPickerDialog(private val activity: Activity) {
    private val d = AlertDialog.Builder(activity)
    private val numberPicker: NumberPicker

    private var positiveButton: Pair<String, (dialog: DialogInterface, value: Int) -> Unit> =
        Pair(activity.getString(R.string.action_ok)) { _, _ -> }
    private var negativeButton: Pair<String, (dialog: DialogInterface) -> Unit> =
        Pair(activity.getString(R.string.action_cancel)) {}

    init {
        val inflater: LayoutInflater = activity.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.number_picker_dialog, null)
        d.setTitle("Title")
        d.setMessage("Message")
        d.setView(dialogView)
        numberPicker = dialogView.findViewById(R.id.dialog_number_picker)
        numberPicker.maxValue = 50
        numberPicker.minValue = 1
        numberPicker.wrapSelectorWheel = false
        numberPicker.setOnValueChangedListener { _, i, i1 ->
            Timber.d("onValueChange: $i, $i1")
        }
    }

    fun setTitle(@StringRes title: Int) {
        d.setTitle(title)
    }

    fun setTitle(title: CharSequence) {
        d.setTitle(title)
    }

    fun setMessage(@StringRes message: Int) {
        d.setMessage(message)
    }

    fun setMessage(message: CharSequence) {
        d.setMessage(message)
    }

    fun setMax(max: Int) {
        numberPicker.maxValue = max
    }

    fun setMin(min: Int) {
        numberPicker.minValue = min
    }

    fun setDefault(value: Int) {
        numberPicker.value = value
    }

    fun setPositiveButton(
        @StringRes text: Int,
        listener: (dialog: DialogInterface, value: Int) -> Unit
    ) {
        positiveButton = Pair(activity.getString(text), listener)
    }

    fun setPositiveButton(text: String, listener: (dialog: DialogInterface, value: Int) -> Unit) {
        positiveButton = Pair(text, listener)
    }

    fun setNegativeButton(@StringRes text: Int, listener: (dialog: DialogInterface) -> Unit) {
        negativeButton = Pair(activity.getString(text), listener)
    }

    fun setNegativeButton(text: String, listener: (dialog: DialogInterface) -> Unit) {
        negativeButton = Pair(text, listener)
    }

    fun build(): AlertDialog {
        d.setPositiveButton(positiveButton.first) { dialog, _ ->
            positiveButton.second(
                dialog,
                numberPicker.value
            )
        }
        d.setNegativeButton(negativeButton.first) { dialog, _ -> negativeButton.second(dialog) }
        return d.create()
    }
}