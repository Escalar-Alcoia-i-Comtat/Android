package com.arnyminerz.escalaralcoiaicomtat.data.climb.enum

import android.content.Context
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

enum class SunTime(val value: Int) {
    AllDay(0),
    Morning(1),
    Afternoon(2),
    NoSun(3);

    override fun toString(): String {
        return value.toString()
    }

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
            MaterialAlertDialogBuilder(context)
                .setTitle(sunDialogTitles[value])
                .setMessage(sunDialogMessages[value])
                .setPositiveButton(R.string.action_close) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    companion object {
        private val sunTimes = arrayListOf(AllDay, Morning, Afternoon, NoSun)

        fun find(value: Int): SunTime = sunTimes[value]
    }
}

fun Int?.toSunTime(): SunTime =
    this?.let { SunTime.find(it) } ?: SunTime.NoSun