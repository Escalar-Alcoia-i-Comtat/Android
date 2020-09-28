package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.BUNDLE_EXTRA_COMPLETED_PATHS
import com.arnyminerz.escalaralcoiaicomtat.activity.BUNDLE_EXTRA_USER
import com.arnyminerz.escalaralcoiaicomtat.activity.CompletedPathActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.CompletedPath
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter.CompletedPathPair.Companion.generatePathsList
import com.arnyminerz.escalaralcoiaicomtat.list.holder.ProfilePathViewHolder
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip

@ExperimentalUnsignedTypes
class ProfilePathAdapter(private val context: Context, private val user: UserData, completedPaths: List<CompletedPath>): RecyclerView.Adapter<ProfilePathViewHolder>() {
    private val paths = generatePathsList(completedPaths)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePathViewHolder =
        ProfilePathViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.list_item_profile_path, parent, false
            )
        )

    override fun getItemCount(): Int = paths.size

    override fun onBindViewHolder(holder: ProfilePathViewHolder, position: Int) {
        val keys = paths.keys.toList()
        val path = paths[keys[position]]!!
        val info = path.info.first()

        with(holder){
            textView.text = path.path.displayName
            textView.setOnClickListener {
                context.startActivity(Intent(context, CompletedPathActivity::class.java).apply {
                    putExtra(BUNDLE_EXTRA_COMPLETED_PATHS, path)
                    putExtra(BUNDLE_EXTRA_USER, user)
                })
            }

            gradeTextView.text = path.path.grade.getSpannable(context, 1)

            attemptsTextView.text = info.attempts.toString()
            hangsTextView.text = info.hangs.toString()

            attemptsTextView.setOnClickListener {
                Tooltip.Builder(context)
                    .anchor(attemptsTextView, 0, 0, true)
                    .text(R.string.tooltip_attempts)
                    .floatingAnimation(Tooltip.Animation.DEFAULT)
                    .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                    .overlay(true)
                    .create()
                    .show(attemptsTextView, Tooltip.Gravity.TOP)
            }
            hangsTextView.setOnClickListener {
                Tooltip.Builder(context)
                .anchor(hangsTextView, 0, 0, true)
                .text(R.string.tooltip_hangs)
                .floatingAnimation(Tooltip.Animation.DEFAULT)
                .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                .overlay(true)
                .create()
                    .show(hangsTextView, Tooltip.Gravity.TOP)
            }

            shareButton.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.message_share_completed_path).format(user.username, path.path.displayName, info.attempts, info.hangs))
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }


        }
    }
}