package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Path
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import io.noties.markwon.Markwon

@ExperimentalUnsignedTypes
class DescriptionDialog private constructor(private val context: Context, private val path: Path) {
    companion object {
        fun create(context: Context, path: Path): DescriptionDialog? =
            if (path.hasInfo())
                DescriptionDialog(context, path)
            else null
    }

    fun show() {
        val dialog = AlertDialog.Builder(context)
        val factory = LayoutInflater.from(context)
        val view = factory.inflate(R.layout.dialog_description, null)

        val builderTextView = view.findViewById<TextView>(R.id.builtBy_textView)
        val rebuilderTextView = view.findViewById<TextView>(R.id.rebuiltBy_textView)
        val descriptionTextView = view.findViewById<TextView>(R.id.description_textView)

        val markwon = Markwon.create(context)
        if (path.builtBy != null)
            markwon.setMarkdown(builderTextView, path.builtBy)
        else builderTextView.hide()
        if (path.rebuiltBy != null)
            markwon.setMarkdown(rebuilderTextView, path.rebuiltBy)
        else rebuilderTextView.hide()
        if (path.description != null)
            markwon.setMarkdown(descriptionTextView, path.description)
        else descriptionTextView.hide()

        dialog.setView(view)
        dialog.show()
    }
}
