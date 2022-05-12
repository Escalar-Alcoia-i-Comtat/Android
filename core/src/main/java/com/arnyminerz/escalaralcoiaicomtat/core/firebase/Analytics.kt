package com.arnyminerz.escalaralcoiaicomtat.core.firebase

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_LIST_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_LIST_NAME
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * Logs through Firebase Analytics that an item has been selected, and appends its data.
 * @author Arnau Mora
 * @since 20220512
 * @param namespace The [Namespace] of the selected item. Will be tagged as [ITEM_LIST_ID].
 * @param objectId The id of the selected item. Will be tagged as [ITEM_ID].
 * @param displayName The name that gets displayed and represents the selected item. Will be tagged
 * as [ITEM_LIST_NAME].
 */
fun logAnalyticsSelectedItem(
    namespace: Namespace,
    @ObjectId objectId: String,
    displayName: String
) {
    Timber.d("Logging selection of $namespace/[$objectId].")

    Firebase
        .analytics
        .logEvent(
            FirebaseAnalytics.Event.SELECT_ITEM,
            Bundle().apply {
                putString(ITEM_LIST_ID, namespace.namespace)
                putString(ITEM_ID, objectId)
                putString(ITEM_LIST_NAME, displayName)
            },
        )
}

/**
 * Adds a log that says that a path has been viewed, this is, pressed the unfold button.
 * @author Arnau Mora
 * @since 20220512
 * @param objectId The id of the selected item. Will be tagged as [ITEM_ID].
 * @param displayName The name that gets displayed and represents the selected item. Will be tagged
 * as [ITEM_LIST_NAME].
 */
fun logAnalyticsViewPath(
    @ObjectId objectId: String,
    displayName: String
) {
    Timber.d("Logging view of Path/[$objectId].")

    Firebase
        .analytics
        .logEvent(
            FirebaseAnalytics.Event.VIEW_ITEM_LIST,
            Bundle().apply {
                putString(ITEM_LIST_ID, Namespace.PATH.namespace)
                putString(ITEM_ID, objectId)
                putString(ITEM_LIST_NAME, displayName)
            },
        )
}
