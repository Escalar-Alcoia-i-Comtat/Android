@file:Suppress("unused")

package com.arnyminerz.escalaralcoiaicomtat.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.firebase.FirebaseMessageNotificationType
import com.arnyminerz.escalaralcoiaicomtat.generic.TranslatableString
import com.arnyminerz.escalaralcoiaicomtat.generic.ValueMax
import timber.log.Timber

private val notificationIds = arrayListOf<Int>()

private fun generateNotificationId(): Int {
    var greatest = 0
    for (id in notificationIds) {
        if (id > greatest)
            greatest = id
    }
    return greatest + 1
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    title: String,
    text: String,
    longText: String,
    intent: PendingIntent? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(title),
        TranslatableString(text),
        TranslatableString(longText),
        intent,
        null,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    title: String,
    text: String,
    pendingIntent: PendingIntent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(title),
        TranslatableString(text),
        TranslatableString(text),
        pendingIntent,
        progress,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    title: String,
    text: String,
    intent: Intent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(title),
        TranslatableString(text),
        TranslatableString(text),
        PendingIntent.getActivity(
            context,
            0,
            intent,
            0
        ),
        progress,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes text: Int,
    intent: PendingIntent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(context, title),
        TranslatableString(context, text),
        TranslatableString(context, text),
        intent,
        progress,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    @StringRes title: Int,
    text: String,
    intent: PendingIntent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(context, title),
        TranslatableString(text),
        TranslatableString(text),
        intent,
        progress,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    @StringRes title: Int,
    text: TranslatableString,
    intent: PendingIntent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1
): Int {
    return updateNotification(
        context,
        channelId,
        icon,
        TranslatableString(context, title),
        text,
        text,
        intent,
        progress,
        notificationId
    )
}

fun updateNotification(
    context: Context,
    type: FirebaseMessageNotificationType,
    intent: Intent? = null,
    vararg parameters: Any
): Int {
    return updateNotification(
        context,
        type.channel,
        R.drawable.ic_notifications,
        context.getString(type.title),
        context.getString(
            type.message,
            parameters
        ),
        intent
    )
}

fun updateNotification(
    context: Context,
    channelId: String, @DrawableRes icon: Int,
    title: TranslatableString,
    text: TranslatableString,
    longText: TranslatableString,
    intent: PendingIntent? = null,
    progress: ValueMax<Int>? = null,
    notificationId: Int = -1,
    actions: MutableList<NotificationButton>? = null
): Int {
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(icon)
        .setContentTitle(title.toString())
        .setContentText(text.toString())
        .setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(longText.toString())
                .bigText(longText.toString())
        )
    if (intent != null) {
        builder.setContentIntent(intent)
            .setAutoCancel(true)
    }
    if (progress != null)
        builder.setProgress(progress.value, progress.max, false)

    actions?.let {
        for(action in it)
            builder.addAction(action.icon, action.text.toString(), action.clickListener)
    }

    val id = if (notificationId < 0) generateNotificationId() else notificationId

    with(NotificationManagerCompat.from(context)) {
        notify(id, builder.build())
    }

    Timber.d("Showing new notification. ID: %s. Title: %s. Text: %s", id, title, text)

    return id
}

fun clearNotification(context: Context, id: Int) {
    with(NotificationManagerCompat.from(context)) {
        cancel(id)
    }
}