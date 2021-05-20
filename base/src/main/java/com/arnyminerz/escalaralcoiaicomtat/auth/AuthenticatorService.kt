package com.arnyminerz.escalaralcoiaicomtat.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        val authenticator = AccountAuthenticator(this)
        return authenticator.iBinder
    }
}