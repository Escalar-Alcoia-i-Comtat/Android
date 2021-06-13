package com.arnyminerz.escalaralcoiaicomtat.core.notification

import android.app.PendingIntent
import androidx.annotation.DrawableRes
import com.arnyminerz.escalaralcoiaicomtat.core.data.TranslatableString

data class NotificationButton(
    @DrawableRes val icon: Int,
    val text: TranslatableString,
    val clickListener: PendingIntent
)
