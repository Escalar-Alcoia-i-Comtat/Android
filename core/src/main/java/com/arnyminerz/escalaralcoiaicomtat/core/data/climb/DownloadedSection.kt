package com.arnyminerz.escalaralcoiaicomtat.data.climb

import android.app.Activity
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ROTATION_A
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ROTATION_B
import com.arnyminerz.escalaralcoiaicomtat.core.shared.TOGGLE_ANIMATION_DURATION
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import timber.log.Timber

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
         * @param activity The [Activity] where the function is being ran on.
         * @param firestore The [FirebaseFirestore] instance to load the data from.
         * @param showNonDownloaded If the non-downloaded sections should be added.
         * @param progressListener A listener for the progress of the load.
         * @return The sections that have been downloaded
         */
        @WorkerThread
        suspend fun list(
            activity: Activity,
            firestore: FirebaseFirestore,
            showNonDownloaded: Boolean,
            progressListener: suspend (current: Int, max: Int) -> Unit
        ) = flow {
            Timber.v("Loading downloads...")
            for (area in AREAS)
                emitAll(
                    area.downloadedSectionList(
                        activity,
                        firestore,
                        showNonDownloaded,
                        progressListener
                    )
                )
        }
    }

    /**
     * Updates the UI to match the section's toggle status.
     * @author Arnau Mora
     * @since 20210413
     * @param view The root view of the card
     * @param toggleButton The button that toggles the card
     * @param recyclerView The recycler view for the children.
     */
    @UiThread
    fun updateView(
        view: ViewGroup,
        toggleButton: ImageButton,
        recyclerView: RecyclerView
    ) {
        toggleButton.animate()
            .rotation(if (toggled) ROTATION_A else ROTATION_B)
            .setDuration(TOGGLE_ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        TransitionManager.beginDelayedTransition(
            view, TransitionSet().addTransition(ChangeBounds())
        )

        visibility(recyclerView, !toggled)
    }

    /**
     * Changes the status of [toggled] to the opposite one, and updates the UI.
     * @author Arnau Mora
     * @since 20210413
     * @param view The root view of the card
     * @param toggleButton The button that toggles the card
     * @param recyclerView The recycler view for the children.
     */
    @UiThread
    fun toggle(view: ViewGroup, toggleButton: ImageButton, recyclerView: RecyclerView) {
        toggled = !toggled

        updateView(view, toggleButton, recyclerView)
    }
}
