package com.arnyminerz.escalaralcoiaicomtat.service

import android.app.Notification
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.utils.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.core.view.getColor
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Timber.v("Got new messaging token: $token.")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.v("Received message from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Timber.v("Message payload: $data")
        }

        remoteMessage.notification?.let { notificationData ->
            val body = notificationData.body
            val title = notificationData.title

            Timber.v("Received notification: $body")

            @Suppress("DEPRECATION")
            val builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    Notification.Builder(applicationContext, ALERT_CHANNEL_ID)
                else
                    Notification.Builder(applicationContext)
            val notification = builder.apply {
                setContentTitle(title)
                setContentText(body)
                setColor(getColor(applicationContext, R.color.colorAccent))
            }.build()
            val id = generateUUID().hashCode()

            NotificationManagerCompat.from(applicationContext)
                .notify(id, notification)
        }
    }
}
