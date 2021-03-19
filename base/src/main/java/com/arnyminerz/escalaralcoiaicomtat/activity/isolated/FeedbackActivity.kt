package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.parse.ParseAnalytics
import io.sentry.Sentry
import io.sentry.UserFeedback

class FeedbackActivity : LanguageAppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ParseAnalytics.trackAppOpenedInBackground(intent)

        binding.sendFab.setOnClickListener {
            with(binding) {
                val name = nameEditText.text.toString()
                val email = emailEditText.text.toString()
                val message = messageEditText.text.toString()

                val sentryId = Sentry.captureMessage("Message from $name")
                val feedback = UserFeedback(sentryId).apply {
                    this.comments = message + "\n" +
                            (if (contactCheckBox.isChecked)
                                "Don't contact me"
                            else "I want to be contacted") +
                            "Version Name: ${BuildConfig.VERSION_NAME}\n" +
                            "Version Code: ${BuildConfig.VERSION_CODE}\n"
                    this.email = email
                    this.name = name
                }
                Sentry.captureUserFeedback(feedback)
                toast(R.string.toast_message_sent)
                finish()
            }
        }
    }
}
