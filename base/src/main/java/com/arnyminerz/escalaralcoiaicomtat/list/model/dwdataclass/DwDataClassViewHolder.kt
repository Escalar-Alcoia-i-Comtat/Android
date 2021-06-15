package com.arnyminerz.escalaralcoiaicomtat.list.model.dwdataclass

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.google.android.material.progressindicator.BaseProgressIndicator

/**
 * Serves as an interface for a [DataClass] that can be downloaded.
 * @author Arnau Mora
 * @since 20210417
 * @param view The root view of the element
 */
class DwDataClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    /**
     * Specifies the [ImageView] that will show the user a preview of the [DataClass].
     * @author Arnau Mora
     * @since 20210417
     */
    val imageView: ImageView = view.findViewById(R.id.imageView)

    /**
     * Specifies the [TextView] that shows the user the name of the [DataClass].
     * @author Arnau Mora
     * @since 20210417
     */
    val titleTextView: TextView = view.findViewById(R.id.title_textView)

    /**
     * Specifies the [ImageButton] that will offer the user a quick access to the [DataClass]'s map.
     * @author Arnau Mora
     * @since 20210417
     */
    val mapImageButton: ImageButton = view.findViewById(R.id.map_imageButton)

    /**
     * Specifies the [ImageButton] that will offer the user the possibility to download the [DataClass].
     * @author Arnau Mora
     * @since 20210417
     */
    val downloadImageButton: ImageButton = view.findViewById(R.id.download_imageButton)

    /**
     * Specifies a progress bar that will tell the user when something is getting loaded.
     * @author Arnau Mora
     * @since 20210417
     */
    val progressIndicator: BaseProgressIndicator<*> = view.findViewById(R.id.progressIndicator)
}
