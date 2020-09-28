package com.arnyminerz.escalaralcoiaicomtat.data.train

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.chip.Chip
import kotlin.math.floor

/**
 * The data class for holding the climb element's data.
 * @param time The time to rest
 */
data class RestDataHolder(var time: Int) : TrainDataHolder {
    constructor(parcel: Parcel) : this(parcel.readInt())

    /**
     * Automatically updates a chip with the text it should have
     * @author Arnau Mora
     * @param context The context for getting the string resources
     * @param chip The chip to update
     */
    override fun updateChip(context: Context, chip: Chip?) {
        val minutes: Int = floor(time / 60.0).toInt()
        val seconds: Int = time - minutes * 60
        val text = context.getString(
            R.string.template_train_time,
            (if (minutes <= 9) "0" else "") + minutes.toString(),
            (if (seconds <= 9) "0" else "") + seconds.toString()
        )
        chip?.setText(text)
    }

    /**
     * Generates a copy of the whole class
     * @author Arnau Mora
     * @return A clone of this class with matching data
     */
    override fun clone() = RestDataHolder(time)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(time)
    }

    override fun describeContents(): Int = 0

    override fun toString(): String =
        "time:$time"

    companion object CREATOR : Parcelable.Creator<RestDataHolder> {
        override fun createFromParcel(parcel: Parcel): RestDataHolder = RestDataHolder(parcel)

        override fun newArray(size: Int): Array<RestDataHolder?> = arrayOfNulls(size)
    }
}