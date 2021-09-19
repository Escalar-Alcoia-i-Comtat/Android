package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_FEEDBACK
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * An activity for offering the user the possibility to send feedback.
 * @author Arnau Mora
 * @since 20210919
 */
class FeedbackActivity : LanguageAppCompatActivity() {
    /**
     * The view binding of the activity.
     * @author Arnau Mora
     * @since 20210919
     */
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // If EXTRA_FEEDBACK is set, update message text
        getExtra(EXTRA_FEEDBACK)?.let { feedback ->
            binding.messageEditText.setText(feedback)
        }

        // If the user is logged in, initialize the text of the name and email fields
        val auth = Firebase.auth
        auth.currentUser?.let { currentUser ->
            binding.emailEditText.setText(currentUser.email)
            binding.nameEditText.setText(currentUser.displayName)
        }

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
