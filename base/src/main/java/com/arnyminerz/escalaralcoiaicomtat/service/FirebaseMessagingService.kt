package com.arnyminerz.escalaralcoiaicomtat.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.*
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.firebase.FirebaseMessageNotificationType.*
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.AUTOMATIC_DATA_UPDATE_PREF
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_ALERT_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getJSONArrayOrEmpty
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotEmpty
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider.NetworkState.Companion.CONNECTED_NO_WIFI
import com.arnyminerz.escalaralcoiaicomtat.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.updateNotification
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.runOnUiThread
import org.json.JSONObject
import timber.log.Timber

var FIREBASE_MESSAGING_TOKEN: String? = null
    private set

@ExperimentalUnsignedTypes
class FirebaseMessagingService : FirebaseMessagingService() {
    init {
        Timber.d("Initializing Firebase Messaging...")
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w(task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log and toast
                Timber.d("  Firebase Messaging Token: $token")
                FIREBASE_MESSAGING_TOKEN = token
            })
    }

    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        FIREBASE_MESSAGING_TOKEN = token
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty())
            Timber.d("Message data payload: %s", remoteMessage.data)

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: %s", it.body)
        }

        val notification = remoteMessage.notification
        val data = remoteMessage.data

        if (notification != null && notification.title != null) {
            if (notification.title!!.startsWith("*"))
                when (notification.title!!.substring(1)) {
                    NOTIFICATION_TYPE_NEW_FRIEND_REQUEST.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val fromUid = data["from_uid"]
                            if (fromUid != null) GlobalScope.launch {
                                try {
                                    val user = UserData.fromUID(CONNECTED_NO_WIFI, fromUid)
                                    runOnUiThread {
                                        Timber.v("Showing friend request notification")
                                        val intent = Intent(
                                            this@FirebaseMessagingService,
                                            ProfileActivity::class.java
                                        ).putExtra(
                                            ProfileActivity.BUNDLE_EXTRA_USER_UID,
                                            fromUid
                                        ).putExtra(
                                            ProfileActivity.BUNDLE_EXTRA_ADDING_FRIEND,
                                            true
                                        )
                                        updateNotification(
                                            applicationContext,
                                            NOTIFICATION_TYPE_NEW_FRIEND_REQUEST,
                                            intent,
                                            user.username
                                        )
                                    }
                                } catch (error: Exception) {
                                    Timber.e(
                                        error,
                                        "Could not find user with uid=\"%s\"",
                                        fromUid
                                    )
                                }
                            } else
                                Timber.e("Received friend data does not contain \"from_uid\"")
                        } else
                            Timber.e("Received friend request without data")

                    NOTIFICATION_TYPE_FRIEND_REQUEST_ACCEPTED.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val userUid = data["user_uid"]
                            if (userUid != null) GlobalScope.launch {
                                try {
                                    val user = UserData.fromUID(CONNECTED_NO_WIFI, userUid)
                                    runOnUiThread {
                                        Timber.v("Showing friend request accepted notification")
                                        updateNotification(
                                            applicationContext,
                                            NOTIFICATION_TYPE_FRIEND_REQUEST_ACCEPTED,
                                            null,
                                            user.username
                                        )
                                    }
                                } catch (error: Exception) {
                                    Timber.e(
                                        error,
                                        "Could not find user with uid=\"%s\"",
                                        userUid
                                    )
                                }
                            } else
                                Timber.e("Received friend data does not contain \"user_uid\"")
                        } else
                            Timber.e("Received friend request without data")

                    NOTIFICATION_TYPE_FRIEND_REMOVED.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val userUid = data["user_uid"]
                            if (userUid != null) GlobalScope.launch {
                                try {
                                    val user = UserData.fromUID(CONNECTED_NO_WIFI, userUid)
                                    Timber.v("Showing friend removed notification")
                                    updateNotification(
                                        applicationContext,
                                        NOTIFICATION_TYPE_FRIEND_REMOVED,
                                        null,
                                        user.username
                                    )
                                } catch (error: Exception) {
                                    Timber.e(
                                        error,
                                        "Could not find user with uid=\"%s\"",
                                        userUid
                                    )
                                }
                            } else
                                Timber.e("Received friend data does not contain \"user_uid\"")
                        } else
                            Timber.e("Received friend request without data")

                    NOTIFICATION_TYPE_NEW_UPDATE_BETA.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val downloadUrl = data["url"]
                            if (downloadUrl != null) {
                                Timber.v("Received new update notification")
                                updateNotification(
                                    applicationContext,
                                    NOTIFICATION_TYPE_NEW_UPDATE_BETA,
                                    Intent(
                                        this,
                                        UpdatingActivity::class.java
                                    ).putExtra(
                                        "apkUrl",
                                        downloadUrl
                                    )
                                )
                            } else
                                Timber.e("Received new update data does not contain \"url\"")
                        } else
                            Timber.e("Received new update request without data")

                    NOTIFICATION_TYPE_USER_LIKED.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val userUid = data["user_uid"]
                            val pathName = data["path_name"]
                            if (userUid != null && pathName != null) GlobalScope.launch {
                                try {
                                    val user = UserData.fromUID(CONNECTED_NO_WIFI, userUid)

                                    Timber.v("Showing user liked notification")
                                    updateNotification(
                                        applicationContext,
                                        NOTIFICATION_TYPE_USER_LIKED,
                                        null,
                                        user.username,
                                        pathName
                                    )
                                } catch (error: Exception) {
                                    Timber.e(
                                        error,
                                        "Could not find user with uid=\"%s\"",
                                        userUid
                                    )
                                }
                            } else
                                Timber.e("Received friend data does not contain \"user_uid\" nor \"path_name\"")
                        } else
                            Timber.e("Received friend request without data")

                    NOTIFICATION_NEW_UPDATE.id ->
                        if (remoteMessage.data.isNotEmpty()) {
                            val toUpdate = data["update_request"]
                            if (toUpdate != null) {
                                Timber.v("Received new data update notification. Parsing the contents...")
                                val json = JSONObject(toUpdate)
                                val areas = json.getJSONArrayOrEmpty("areas")
                                val zones = json.getJSONArrayOrEmpty("zones")
                                val sectors = json.getJSONArrayOrEmpty("sectors")

                                if (AUTOMATIC_DATA_UPDATE_PREF.get(sharedPreferences)) {
                                    // No notification should be shown, update silently
                                    Timber.v("  Won't show notification, the content will be updated silently.")

                                    updateNotification(
                                        applicationContext,
                                        DOWNLOAD_PROGRESS_CHANNEL_ID,
                                        R.drawable.ic_notifications,
                                        R.string.notification_updating_title,
                                        R.string.notification_updating_message,
                                        progress = ValueMax(0, 0)
                                    )

                                    for (a in 0 until areas.length()) {
                                        val areaId = areas.getInt(a)
                                    }
                                } else {
                                    Timber.v("  Showing notification, update requires user confirmation.")
                                    val intent = Intent(this, UpdatingActivity::class.java)

                                    if (areas.isNotEmpty()) {
                                        // TODO: Implement more areas support
                                        intent.putExtra(UPDATE_AREA, areas.getInt(0))
                                    }
                                    if (zones.isNotEmpty()) {
                                        // TODO: Implement more zones support
                                        intent.putExtra(UPDATE_ZONE, zones.getInt(0))
                                    }
                                    if (sectors.isNotEmpty()) {
                                        // TODO: Implement more sectors support
                                        intent.putExtra(UPDATE_SECTOR, sectors.getInt(0))
                                    }

                                    updateNotification(
                                        applicationContext,
                                        NOTIFICATION_NEW_UPDATE,
                                        intent
                                    )
                                }
                            } else
                                Timber.e("Received new update data does not contain \"url\"")
                        } else
                            Timber.e("Received new update request without data")
                }
            else if (data.containsKey("area_update") || data.containsKey("zone_update") ||
                data.containsKey("sector_update") || data.containsKey("path_update")
            ) if (SETTINGS_ALERT_PREF.get(sharedPreferences!!))
                GlobalScope.launch {
                    val dataClass = when {
                        data.containsKey("area_update") -> data["area_update"]?.toIntOrNull()
                            ?.let { Area.fromId(it) }
                        data.containsKey("zone_update") -> data["zone_update"]?.toIntOrNull()
                            ?.let { Zone.fromId(it) }
                        data.containsKey("sector_update") -> data["sector_update"]?.toIntOrNull()
                            ?.let { Sector.fromId(it) }
                        else -> null
                    }

                    val path = if (dataClass.isNull() && data.containsKey("path_update"))
                        data["path_update"]?.toIntOrNull()?.let { Path.fromId(it) }
                    else null

                    val name = dataClass?.displayName ?: path?.displayName ?: return@launch

                    updateNotification(
                        applicationContext,
                        ALERT_CHANNEL_ID,
                        R.drawable.ic_notifications,
                        when {
                            data.containsKey("area_update") -> getString(R.string.notification_updated_area_title)
                            data.containsKey("zone_update") -> getString(R.string.notification_updated_zone_title)
                            data.containsKey("sector_update") -> getString(R.string.notification_updated_sector_title)
                            data.containsKey("path_update") -> getString(R.string.notification_updated_path_title)
                            else -> notification.title
                                ?: getString(R.string.notification_new) // This will never happen but, who knows ¯\_(ツ)_/¯
                        },
                        getString(R.string.notification_updated_message, name),
                        if (data.containsKey("url"))
                            PendingIntent.getActivity(
                                applicationContext, 0, Intent(
                                    Intent.ACTION_VIEW, Uri.parse(
                                        data["url"]
                                    )
                                ), 0
                            )
                        else null
                    )
                } else
                Timber.e("Received notification but alerts are disabled")
            else
                updateNotification(
                    applicationContext,
                    ALERT_CHANNEL_ID,
                    R.drawable.ic_notifications,
                    notification.title!!,
                    notification.body!!,
                    if (remoteMessage.data.containsKey("url"))
                        PendingIntent.getActivity(
                            applicationContext, 0, Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    remoteMessage.data["url"]
                                )
                            ), 0
                        )
                    else null
                )
        } else
            Timber.e("Some parameters are missing")
    }
}