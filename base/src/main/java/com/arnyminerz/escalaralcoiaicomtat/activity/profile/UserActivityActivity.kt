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
    companion object {
        /**
         * Returns the [Intent] with all the extras included for launching directly.
         * @author Arnau Mora
         * @since 20210821
         * @param context The context where it's launching from.
         * @param completions The list of [MarkedDataInt] to display.
         * @param completionsCount The amount of items from [completions] which are completions and not projects.
         */
        fun intent(
            context: Context,
            completions: ArrayList<MarkedDataInt>,
            completionsCount: Int,
            projectsCount: Int
        ): Intent =
            Intent(context, UserActivityActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_COMPLETIONS.key, completions)
                putExtra(EXTRA_COMPLETIONS_COUNT, completionsCount)
                putExtra(EXTRA_PROJECTS_COUNT, projectsCount)
            }
    }

    private lateinit var binding: ActivityUserActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val completionsCount = intent.getExtra(EXTRA_COMPLETIONS_COUNT)
        val projectsCount = intent.getExtra(EXTRA_PROJECTS_COUNT)
        val completions = intent.getParcelableArrayListExtra<MarkedDataInt>(EXTRA_COMPLETIONS.key)
            ?: return run {
                Timber.e("${EXTRA_COMPLETIONS.key} extra not found in intent.")
                onBackPressed()
                finish()
            }

        binding.backFab.setOnClickListener { onBackPressed() }
    }
}