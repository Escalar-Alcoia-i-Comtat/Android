package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.content.Intent
import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityServerDownBinding

@ExperimentalUnsignedTypes
class ServerDownActivity : LanguageAppCompatActivity() {
    private lateinit var binding: ActivityServerDownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerDownBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tryAgainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.sendFeedbackButton.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}
