package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R

enum class PitchEndingOrientation(val key: String) : Parcelable {
    VERTICAL("vertical"), SHELF("horizontal"), INCLINED("diagonal");

    override fun toString(): String = key

    fun toString(context: Context): String =
        when (this) {
            VERTICAL -> context.getString(R.string.path_ending_pitch_vertical)
            SHELF -> context.getString(R.string.path_ending_pitch_shelf)
            INCLINED -> context.getString(R.string.path_ending_pitch_inclined)
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PitchEndingOrientation> {
        fun find(key: String): PitchEndingOrientation? {
            for (orientation in values())
                if (orientation.key.equals(key, true))
                    return orientation
            return null
        }

        override fun createFromParcel(parcel: Parcel): PitchEndingOrientation? {
            return find(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<PitchEndingOrientation?> {
            return arrayOfNulls(size)
        }
    }
}

enum class PitchEndingRappel(val key: String) : Parcelable {
    RAPPEL("rappel"), EQUIPPED("equipped"), CLEAN("clean");

    override fun toString(): String = key

    fun toString(context: Context): String =
        when (this) {
            RAPPEL -> context.getString(R.string.path_ending_pitch_rappeleable)
            EQUIPPED -> ""
            CLEAN -> context.getString(R.string.path_ending_pitch_clean)
        }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PitchEndingRappel> {
        fun find(key: String): PitchEndingRappel? {
            for (rappel in values())
                if (rappel.key.equals(key, true))
                    return rappel
            return null
        }

        override fun createFromParcel(parcel: Parcel): PitchEndingRappel? {
            return find(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<PitchEndingRappel?> {
            return arrayOfNulls(size)
        }
    }
}
