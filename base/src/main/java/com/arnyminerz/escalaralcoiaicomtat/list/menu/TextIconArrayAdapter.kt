package com.arnyminerz.escalaralcoiaicomtat.list.menu

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull

data class TextIcon(val text: String, val icon: Drawable?) {
    constructor(context: Context, @StringRes text: Int, @DrawableRes icon: Int) : this(
        context.getString(text),
        context.getDrawable(icon)
    )
}

class TextIconArrayAdapter(context: Context) :
    ArrayAdapter<TextIcon>(context, android.R.layout.simple_spinner_item) {
    var fontSize: Float? = null

    private fun view(superCall: View, position: Int): View {
        val label = superCall as TextView

        val item = getItem(position)
        if (item.isNotNull()) {
            label.text = item!!.text
            label.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null)
            if (fontSize != null)
                label.textSize = fontSize!!
            label.gravity = Gravity.CENTER_VERTICAL
        }

        return label
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        view(super.getView(position, convertView, parent), position)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        view(super.getDropDownView(position, convertView, parent), position)
}