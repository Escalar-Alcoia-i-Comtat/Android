package com.arnyminerz.escalaralcoiaicomtat.intent

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.auth.api.signin.GoogleSignInClient

/**
 * Requests Google Play API to authorise the user.
 * @author Arnau Mora
 * @since 20210519
 */
class GoogleLoginRequestContract : ActivityResultContract<GoogleSignInClient, Intent>() {
    override fun createIntent(context: Context, client: GoogleSignInClient): Intent =
        client.signInIntent

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? = intent
}

