package com.arnyminerz.escalaralcoiaicomtat.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.arnyminerz.escalaralcoiaicomtat.R

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createFriendRequestsChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_friend_request_name)
    val description = getString(R.string.notification_channel_friend_request_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(FRIEND_REQUEST_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = PEOPLE_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createFriendRequestAcceptedChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_friend_accepted_name)
    val description = getString(R.string.notification_channel_friend_accepted_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(FRIEND_REQUEST_ACCEPTED_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = PEOPLE_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createFriendRemovedChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_friend_removed_name)
    val description = getString(R.string.notification_channel_friend_removed_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(FRIEND_REMOVED_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = PEOPLE_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createAlertsChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_alert_name)
    val description = getString(R.string.notification_channel_alert_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(ALERT_CHANNEL_ID, name, importance)
    channel.description = description

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createTaskCompletedChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_task_completed_name)
    val description = getString(R.string.notification_channel_task_completed_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(TASK_COMPLETED_CHANNEL_ID, name, importance)
    channel.description = description

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createDownloadProgressChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_download_progress_name)
    val description = getString(R.string.notification_channel_download_progress_desc)
    val importance = NotificationManager.IMPORTANCE_LOW

    val channel = NotificationChannel(DOWNLOAD_PROGRESS_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = DOWNLOADS_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createDownloadCompleteChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_download_complete_name)
    val description = getString(R.string.notification_channel_download_complete_desc)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val channel = NotificationChannel(DOWNLOAD_COMPLETE_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = DOWNLOADS_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createBetaUpdateChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_beta_update_available_name)
    val description = getString(R.string.notification_channel_beta_update_available_desc)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(BETA_UPDATE_CHANNEL_ID, name, importance)
    channel.description = description

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createUserInteractedChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_user_interacted_name)
    val description = getString(R.string.notification_channel_user_interacted_desc)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val channel = NotificationChannel(USER_INTERACTED_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = PEOPLE_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
private fun Context.createNewUpdateChannel(): NotificationChannel {
    val name = getString(R.string.notification_channel_new_update_name)
    val description = getString(R.string.notification_channel_new_update_desc)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val channel = NotificationChannel(UPDATE_AVAILABLE_CHANNEL_ID, name, importance)
    channel.description = description
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        channel.group = DOWNLOADS_NOTIFICATION_CHANNEL_GROUP

    return channel
}

@TargetApi(Build.VERSION_CODES.O)
fun Context.createNotificationChannels() {
    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val downloadsGroupName = getString(R.string.notification_channel_downloads_group_name)
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                DOWNLOADS_NOTIFICATION_CHANNEL_GROUP, downloadsGroupName
            )
        )

        val peopleGroupName = getString(R.string.notification_channel_people_group_name)
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                PEOPLE_NOTIFICATION_CHANNEL_GROUP, peopleGroupName
            )
        )
    }

    notificationManager.createNotificationChannel(createDownloadProgressChannel())
    notificationManager.createNotificationChannel(createDownloadCompleteChannel())
    notificationManager.createNotificationChannel(createAlertsChannel())
    notificationManager.createNotificationChannel(createFriendRequestsChannel())
    notificationManager.createNotificationChannel(createFriendRequestAcceptedChannel())
    notificationManager.createNotificationChannel(createFriendRemovedChannel())
    notificationManager.createNotificationChannel(createTaskCompletedChannel())
    notificationManager.createNotificationChannel(createBetaUpdateChannel())
    notificationManager.createNotificationChannel(createUserInteractedChannel())
    notificationManager.createNotificationChannel(createNewUpdateChannel())
}