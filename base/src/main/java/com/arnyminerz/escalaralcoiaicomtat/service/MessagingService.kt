package com.arnyminerz.escalaralcoiaicomtat.service

import com.google.firebase.messaging.FirebaseMessagingService
import timber.log.Timber

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Timber.v("Got new messaging token: $token.")
    }
}
