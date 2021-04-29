package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMarkCompletedBinding

class MarkCompletedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkCompletedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkCompletedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
