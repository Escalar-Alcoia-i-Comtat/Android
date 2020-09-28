package com.arnyminerz.escalaralcoiaicomtat.firebase

import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.notification.*

enum class FirebaseMessageNotificationType(
    val id: String,
    @StringRes val title: Int,
    @StringRes val message: Int,
    val channel: String
) {
    NOTIFICATION_TYPE_NEW_FRIEND_REQUEST(
        "new_friend_request",
        R.string.notification_friend_request_title,
        R.string.notification_friend_request_message,
        FRIEND_REQUEST_CHANNEL_ID
    ),
    NOTIFICATION_TYPE_FRIEND_REQUEST_ACCEPTED(
        "friend_request_accepted",
        R.string.notification_friend_request_accepted_title,
        R.string.notification_friend_request_accepted_message,
        FRIEND_REQUEST_ACCEPTED_CHANNEL_ID
    ),
    NOTIFICATION_TYPE_FRIEND_REMOVED(
        "friend_removed",
        R.string.notification_friend_removed_title,
        R.string.notification_friend_removed_message,
        FRIEND_REMOVED_CHANNEL_ID
    ),
    NOTIFICATION_TYPE_NEW_UPDATE_BETA(
        "new_update_beta",
        R.string.notification_new_update_title,
        R.string.notification_new_update_beta_message,
        BETA_UPDATE_CHANNEL_ID
    ),
    NOTIFICATION_TYPE_USER_LIKED(
        "user_liked",
        R.string.notification_user_liked_title,
        R.string.notification_user_liked_message,
        USER_INTERACTED_CHANNEL_ID
    ),
    NOTIFICATION_NEW_UPDATE(
        "new_update",
        R.string.notification_update_available_title,
        R.string.notification_update_available_message,
        UPDATE_AVAILABLE_CHANNEL_ID
    ),
    NOTIFICATION_UPDATING_PROGRESS(
        "updating",
        R.string.notification_update_available_title,
        R.string.notification_update_available_message,
        DOWNLOAD_PROGRESS_CHANNEL_ID
    );
}