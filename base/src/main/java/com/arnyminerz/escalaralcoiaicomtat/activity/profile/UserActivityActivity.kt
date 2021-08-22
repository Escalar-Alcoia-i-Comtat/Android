package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.UserActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_COMPLETIONS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_COMPLETIONS_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PROJECTS_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityUserActivityBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import timber.log.Timber

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

        binding.summaryCompletions.text =
            getString(R.string.activity_summary_completions, completionsCount)
        binding.summaryProjects.text = getString(R.string.activity_summary_projects, projectsCount)

        val gradesTitle = resources.getString(R.string.activity_summary_grades)
        doAsync {
            val gradesCount = hashMapOf<String, Int>()
            for (completion in completions)
                completion.getPath().let { path ->
                    val grade = path.grade()
                    var count = gradesCount[grade.displayName] ?: 0
                    gradesCount[grade.displayName] = ++count
                }

            val entries = arrayListOf<BarEntry>()
            for ((i, gradeName) in gradesCount.keys.sorted().withIndex()) {
                val count = gradesCount.getValue(gradeName)
                entries.add(BarEntry(i.toFloat(), count.toFloat()))
            }

            val dataSet = BarDataSet(entries, gradesTitle)
            val barData = BarData(dataSet)
            uiContext {
                binding.chart.data = barData
            }
        }
    }
}