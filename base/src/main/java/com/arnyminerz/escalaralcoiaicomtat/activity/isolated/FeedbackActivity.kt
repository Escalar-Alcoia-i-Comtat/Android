package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.toast

class FeedbackActivity : LanguageAppCompatActivity() {
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

                // TODO: Send messages
                toast(R.string.toast_message_sent)
                finish()
            }
        }
    }
}
