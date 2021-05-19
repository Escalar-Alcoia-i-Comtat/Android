package com.arnyminerz.escalaralcoiaicomtat.intent

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.AuthActivity

/**
 * Requests the [AuthActivity] to ask the user to get logged in.
 * @author Arnau Mora
 * @since 20210519
 */
class LoginRequestContract : ActivityResultContract<Any?, Int>() {
    override fun createIntent(context: Context, input: Any?): Intent =
        Intent(context, AuthActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Int = resultCode
}
