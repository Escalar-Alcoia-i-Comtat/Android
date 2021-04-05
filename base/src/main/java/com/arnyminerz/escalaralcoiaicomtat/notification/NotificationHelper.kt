@file:Suppress("unused")

package com.arnyminerz.escalaralcoiaicomtat.notification

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.collection.arrayMapOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arnyminerz.escalaralcoiaicomtat.exception.notification.NullChannelIdException
import com.arnyminerz.escalaralcoiaicomtat.exception.notification.NullIconException
import com.arnyminerz.escalaralcoiaicomtat.generic.ValueMax
import com.mapbox.mapboxsdk.plugins.offline.model.NotificationOptions

private fun generateNotificationId(): Int {
    var greatest = 0
    for (id in builders.keys) {
        if (id > greatest)
            greatest = id
    }
    return greatest + 1
}

private val builders = arrayMapOf<Int, Notification.Builder>()

class Notification private constructor(private val builder: Builder) {
    /**
     * Returns [android.app.Notification] if the [show] method has been ran, or null if the notification
     * is not ready.
     * @author Arnau Mora
     * @since 20210406
     */
    var notification: android.app.Notification? = null
        private set

    /**
     * Returns the builder for this notification, for editing its contents.
     * @author Arnau Mora
     * @since 20210323
     * @return The [Notification.Builder] for this notification.
     * @see Builder
     */
    fun edit(): Builder = builder

    /**
     * Shows the notification.
     * @author Arnau Mora
     * @since 20210323
     * @return The current instance, for chaining functions
     */
    fun show(): Notification {
        with(builder) {
            val notificationBuilder = NotificationCompat.Builder(context, channelId!!)
            notificationBuilder.setSmallIcon(icon!!)
            if (title != null)
                notificationBuilder.setContentTitle(title)
            if (text != null)
                notificationBuilder.setContentText(text)
            if (info != null)
                notificationBuilder.setContentInfo(info)
            if (longText != null)
                notificationBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(longText)
                )
            else if (text != null)
                notificationBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(text)
                )

            intent?.let { notificationBuilder.setContentIntent(it) }
            progress?.let { value, max ->
                if (value < 0)
                    notificationBuilder.setProgress(0, 0, true)
                else
                    notificationBuilder.setProgress(value, max, false)
            }
            notificationBuilder.setOngoing(persistent)

            for (action in actions)
                notificationBuilder.addAction(
                    action.icon,
                    action.text.toString(),
                    action.clickListener
                )

            notification = notificationBuilder.build()

