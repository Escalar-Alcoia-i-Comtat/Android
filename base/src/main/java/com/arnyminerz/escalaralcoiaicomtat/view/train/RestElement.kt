package com.arnyminerz.escalaralcoiaicomtat.view.train

import android.app.Activity
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.children
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.train.RestDataHolder
import com.arnyminerz.escalaralcoiaicomtat.list.viewListOf
import com.arnyminerz.escalaralcoiaicomtat.view.dialog.TimePickerDialog
import com.arnyminerz.escalaralcoiaicomtat.view.isInside
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import timber.log.Timber

val defaultRestData
    get() = RestDataHolder(0).clone()

class RestElement(
    activity: Activity,
    parentLayout: LinearLayout,
    releasePosition: Pair<Float, Float>,
    data: RestDataHolder = defaultRestData.clone()
) : TrainElement(activity, parentLayout, data) {
    private var timerChip: Chip? = null
    private var moveUpButton: ImageButton? = null
    private var moveDownButton: ImageButton? = null

    val element = activity.layoutInflater.inflate(
        R.layout.train_element_rest,
        parentLayout,
        false
    ) as? MaterialCardView

    private fun updateMoveButtonsVisibility() {
        visibility(moveUpButton, index != 0)
        visibility(moveDownButton, index < parentLayout.childCount - 1)

        for ((index, child) in parentLayout.children.withIndex()) {
            val moveUpRest = child.findViewById<ImageButton>(R.id.train_rest_move_up)
            val moveDownRest = child.findViewById<ImageButton>(R.id.train_rest_move_down)
            val moveUpClimb = child.findViewById<ImageButton>(R.id.train_climb_move_up)
            val moveDownClimb = child.findViewById<ImageButton>(R.id.train_climb_move_down)

            visibility(moveUpRest, index != 0)
            visibility(moveDownRest, index < parentLayout.childCount - 1)

            visibility(moveUpClimb, index != 0)
            visibility(moveDownClimb, index < parentLayout.childCount - 1)
        }
    }

    init {
        // This is for getting the index
        for ((i, child) in parentLayout.children.withIndex())
            if (child.isInside(releasePosition)) {
                val relY =
                    releasePosition.second - child.y // This is the y position relative to the view
                val halfHeight =
                    child.height / 2 // Get half of the height to calculate if below mid view
                index = i // By default, set the index on top of view
                if (relY > halfHeight) // This means the new has been dropped below the view
                    index++ // Put the item 1 below
            } else Timber.d("Dropped item is not in child $i")

        parentLayout.addView(element)
        element?.apply {
            timerChip = findViewById(R.id.train_rest_timer_chip)
            moveUpButton = findViewById(R.id.train_rest_move_up)
            moveDownButton = findViewById(R.id.train_rest_move_down)

            timerChip?.setOnClickListener {
                TimePickerDialog(activity).apply {
                    setTitle(R.string.dialog_time_title)
                    setMessage(R.string.dialog_time_message)
                    setPositiveButton(R.string.action_ok) { dialog, value ->
                        dialog.dismiss()
                        data.time = value
                        data.updateChip(activity, timerChip)
                    }
                }.build().show()
            }
            data.updateChip(activity, timerChip)

            Timber.v("Index of the added ClimbElement: $index")
            if (index >= 0) {
                moveUpButton?.setOnClickListener {
                    parentLayout.removeView(element)
                    parentLayout.addView(element, --index)
                    updateMoveButtonsVisibility()
                }
                moveDownButton?.setOnClickListener {
                    parentLayout.removeView(element)
                    parentLayout.addView(element, ++index)
                    updateMoveButtonsVisibility()
                }
                updateMoveButtonsVisibility()
            } else
                visibility(viewListOf(moveUpButton, moveDownButton), false)
        } ?: Timber.e("Could not cast to LinearLayout. Type: ${element!!::class.java.simpleName}")
    }
}