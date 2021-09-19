package com.arnyminerz.escalaralcoiaicomtat.service

import android.annotation.SuppressLint
import android.util.Log
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ALERT_PREF
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("LogNotTimber")
class MessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "MessagingService"

        /**
         * This type is used when a new update is available.
         * @author Arnau Mora
         * @since 20210919
         */
        private const val MESSAGE_TYPE_UPDATE = "update"
    }

    init {
        Log.v(TAG, "Initialized messaging service.")
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "Got new messaging token: $token.")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (SETTINGS_ALERT_PREF.get().not()) {
            Log.w(TAG, "Will not show notification since they are disabled.")
            return
        }

        Log.v(TAG, "Received message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty())
            Log.v(TAG, "Message payload: $data")

        remoteMessage.notification?.let { notificationData ->
            val body = notificationData.body
            val title = notificationData.title
            val shouldDisplay = notificationData.sound != null

            Log.v(TAG, "Received notification: $body")

            if (shouldDisplay && title != null && body != null)
                Notification.Builder(applicationContext)
                    .withChannelId(ALERT_CHANNEL_ID)
                    .withIcon(R.drawable.ic_notifications)
                    .withTitle(title)
                    .withText(body)
                    .withColor(R.color.colorAccent)
                    .buildAndShow()
            else if (!shouldDisplay) {
                Log.v(TAG, "Got hidden notification.")
                when (val type = data["type"]) {
                    MESSAGE_TYPE_UPDATE -> {
                        Log.i(TAG, "There's a new version available.")
                    }
                    else -> Log.w(TAG, "Got invalid hidden notification. Type: $type")
                }
            } else Log.w(TAG, "Received notification without title nor body.")
        }
    }
}
