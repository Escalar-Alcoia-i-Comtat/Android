package com.arnyminerz.escalaralcoiaicomtat.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.exception.MissingDataException
import com.arnyminerz.escalaralcoiaicomtat.generic.ValueMax

private val notificationIds = arrayListOf<Int>()

private fun generateNotificationId(): Int {
    var greatest = 0
    for (id in notificationIds) {
        if (id > greatest)
            greatest = id
    }
    return greatest + 1
}

class NotificationBuilder(
    private val context: Context,
    private val channelId: String
) {
    var id: Int = generateNotificationId()
        private set

    private var title: String = ""
    private var message: String = ""
    private var longText: String = ""

    private var intent: PendingIntent? = null
    private var deleteIntent: PendingIntent? = null

    private var progressBarIndeterminate: Boolean? = null
    private var progressBarValue: Int? = null
    private var progressBarMax: Int? = null

    private val actions: ArrayList<NotificationButton> = arrayListOf()

    @DrawableRes
    private var icon: Int = R.drawable.ic_notifications

    fun setTitle(title: String): NotificationBuilder {
        this.title = title
        return this
    }

    fun setTitle(@StringRes title: Int): NotificationBuilder = setTitle(context.getString(title))

    fun setMessage(message: String): NotificationBuilder {
        this.message = message
        return this
    }

    fun setMessage(@StringRes message: Int): NotificationBuilder =
        setMessage(context.getString(message))

    fun setLongText(longText: String): NotificationBuilder {
        this.longText = longText
        return this
    }

    fun setLongText(@StringRes longText: Int): NotificationBuilder =
        setLongText(context.getString(longText))

    fun setIcon(@DrawableRes icon: Int): NotificationBuilder {
        this.icon = icon
        return this
    }

    fun setIntent(pendingIntent: PendingIntent): NotificationBuilder {
        this.intent = pendingIntent
        return this
    }

    fun setIntent(intent: Intent, requestCode: Int = 0, flags: Int = 0): NotificationBuilder {
        this.intent = PendingIntent.getActivity(context, requestCode, intent, flags)
        return this
    }

    fun setDeleteIntent(pendingDeleteIntent: PendingIntent): NotificationBuilder {
        this.deleteIntent = pendingDeleteIntent
        return this
    }

    fun setDeleteIntent(intent: Intent, requestCode: Int = 0, flags: Int = 0): NotificationBuilder {
        this.deleteIntent = PendingIntent.getActivity(context, requestCode, intent, flags)
        return this
    }

    fun showProgressBar(): NotificationBuilder {
        this.progressBarIndeterminate = true
        return this
    }

    fun hideProgressBar(): NotificationBuilder {
        this.progressBarIndeterminate = null
        return this
    }

    fun setProgress(value: Int, max: Int): NotificationBuilder {
        this.progressBarValue = value
        this.progressBarMax = max
        this.progressBarIndeterminate = false
        return this
    }

    fun setProgress(valueMax: ValueMax<Int>): NotificationBuilder {
        this.progressBarValue = valueMax.value
        this.progressBarMax = valueMax.max
        this.progressBarIndeterminate = false
        return this
    }

    fun addActions(vararg actions: NotificationButton): NotificationBuilder {
        this.actions.addAll(actions)
        return this
    }

    fun show() {
        if (title.isEmpty())
            throw MissingDataException("Title has not been set. Tip: use setTitle")
        if (message.isEmpty())
            throw MissingDataException("Message has not been set. Tip: use setMessage")

        if (longText.isEmpty())
            longText = message

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(longText)
                    .bigText(longText)
            )

        intent?.let { builder.setContentIntent(it) }
        deleteIntent?.let { builder.setDeleteIntent(it) }

        if (progressBarIndeterminate != null)
            if (progressBarIndeterminate!!)
                builder.setProgress(0, 0, true)
            else if (progressBarMax != null && progressBarValue != null)
                builder.setProgress(progressBarMax!!, progressBarValue!!, false)
                    .setOnlyAlertOnce(true) // Only alert once since progress may be updated

        for (action in actions)
            builder.addAction(action.icon, action.text.toString(), action.clickListener)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
}