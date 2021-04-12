package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

class FeedbackActivity : LanguageAppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.messageEditText.addTextChangedListener { clearErrors(binding.messageLayout) }
        binding.messageEditText.setOnFocusChangeListener { _, _ -> clearErrors(binding.messageLayout) }

        binding.sendFab.setOnClickListener {
            with(binding) {
                val name = nameEditText.text.toString()
                val email = emailEditText.text.toString()
                val message = messageEditText.text.toString()

                if (message.isEmpty()) {
                    messageLayout.isErrorEnabled = true
                    messageLayout.error = getString(R.string.feedback_error_empty_message)
                }

                val contactEmail = if (contactCheckBox.isChecked) email else "Don't contact"
                val fullMessage = "New message from $name ($contactEmail):\n$message"

                val crashlytics = Firebase.crashlytics
                crashlytics.log(fullMessage)
                toast(R.string.toast_message_sent)
                finish()
            }
        }
    }

    /**
     * Clear the error messages from a text field.
     * @author Arnau Mora
     * @since 20210412
     * @param layout The layout to update
     */
    private fun clearErrors(layout: TextInputLayout) {
        layout.error = null
        layout.isErrorEnabled = false
    }
}
