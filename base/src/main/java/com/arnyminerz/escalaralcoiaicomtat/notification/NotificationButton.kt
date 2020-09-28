package com.arnyminerz.escalaralcoiaicomtat.notification

import android.app.PendingIntent
import androidx.annotation.DrawableRes
import com.arnyminerz.escalaralcoiaicomtat.generic.TranslatableString

data class NotificationButton(@DrawableRes val icon: Int, val text: TranslatableString, val clickListener: PendingIntent)