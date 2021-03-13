package com.arnyminerz.escalaralcoiaicomtat.data.climb.download

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.TOGGLED_CARD_HEIGHT
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber

private const val ROTATION_A = 90f
private const val ROTATION_B = -90f

private const val TOGGLE_ANIMATION_DURATION: Long = 300

@Suppress("UNCHECKED_CAST")
// This must be Serializable and not DataClass because A at DataClass can't be specified as DataClass
data class DownloadedSection(val section: DataClass<*, *>) {
    /**
     * toggled -> true : Content hidden
     */
    private var toggled: Boolean = true

    companion object {
        /**
         * Gets all the downloaded sections
         * @date 14/05/2020
         * @patch Arnau Mora - 2020/09/11
         * @author ArnyminerZ
         * @return The sections that have been downloaded
         */
        fun list(): ArrayList<DownloadedSection> {
            Timber.v("Loading downloads...")
            val list = arrayListOf<DownloadedSection>()

            for (area in AREAS.values)
                list.addAll(area.downloadedSectionList())

            return list
        }
    }

    fun updateView(
        view: ViewGroup,
        toggleButton: ImageButton,
        list: RecyclerView,
        context: Context
    ) {
        toggleButton.animate()
            .rotation(if (toggled) ROTATION_A else ROTATION_B)
            .setDuration(TOGGLE_ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        visibility(list, !toggled)

        TransitionManager.beginDelayedTransition(
            view, TransitionSet().addTransition(ChangeBounds())
        )

        val params = view.layoutParams
        params.height = if (toggled) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            TOGGLED_CARD_HEIGHT,
            context.resources.displayMetrics
        ).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
        view.layoutParams = params
    }

    fun toggle(view: ViewGroup, toggleButton: ImageButton, list: RecyclerView, context: Context) {
        toggled = !toggled

        updateView(view, toggleButton, list, context)
    }
}
