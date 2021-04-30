package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityCommentsBinding

/**
 * Shows the user the comments that people have published when marking a path as complete or project.
 * @author Arnau Mora
 * @since 20210430
 */
class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
