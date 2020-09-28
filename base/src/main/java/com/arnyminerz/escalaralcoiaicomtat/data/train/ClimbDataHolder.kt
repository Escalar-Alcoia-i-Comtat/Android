package com.arnyminerz.escalaralcoiaicomtat.data.train

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.chip.Chip
import kotlin.math.floor

const val CLIMB_STAY_TYPE_SECONDS = 0
const val CLIMB_STAY_TYPE_STEPS = 1

@IntDef(CLIMB_STAY_TYPE_SECONDS, CLIMB_STAY_TYPE_STEPS)
@Retention(AnnotationRetention.SOURCE)
annotation class ClimbStayType

/**
 * The data class for holding the climb element's data.
 * @param stay How much seconds should you stay, or how much steps should you do. This is stored in the same field, and formatted for the user
 * @param type The stay type
 */
data class ClimbDataHolder(var stay: Int, @ClimbStayType var type: Int) : TrainDataHolder {
    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt())

    /**
     * Changes the type variable
     * @author Arnau Mora
     */
    fun flip() {
        type = if (type == CLIMB_STAY_TYPE_SECONDS)
            CLIMB_STAY_TYPE_STEPS
        else CLIMB_STAY_TYPE_SECONDS
    }

    /**
     * Gets the icon that should be displayed to describe the type in a chip
     * @author Arnau Mora
     * @return The Drawable Resource for the icon
     */
    @DrawableRes
    fun icon(): Int =
        if (type == CLIMB_STAY_TYPE_STEPS) R.drawable.round_av_timer_24 else R.drawable.round_timer_24

    /**
     * Gets the text that should be displayed to describe the current stay based on type in a chip
     * @author Arnau Mora
     * @param context The context to run the string resource get
     * @return The text for the chip
     */
    fun chipText(context: Context): String =
        if (type == CLIMB_STAY_TYPE_STEPS)
            context.getString(R.string.template_train_steps, stay)
        else {
            val minutes: Int = floor(stay / 60.0).toInt()
            val seconds: Int = stay - minutes * 60
            context.getString(
                R.string.template_train_time,
                (if (minutes <= 9) "0" else "") + minutes.toString(),
                (if (seconds <= 9) "0" else "") + seconds.toString()
            )
        }

    /**
     * Automatically updates a chip with the chipText and icon functions
     * @author Arnau Mora
     * @param context The context to run chipText from
     * @param chip The chip to update
     */
    override fun updateChip(context: Context, chip: Chip?) {
        chip?.setText(chipText(context))
        chip?.setChipIconResource(icon())
    }

    /**
     * Generates a copy of the whole class
     * @author Arnau Mora
     * @return A clone of this class with matching data
     */
    override fun clone() = ClimbDataHolder(stay, type)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(stay)
        parcel.writeInt(type)
    }

    override fun describeContents(): Int = 0

    override fun toString(): String =
        "stay: $stay; type: $type"

    companion object CREATOR : Parcelable.Creator<ClimbDataHolder> {
        override fun createFromParcel(parcel: Parcel): ClimbDataHolder = ClimbDataHolder(parcel)

        override fun newArray(size: Int): Array<ClimbDataHolder?> = arrayOfNulls(size)
    }
}