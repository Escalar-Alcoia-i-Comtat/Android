package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Suppress("unused")
class ChangeNameDialog(private val context: Context) {
    private val alertDialog: AlertDialog? = null

    private var input: EditText? = null
    private var showInput: Boolean = false
    private var inputType: Int = -1
    private var hint: String? = null
    private var defaultText: String? = null

    private var title: String? = null
    private var message: String? = null
    private var positiveButton: Pair<String, (dialog: DialogInterface, which: Int) -> Unit>? = null
    private var negativeButton: Pair<String, (dialog: DialogInterface, which: Int) -> Unit>? = null
    private var neutralButton: Pair<String, (dialog: DialogInterface, which: Int) -> Unit>? = null

    fun title(@StringRes title: Int): ChangeNameDialog {
        this.title = context.getString(title)
        return this
    }

    fun title(title: String): ChangeNameDialog {
        this.title = title
        return this
    }

    fun message(@StringRes message: Int): ChangeNameDialog {
        this.message = context.getString(message)
        return this
    }

    fun message(message: String): ChangeNameDialog {
        this.message = message
        return this
    }

    fun positiveButton(
        @StringRes text: Int,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        positiveButton = Pair(context.getString(text), listener)
        return this
    }

    fun positiveButton(
        text: String,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        positiveButton = Pair(text, listener)
        return this
    }

    fun negativeButton(
        @StringRes text: Int,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        negativeButton = Pair(context.getString(text), listener)
        return this
    }

    fun negativeButton(
        text: String,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        negativeButton = Pair(text, listener)
        return this
    }

    fun neutralButton(
        @StringRes text: Int,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        neutralButton = Pair(context.getString(text), listener)
        return this
    }

    fun neutralButton(
        text: String,
        listener: (dialog: DialogInterface, which: Int) -> Unit
    ): ChangeNameDialog {
        neutralButton = Pair(text, listener)
        return this
    }

    fun input(
        showInput: Boolean = true,
        type: Int = InputType.TYPE_CLASS_TEXT,
        hint: String? = null,
        defaultText: String? = null
    ): ChangeNameDialog {
        this.showInput = showInput
        this.inputType = type
        this.hint = hint
        this.defaultText = defaultText
        return this
    }

    fun getText(): String? = if (input != null) input!!.text.toString() else null

    fun show() {
        val dialog = MaterialAlertDialogBuilder(context)
        if (title != null) dialog.setTitle(title)
        if (message != null) dialog.setMessage(message)
        if (positiveButton != null) dialog
            .setPositiveButton(positiveButton!!.first) { d: DialogInterface, which: Int ->
                positiveButton!!.second(d, which)
            }
        if (negativeButton != null) dialog
            .setNegativeButton(negativeButton!!.first) { d: DialogInterface, which: Int ->
                negativeButton!!.second(d, which)
            }
        if (neutralButton != null) dialog
            .setNeutralButton(neutralButton!!.first) { d: DialogInterface, which: Int ->
                neutralButton!!.second(d, which)
            }
        if (showInput) {
            input = EditText(context)

            input!!.inputType = inputType
            if (hint != null)
                input!!.hint = hint!!
            if (defaultText != null)
                input!!.setText(defaultText)
            dialog.setView(input)
        }
        dialog.show()
    }

    fun hide() {
        alertDialog?.dismiss()
    }
}
