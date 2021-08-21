package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.UserActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityUserActivityBinding

/**
 * Serves for displaying the user the contents of a [UserActivity].
 * @author Arnau Mora
 * @since 20210821
 */
class UserActivityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backFab.setOnClickListener { onBackPressed() }
    }
}