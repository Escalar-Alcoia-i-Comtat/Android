package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityFeedbackBinding

class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}