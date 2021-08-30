package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.view.viewListOf
import io.noties.markwon.Markwon

class DescriptionDialog private constructor(private val context: Context, private val path: Path) {
    companion object {
        fun create(context: Context, path: Path): DescriptionDialog? =
            if (path.hasInfo())
                DescriptionDialog(context, path)
            else null
    }

    fun show() {
        val dialog = AlertDialog.Builder(context, R.style.ThemeOverlay_App_AlertDialog)
        val factory = LayoutInflater.from(context)
        val view = factory.inflate(R.layout.dialog_description, null)

        val builderTextView = view.findViewById<TextView>(R.id.builtBy_textView)
        val rebuilderTextView = view.findViewById<TextView>(R.id.rebuiltBy_textView)
        val descriptionTextView = view.findViewById<TextView>(R.id.description_textView)

        val builderTitleTextView = view.findViewById<TextView>(R.id.builtBy_titleTextView)
        val rebuilderTitleTextView = view.findViewById<TextView>(R.id.rebuiltBy_titleTextView)
        val descriptionTitleTextView = view.findViewById<TextView>(R.id.description_titleTextView)

        val referenceTextView = view.findViewById<TextView>(R.id.reference_textView)

        val markwon = Markwon.create(context)
        if (path.builtBy != null)
            markwon.setMarkdown(builderTextView, path.builtBy!!)
        else
            viewListOf(builderTextView, builderTitleTextView).visibility(false)
        if (path.rebuiltBy != null)
            markwon.setMarkdown(rebuilderTextView, path.rebuiltBy!!)
        else viewListOf(rebuilderTextView, rebuilderTitleTextView).visibility(false)
        if (path.description != null)
            markwon.setMarkdown(descriptionTextView, path.description!!)
        else viewListOf(descriptionTextView, descriptionTitleTextView).visibility(false)

        referenceTextView.text =
            context.getString(R.string.dialog_description_reference, path.documentPath)
                .replace("Areas/", "")
                .replace("Zones/", "")
                .replace("Sectors/", "")
                .replace("Paths/", "")

        dialog.setView(view)
        dialog.show()
    }
}
