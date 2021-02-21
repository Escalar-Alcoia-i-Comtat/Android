package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityServerDownBinding

@ExperimentalUnsignedTypes
class ServerDownActivity : AppCompatActivity() {
    private lateinit var binding: ActivityServerDownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerDownBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tryAgainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}