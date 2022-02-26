package com.arnyminerz.escalaralcoiaicomtat.core.annotations

import androidx.annotation.DrawableRes
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AFTERNOON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ALL_DAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MORNING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN
import compose.icons.WeatherIcons
import compose.icons.weathericons.Cloudy
import compose.icons.weathericons.DaySunny
import compose.icons.weathericons.Na
import compose.icons.weathericons.Sunrise
import compose.icons.weathericons.Sunset

@StringDef(ALL_DAY, MORNING, AFTERNOON, NO_SUN)
@Retention(AnnotationRetention.SOURCE)
annotation class SunTime

/**
 * Gets the string resource that should be using to represent the sun time.
 * @author Arnau Mora
 * @since 20220106
 */
val @receiver:SunTime String.textResource: Int
    @StringRes
    get() = when (this) {
        ALL_DAY -> R.string.sector_sun_day
        MORNING -> R.string.sector_sun_morning
        AFTERNOON -> R.string.sector_sun_afternoon
        NO_SUN -> R.string.sector_sun_no
        else -> R.string.toast_error_internal
    }

@Deprecated("Use Jetpack Compose", replaceWith = ReplaceWith("vector"))
val @receiver:SunTime String.icon: Int
    @DrawableRes
    get() = when (this) {
        ALL_DAY -> R.drawable.weather_sunny
        MORNING -> R.drawable.weather_sunset_up
        AFTERNOON -> R.drawable.weather_sunset_down
        NO_SUN -> R.drawable.weather_partly_cloudy
        else -> R.drawable.round_close_24
    }

/**
 * Returns the [ImageVector] that corresponds to the select [SunTime].
 * @author Arnau Mora
 * @since 20220226
 */
val @receiver:SunTime String.vector: ImageVector
    get() = when (this) {
        ALL_DAY -> WeatherIcons.DaySunny
        MORNING -> WeatherIcons.Sunrise
        AFTERNOON -> WeatherIcons.Sunset
        NO_SUN -> WeatherIcons.Cloudy
        else -> WeatherIcons.Na
    }
