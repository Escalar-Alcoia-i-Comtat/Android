package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityEmailConfirmationBinding

class EmailConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}