package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import android.content.Context
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.SunTime
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AFTERNOON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ALL_DAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MORNING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UiThread
fun appendChip(context: Context, @SunTime sunTime: Int, sunChip: Chip) {
    val resources = context.resources
    val suns = resources.getStringArray(R.array.suns)
    val sunDialogTitles = resources.getStringArray(R.array.suns_dialog_title)
    val sunDialogMessages = resources.getStringArray(R.array.suns_dialog_msg)

    sunChip.chipIcon = ContextCompat.getDrawable(
        context,
        when (sunTime) {
            ALL_DAY -> R.drawable.weather_sunset_up
            MORNING -> R.drawable.weather_sunset_down
            AFTERNOON -> R.drawable.weather_sunny
            NO_SUN -> R.drawable.weather_partly_cloudy
            else -> R.drawable.round_close_24
        }
    )
    sunChip.text = suns[sunTime]
    sunChip.setOnClickListener {
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(sunDialogTitles[sunTime])
            .setMessage(sunDialogMessages[sunTime])
            .setPositiveButton(R.string.action_close) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
