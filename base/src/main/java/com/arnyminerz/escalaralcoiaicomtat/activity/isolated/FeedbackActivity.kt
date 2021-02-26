package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding
import io.sentry.Sentry
import io.sentry.UserFeedback

class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.sendFab.setOnClickListener {
            with(binding) {
                val name = nameEditText.text.toString()
                val email = emailEditText.text.toString()
                val message = messageEditText.text.toString()

                val sentryId = Sentry.captureMessage(message)
                val feedback = UserFeedback(sentryId).apply {
                    this.comments = if (contactCheckBox.isChecked)
                        "Don't contact me"
                        else "I want to be contacted"
                    this.email = email
                    this.name = name
                }
                Sentry.captureUserFeedback(feedback)
            }
        }
    }
}