package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.generic.getSerializable
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.activity_completed_path.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber

const val BUNDLE_EXTRA_COMPLETED_PATHS = "completed_paths"
const val BUNDLE_EXTRA_USER = "user"

@ExperimentalUnsignedTypes
class CompletedPathActivity : NetworkChangeListenerActivity() {
    private lateinit var paths: CompletedPathBigAdapter.CompletedPathPair
    private lateinit var user: UserData

    private lateinit var completedPathInfo: CompletedPathBigAdapter.CompletedPathPathInfo
    private val completedPaths = arrayListOf<CompletedPathBigAdapter.CompletedPathInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_path)

        if (intent == null) {
            Timber.e("Intent is null")
            onBackPressed()
            return
        }
        if (intent.extras == null || intent.extras!!.isEmpty) {
            Timber.e("Intent doesn't have extras")
            onBackPressed()
            return
        }

        setSupportActionBar(completedPath_toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_round_arrow_back_24)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        paths = intent.extras!!.getSerializable<CompletedPathBigAdapter.CompletedPathPair>(
            BUNDLE_EXTRA_COMPLETED_PATHS
        )
        completedPathInfo = paths.path
        val info = paths.info
        val first = info.first()
        completedPaths.addAll(info)

        like_fab.hide()
        when {
            intent.extras!!.containsKey(BUNDLE_EXTRA_USER) -> {
                user = intent.extras!!.getSerializable<UserData>(BUNDLE_EXTRA_USER)

                like_fab.setOnClickListener {
                    Timber.v("Liking...")
                    GlobalScope.launch {
                        try {
                            val liked = paths.like(networkState, user)
                            if (liked) {
                                Timber.v("Liked!")
                                refreshLikeFab()
                            } else throw Exception()
                        } catch (error: Exception) {
                            Timber.e(error, "Could not like!")
                            toast(R.string.toast_error_liked_check)
                        }
                    }
                }
                GlobalScope.launch { refreshLikeFab() }
            }
            MainActivity.loggedIn() -> GlobalScope.launch {
                val uid = MainActivity.user()!!.uid
                Timber.v("Loading like button user. UID: $uid")
                val user = UserData.fromUID(networkState, uid)
                this@CompletedPathActivity.user = user

                runOnUiThread {
                    Timber.v("Got user data!")
                    like_fab.setOnClickListener {
                        Timber.v("Liking...")
                        GlobalScope.launch {
                            try {
                                val liked = paths.like(networkState, user)
                                if (liked) {
                                    Timber.v("Liked!")
                                    refreshLikeFab()
                                } else throw Exception()
                            } catch (error: Exception) {
                                Timber.e(error, "Could not like!")
                                toast(R.string.toast_error_liked_check)
                            }
                        }
                    }
                }

                refreshLikeFab()
            }
            else -> Timber.e("User not logged in")
        }

        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.text_dark, typedValue, true)
        val textColorDark = typedValue.data

        toolbar_title.text = completedPathInfo.displayName
        times_tried_textView.text =
            getString(R.string.completed_path_activity_info_times_tried, info.size)
        difficulty_textView.text = completedPathInfo.grade.getSpannable(this)
        //evolution_textView.text = "TODO" // TODO: Evolution Analysis
        evolution_layout.hide()

        val attempts = arrayListOf<Entry>()
        val hangs = arrayListOf<Entry>()
        if (first.date != null)
            for (completedPath in info)
                if (completedPath.date != null) {
                    val x = first.date.time - completedPath.date.time
                    val y1 = completedPath.attempts
                    val y2 = completedPath.hangs
                    Timber.v("X=%d. Attempts: %d. Hangs: %d", x, y1, y2)
                    attempts.add(Entry(x.toFloat(), y1.toFloat()))
                    hangs.add(Entry(x.toFloat(), y2.toFloat()))
                }

        val attemptsDataSet =
            LineDataSet(attempts, getString(R.string.completed_path_activity_info_graph_attempts))
                .apply {
                    setColors(intArrayOf(R.color.graph_attempts), this@CompletedPathActivity)
                }
        val hangsDataSet =
            LineDataSet(hangs, getString(R.string.completed_path_activity_info_graph_hangs))
                .apply {
                    setColors(intArrayOf(R.color.graph_hangs), this@CompletedPathActivity)
                }

        completed_path_lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String = ""
            }
            this.textColor = textColorDark
        }
        completed_path_lineChart.axisLeft.textColor = textColorDark
        completed_path_lineChart.axisRight.textColor = textColorDark

        val data = LineData(attemptsDataSet, hangsDataSet)
        completed_path_lineChart.data = data
        completed_path_lineChart.legend.apply {
            textColor = textColorDark
        }
        completed_path_lineChart.description = Description().apply {
            text = ""
        }
        completed_path_lineChart.invalidate()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean =
        if (MainActivity.betaUser)
            menu?.let { menuInflater.inflate(R.menu.completed_path_menu, menu); true }
                ?: super.onCreateOptionsMenu(menu)
        else super.onCreateOptionsMenu(menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.share_button -> {
                Timber.v("Opening share editor...")
                val cp = completedPaths.last()

                startActivity(
                    Intent(
                        this@CompletedPathActivity,
                        ImageShareActivity::class.java
                    ).apply {
                        putExtra(SHARE_PATH_ID, completedPathInfo.id)
                        putExtra(SHARE_DATE, cp.date)
                        putExtra(SHARE_ATTEMPTS, cp.attempts)
                        putExtra(SHARE_HANGS, cp.hangs)
                    })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        GlobalScope.launch { refreshLikeFab() }
    }

    private suspend fun refreshLikeFab() {
        try {
            Timber.v("Refreshing like FAB...")
            val liked = paths.likedCompletedPath(networkState, user)
            runOnUiThread {
                Timber.d("User has liked? $liked")
                if (liked) // Liked
                    like_fab.setImageResource(R.drawable.ic_round_favorite)
                else
                    like_fab.setImageResource(R.drawable.ic_round_favorite_border)
                like_fab.show()
            }
        } catch (error: Exception) {
            Timber.e(error, "Could not check if path is liked.")
            runOnUiThread {
                like_fab.hide()
            }
        }
    }
}