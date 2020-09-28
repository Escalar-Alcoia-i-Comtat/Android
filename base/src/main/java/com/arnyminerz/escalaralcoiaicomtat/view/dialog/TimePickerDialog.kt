package com.arnyminerz.escalaralcoiaicomtat.view.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R


class TimePickerDialog(private val activity: Activity) {
    private val d = AlertDialog.Builder(activity)
    private val secondsNumberPicker: NumberPicker
    private val minutesNumberPicker: NumberPicker

    private var positiveButton: Pair<String, (dialog: DialogInterface, value: Int) -> Unit> =
        Pair(activity.getString(R.string.action_ok)) { _, _ -> }
    private var negativeButton: Pair<String, (dialog: DialogInterface) -> Unit> =
        Pair(activity.getString(R.string.action_cancel)) {}

    init {
        val inflater: LayoutInflater = activity.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.time_picker_dialog, null)
        d.setTitle("Title")
        d.setMessage("Message")
        d.setView(dialogView)

        minutesNumberPicker = dialogView.findViewById(R.id.dialog_minutes_picker)
        minutesNumberPicker.maxValue = 1000
        minutesNumberPicker.minValue = 0
        minutesNumberPicker.wrapSelectorWheel = false

        secondsNumberPicker = dialogView.findViewById(R.id.dialog_seconds_picker)
        secondsNumberPicker.maxValue = 59
        secondsNumberPicker.minValue = 0
        secondsNumberPicker.wrapSelectorWheel = false
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
                minutesNumberPicker.value * 60 + secondsNumberPicker.value
            )
        }
        d.setNegativeButton(negativeButton.first) { dialog, _ -> negativeButton.second(dialog) }
        return d.create()
    }
}