            NotificationManagerCompat.from(builder.context)
                .notify(builder.id, notification!!)
        }
        return this
    }

    /**
     * Hides the notification
     * @author Arnau Mora
     * @since 20210313
     * @return The current instance, for chaining functions
     */
    fun hide(): Notification {
        NotificationManagerCompat.from(builder.context)
            .cancel(builder.id)
        notification = null
        return this
    }

    /**
     * Removes the current instance from the notifications registry. It won't be able to be
     * fetched from its id.
     * @author Arnau Mora
     * @since 20210313
     */
    fun destroy() {
        hide()
        builders.remove(builder.id)
    }

    class Builder
    /**
     * Creates a new instance of the Builder class.
     * @author Arnau Mora
     * @since 20210323
     * @param context The context to create the notification from
     * @param id The id of the notification, or if not set, a new id will be generated.
     * @see generateNotificationId
     * @see Context
     */
    constructor(val context: Context, var id: Int = generateNotificationId()) {
        var channelId: String? = null

        @DrawableRes
        var icon: Int? = null
        var title: String? = null
        var text: String? = null
        var info: String? = null
        var longText: String? = null
        var persistent: Boolean = false
        var intent: PendingIntent? = null
        var progress: ValueMax<Int>? = null
        val actions: ArrayList<NotificationButton> = arrayListOf()

        /**
         * Sets an id to the notification
         * @author Arnau Mora
         * @since 20210313
         * @param id The id to set
         * @return The Builder instance
         * @throws IllegalStateException If trying to set an already existing id
         */
        @Throws(IllegalStateException::class)
        fun withId(id: Int): Builder {
            if (builders.containsKey(id))
                throw IllegalStateException("The specified id is already registered")
            this.id = id
            return this
        }

        /**
         * Sets a the channel id of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param channelId The channel id of the notification
         * @return The Builder instance
         */
        fun withChannelId(channelId: String): Builder {
            this.channelId = channelId
            return this
        }

        /**
         * Sets a the icon of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param icon The drawable icon id of the notification
         * @return The Builder instance
         */
        fun withIcon(@DrawableRes icon: Int): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets the title of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param title The title of the notification
         * @return The Builder instance
         */
        fun withTitle(title: String): Builder {
            this.title = title
            return this
        }

        /**
         * Sets the title of the notification from a resource
         * @author Arnau Mora
         * @since 20210313
         * @param titleRes The resource string of the title of the notification
         * @param arguments Arguments for filling the title
         * @return The Builder instance
         */
        fun withTitle(@StringRes titleRes: Int, vararg arguments: Any): Builder =
            withTitle(context.getString(titleRes, arguments))

        /**
         * Sets the message of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param text The message of the notification
         * @return The Builder instance
         */
        fun withText(text: String): Builder {
            this.text = text
            return this
        }

        /**
         * Sets the message of the notification from a resource
         * @author Arnau Mora
         * @since 20210313
         * @param messageRes The resource string of the message of the notification
         * @param arguments Arguments for filling the message
         * @return The Builder instance
         */
        fun withText(@StringRes messageRes: Int, vararg arguments: Any): Builder =
            withText(context.getString(messageRes, arguments))

        /**
         * Sets the info message of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param info The info message of the notification
         * @return The Builder instance
         */
        fun withInfoText(info: String): Builder {
            this.info = info
            return this
        }

        /**
         * Sets the info text of the notification from a resource
         * @author Arnau Mora
         * @since 20210313
         * @param textRes The resource string of the info text of the notification
         * @param arguments Arguments for filling the info text
         * @return The Builder instance
         */
        fun withInfoText(@StringRes textRes: Int, vararg arguments: Any): Builder =
            withInfoText(context.getString(textRes, arguments))

        /**
         * Sets the text of the notification when it's expanded
         * @author Arnau Mora
         * @since 20210313
         * @param longText The text of the notification when expanded
         * @return The Builder instance
         */
        fun withLongText(longText: String): Builder {
            this.longText = longText
            return this
        }

        /**
         * Sets the action of the notification
         * @author Arnau Mora
         * @since 20210313
         * @param pendingIntent What should be called when the notification is tapped
         * @return The Builder instance
         */
        fun withIntent(pendingIntent: PendingIntent?): Builder {
            this.intent = pendingIntent
            return this
        }

        /**
         * Sets the progress of the notification.
         * If progress is set to less than 0, it will be indeterminate
         * @author Arnau Mora
         * @since 20210313
         * @param progress The progress of the notification
         * @return The Builder instance
         */
        fun withProgress(progress: ValueMax<Int>, persistent: Boolean = true): Builder {
            this.progress = progress
            setPersistent(persistent)
            return this
        }

        /**
         * Sets the progress of the notification.
         * If progress is set to less than 0, it will be indeterminate
         * @author Arnau Mora
         * @since 20210313
         * @param progress The current progress
         * @param max The maximum progress
         * @return The Builder instance
         * @throws IllegalStateException When progress is greater than max
         */
        @Throws(IllegalStateException::class)
        fun withProgress(progress: Int, max: Int, persistent: Boolean = true): Builder =
            withProgress(ValueMax(progress, max), persistent)

        /**
         * Adds a button to the notification
         * @author Arnau Mora
         * @since 20210313
         * @param button The button to add
         * @return The Builder instance
         */
        fun addAction(button: NotificationButton): Builder {
            actions.add(button)
            return this
        }

        /**
         * Adds multiple buttons to the notification
         * @author Arnau Mora
         * @since 20210313
         * @param buttons The buttons to add
         * @return The Builder instance
         */
        fun addActions(vararg buttons: NotificationButton): Builder {
            actions.addAll(buttons)
            return this
        }

        /**
         * Adds multiple buttons to the notification
         * @author Arnau Mora
         * @since 20210313
         * @param buttons The buttons to add
         * @return The Builder instance
         */
        fun addActions(buttons: Collection<NotificationButton>): Builder {
            actions.addAll(buttons)
            return this
        }

        /**
         * Sets if the notification should be removable by the user
         * @author Arnau Mora
         * @since 20210313
         * @param persistent If the notification should be non-removable by the user
         * @return The Builder instance
         */
        fun setPersistent(persistent: Boolean = true): Builder {
            this.persistent = persistent
            return this
        }

        /**
         * Builds the notification
         * @author Arnau Mora
         * @since 20210313
         * @return The built notification
         * @throws IllegalStateException If the notification id already exists
         * @throws NullChannelIdException If the channel id is null
         * @throws NullIconException If the icon has not been specified
         */
        @Throws(
            IllegalStateException::class,
            NullChannelIdException::class,
            NullIconException::class
        )
        fun build(): Notification {
            var exception: Exception? = null
            if (channelId == null)
                exception = NullChannelIdException()
            if (icon == null)
                exception = NullIconException("The icon has not been set")
            if (builders.containsKey(id))
                exception =
                    IllegalStateException("The specified notification id is already registered")

            if (exception != null)
                throw exception

            return Notification(this)
        }

        /**
         * Builds the notification, and calls the show method.
         * @author Arnau Mora
         * @since 20210313
         * @return The built notification
         * @throws IllegalStateException If the notification id already exists
         * @throws NullChannelIdException If the channel id is null
         * @throws NullIconException If the icon has not been specified
         */
        @Throws(
            IllegalStateException::class,
            NullChannelIdException::class,
            NullIconException::class
        )
        fun buildAndShow(): Notification =
            build().show()

        val notificationOptions: NotificationOptions
            get() {
                val builder = NotificationOptions.builder(context)
                if (icon != null)
                    builder.smallIconRes(icon!!)
                if (title != null)
                    builder.contentTitle(title)
                if (text != null)
                    builder.contentText(text)
                return builder.build()
            }
    }
}
