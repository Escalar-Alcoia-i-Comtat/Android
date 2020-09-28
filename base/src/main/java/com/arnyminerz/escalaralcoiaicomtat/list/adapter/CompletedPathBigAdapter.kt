package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.BUNDLE_EXTRA_COMPLETED_PATHS
import com.arnyminerz.escalaralcoiaicomtat.activity.CompletedPathActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.CompletedPath
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.Grade
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter.CompletedPathPair.Companion.generatePathsList
import com.arnyminerz.escalaralcoiaicomtat.list.holder.CompletedPathBigViewHolder
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import timber.log.Timber
import java.io.Serializable
import java.util.*

@ExperimentalUnsignedTypes
class CompletedPathBigAdapter(private val context: Context, completedPaths: ArrayList<CompletedPath>): RecyclerView.Adapter<CompletedPathBigViewHolder>() {
    data class CompletedPathInfo(val date: Date?, val attempts: Int, val hangs: Int) :
        Serializable {
        constructor(completedPath: CompletedPath) : this(
            completedPath.timestamp,
            completedPath.attempts,
            completedPath.hangs
        )
    }

    data class CompletedPathPathInfo(val id: Int, val displayName: String, val grade: Grade) :
        Serializable {
        constructor(path: Path) : this(path.id, path.displayName, path.grade())
    }

    data class CompletedPathPair(
        val registerId: Int,
        val path: CompletedPathPathInfo,
        val info: ArrayList<CompletedPathInfo>
    ) : Serializable {
        constructor(registerId: Int, path: Path, info: ArrayList<CompletedPathInfo>) : this(
            registerId,
            CompletedPathPathInfo(path),
            info
        )

        companion object {
            fun generatePathsList(completedPaths: Collection<CompletedPath>): HashMap<Int, CompletedPathPair> {
                val map = hashMapOf<Int, CompletedPathPair>()
                for (completedPath in completedPaths) {
                    val path = completedPath.path
                    val completedPathId = completedPath.id
                    val id = path.id
                    Timber.v("Iterating path #%d (%s)", id, path.displayName)
                    if (map.containsKey(id)) {
                        Timber.v("  Got a repeated path!")
                        map[id]!!.info.add(CompletedPathInfo(completedPath))
                    } else
                        map[id] =
                            CompletedPathPair(
                                completedPathId,
                                path,
                                arrayListOf(CompletedPathInfo(completedPath))
                            )
                }
                return map
            }
        }

        suspend fun likedCompletedPath(
            networkState: ConnectivityProvider.NetworkState,
            userData: UserData
        ): Boolean = userData.likedCompletedPath(networkState, this.path.id)

        suspend fun like(
            networkState: ConnectivityProvider.NetworkState,
            userData: UserData
        ): Boolean = userData.likeCompletedPath(networkState, registerId)
    }

    private val paths = generatePathsList(completedPaths)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedPathBigViewHolder =
        CompletedPathBigViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_completed_path_big, parent, false))

    override fun getItemCount(): Int = paths.size

    override fun onBindViewHolder(holder: CompletedPathBigViewHolder, position: Int) {
        val keys = paths.keys.toList()
        val path = paths[keys[position]]!!

        holder.pathNameTextView.text = path.path.displayName
        holder.pathGradeTextView.text = path.path.grade.getSpannable(context)

        holder.enterImageButton.setOnClickListener {
            Timber.v("\"%s\" has been completed %d times", path.path.displayName, path.info.size)
            context.startActivity(Intent(context, CompletedPathActivity::class.java).apply {
                putExtra(BUNDLE_EXTRA_COMPLETED_PATHS, path)
            })
        }
    }
}