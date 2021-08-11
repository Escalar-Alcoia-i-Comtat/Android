package com.arnyminerz.escalaralcoiaicomtat

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.arnyminerz.escalaralcoiaicomtat.core.notification.ALERT_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOADS_NOTIFICATION_CHANNEL_GROUP
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.PEOPLE_NOTIFICATION_CHANNEL_GROUP
import com.arnyminerz.escalaralcoiaicomtat.core.notification.TASK_COMPLETED_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.UPDATE_AVAILABLE_CHANNEL_ID
import com.google.firebase.perf.metrics.AddTrace

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
@AddTrace(name = "createNotificationChannels")
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
    notificationManager.createNotificationChannel(createTaskCompletedChannel())
    notificationManager.createNotificationChannel(createNewUpdateChannel())
}
