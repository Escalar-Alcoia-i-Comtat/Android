package com.arnyminerz.escalaralcoiaicomtat.activity.train

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.train.ClimbDataHolder
import com.arnyminerz.escalaralcoiaicomtat.data.train.RestDataHolder
import com.arnyminerz.escalaralcoiaicomtat.data.train.TrainDataHolder
import com.arnyminerz.escalaralcoiaicomtat.drag.TrainingCardShadowBuilder
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull
import com.arnyminerz.escalaralcoiaicomtat.generic.loadLocale
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.view.train.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_train_main.*
import org.jetbrains.anko.toast
import timber.log.Timber
import java.util.*

@ExperimentalUnsignedTypes
class TrainMainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val dataList = arrayListOf<TrainElement>()

    var dragX = -1f
    var dragY = -1f
    private val trainingCardDragListener = View.OnDragListener { view, event ->
        val layout = view as? LinearLayout
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (!event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                    return@OnDragListener false

                layout?.apply {
                    setBackgroundColor(getColor(R.color.grade_blue))
                    invalidate()
                }

                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                layout?.apply {
                    setBackgroundColor(getColor(R.color.grade_green))
                    invalidate()
                }
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                // Ignore the event
                dragX = event.x
                dragY = event.y
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                layout?.apply {
                    setBackgroundColor(getColor(R.color.grade_blue))
                    invalidate()
                }
                true
            }
            DragEvent.ACTION_DROP -> {
                val item: ClipData.Item = event.clipData.getItemAt(0)
                val dragData = item.text
                val dragFrom = dragData.split(" ").last().toIntOrNull()
                val draggedData = if (dragFrom.isNotNull()) dragData.toString()
                    .replace(" " + dragData.split(" ").last(), "") else dragData.toString()

                Timber.d("Drag data is \"$dragData\". DraggedData: \"$draggedData\". DragFrom: $dragFrom. Dropped on $dragX, $dragY")

                var data: TrainDataHolder? = null
                if (dragFrom != null) {
                    val d = dataList[dragFrom]
                    data = d.data
                    dataList.remove(d)
                    training_list_layout.removeViewAt(dragFrom)
                    Timber.v("Removed view at $dragFrom")
                }

                val element = when (draggedData) {
                    "type climb" -> ClimbElement(
                        this,
                        training_list_layout,
                        Pair(dragX, dragY),
                        (data as? ClimbDataHolder) ?: defaultClimbData
                    )
                    "type rest" -> RestElement(
                        this,
                        training_list_layout,
                        Pair(dragX, dragY),
                        (data as? RestDataHolder) ?: defaultRestData
                    )
                    else -> null
                }
                if (element != null)
                    dataList.add(element.index, element)

                Timber.v("Added new $draggedData element to the training")

                layout?.apply {
                    setBackgroundColor(getColor(R.color.bg_color))
                    invalidate()
                }

                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                layout?.apply {
                    setBackgroundColor(getColor(R.color.bg_color))
                    invalidate()
                }

                if (event.result)
                    Timber.v("Drag ended!")
                else Timber.e("Drag ended with false result! :(")

                true
            }
            else -> {
                // An unknown action type was received.
                Timber.e("Unknown action type received by OnDragListener.")
                false
            }
        }
    }
    private val deleteDragListener = View.OnDragListener { view, event ->
        val imageView = view as? ImageView
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (!event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                    return@OnDragListener false

                imageView?.apply {
                    Timber.v("Showing trash icon")
                    setColorFilter(getColor(R.color.grade_blue))
                    setImageResource(R.drawable.round_delete_24)
                    show()
                    invalidate()
                } ?: Timber.e("Trash icon is null")

                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                imageView?.apply {
                    Timber.v("Showing trash icon")
                    setColorFilter(getColor(R.color.grade_red))
                    setImageResource(R.drawable.round_delete_forever_24)
                    show()
                    invalidate()
                } ?: Timber.e("Trash icon is null")
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                imageView?.apply {
                    Timber.v("Showing trash icon")
                    setColorFilter(getColor(R.color.grade_red))
                    setImageResource(R.drawable.round_delete_forever_24)
                    show()
                    invalidate()
                } ?: Timber.e("Trash icon is null")
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                imageView?.apply {
                    Timber.v("Showing trash icon")
                    setColorFilter(getColor(R.color.grade_blue))
                    show()
                    setImageResource(R.drawable.round_delete_24)
                    invalidate()
                } ?: Timber.e("Trash icon is null")
                true
            }
            DragEvent.ACTION_DROP -> {
                val item: ClipData.Item = event.clipData.getItemAt(0)
                val dragData = item.text
                val index = dragData.toString().toIntOrNull()

                Timber.d("Dragged data is $dragData")

                if (index != null && event.result) {
                    Timber.v("Removing training element at index $index")
                    training_list_layout.removeViewAt(index)
                } else
                    Timber.e("Could not delete element of the training since data doesn't have index. Result: ${event.result}")

                imageView?.apply {
                    setColorFilter(null)
                    invalidate()
                } ?: Timber.e("Trash icon is null")

                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                imageView?.apply {
                    Timber.v("Hiding trash icon")
                    setColorFilter(null)
                    hide()
                    invalidate()
                }

                true
            }
            else -> {
                // An unknown action type was received.
                Timber.e("Unknown action type received by OnDragListener.")
                false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        Timber.v("Planted Timber.")
        setContentView(R.layout.activity_train_main)

        loadLocale()

        tts = TextToSpeech(this, this)

        /*train_climb_timer_type_switch.setOnCheckedChangeListener { switch, checked ->
            val chip = (switch.parent.parent as? LinearLayout)?.findViewWithTag<Chip>("timer")
            chip?.chipIcon = ContextCompat.getDrawable(
                this,
                if (checked) R.drawable.round_av_timer_24 else R.drawable.round_timer_24
            )
        }
        climbCardView = train_climb_card*/

        train_element_climb.setOnLongClickListener { v ->
            val tag = v.tag as? CharSequence
            val item = ClipData.Item(tag)

            val dragData = ClipData(
                tag,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )

            val shadow = TrainingCardShadowBuilder(v)

            v.startDragAndDrop(dragData, shadow, null, 0)
        }

        train_element_rest.setOnLongClickListener { v ->
            val tag = v.tag as? CharSequence
            val item = ClipData.Item(tag)

            val dragData = ClipData(
                tag,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )

            val shadow = TrainingCardShadowBuilder(v)

            v.startDragAndDrop(dragData, shadow, null, 0)
        }

        training_list_layout.setOnDragListener(trainingCardDragListener)
        train_delete_icon.setOnDragListener(deleteDragListener)

        train_fab.setOnClickListener {
            var msg = ""

            for (data in dataList)
                when (data) {
                    is ClimbElement -> msg += "${data.index}: ${data.data}\n"
                    is RestElement -> msg += "${data.index}: ${data.data}\n"
                }

            MaterialAlertDialogBuilder(this)
                .setTitle("Training")
                .setMessage(msg)
            //.show()

        }
    }

    private var tts: TextToSpeech? = null

    override fun onInit(status: Int) {
        if (tts != null && status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                toast(R.string.toast_error_tts_language_unsupported)
                tts = null
            }
        } else toast(R.string.toast_error_tts_init_failed)
    }
}