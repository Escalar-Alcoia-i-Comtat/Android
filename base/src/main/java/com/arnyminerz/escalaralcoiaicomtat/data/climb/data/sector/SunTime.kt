package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.shared.AFTERNOON_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.ALL_DAY_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.MORNING_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.NO_SUN_INDEX
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

enum class SunTime(val value: Int) : Parcelable {
    AllDay(ALL_DAY_INDEX),
    Morning(MORNING_INDEX),
    Afternoon(AFTERNOON_INDEX),
    NoSun(NO_SUN_INDEX);

    override fun toString(): String = value.toString()

    fun appendChip(context: Context, sunChip: Chip) {
        val resources = context.resources
        val suns = resources.getStringArray(R.array.suns)
        val sunDialogTitles = resources.getStringArray(R.array.suns_dialog_title)
        val sunDialogMessages = resources.getStringArray(R.array.suns_dialog_msg)

        sunChip.chipIcon = ContextCompat.getDrawable(
            context,
            when (this) {
                Morning -> R.drawable.weather_sunset_up
                Afternoon -> R.drawable.weather_sunset_down
                AllDay -> R.drawable.weather_sunny
                NoSun -> R.drawable.weather_partly_cloudy
            }
        )
        sunChip.text = suns[value]
        sunChip.setOnClickListener {
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(sunDialogTitles[value])
                .setMessage(sunDialogMessages[value])
                .setPositiveButton(R.string.action_close) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(value)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SunTime> {
        private val sunTimes = arrayListOf(AllDay, Morning, Afternoon, NoSun)

        fun find(value: Int): SunTime = sunTimes[value]

        override fun createFromParcel(parcel: Parcel): SunTime {
            return find(parcel.readInt())
        }

        override fun newArray(size: Int): Array<SunTime?> {
            return arrayOfNulls(size)
        }
    }
}
