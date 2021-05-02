package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityCommentsBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.finishActivityWithResult
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter.CommentsAdapter
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_UNKNOWN_ERROR
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.toCollection
import timber.log.Timber

/**
 * Shows the user the comments that people have published when marking a path as complete or project.
 * It's required that the intent contains the following extras:
 * [EXTRA_PATH_DOCUMENT]: The document path that leads to the [Path] that wants to be loaded.
 * May return the following result codes:
 * [RESULT_CODE_MISSING_DATA]: When required intent data was not found.
 * @author Arnau Mora
 * @since 20210430
 */
class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding

    private lateinit var firestore: FirebaseFirestore

    /**
     * Stores the completions that people have marked in the loaded path.
     * @author Arnau Mora
     * @since 20210501
     */
    private val completions = arrayListOf<MarkedDataInt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.v("Getting extras...")
        val pathDocument = intent.getExtra(EXTRA_PATH_DOCUMENT)

        if (pathDocument == null) {
            Timber.e("Could not find extra for path document.")
            finishActivityWithResult(RESULT_CODE_MISSING_DATA, null)
            return
        }

        Timber.v("Adding back listener")
        binding.backFab.setOnClickListener { onBackPressed() }

        Timber.v("Initializing Firestore...")
        firestore = Firebase.firestore

        Timber.v("Fetching Path data...")
        firestore.document(pathDocument)
            .get()
            .addOnSuccessListener { snapshot ->
                Timber.v("Got Path data, processing...")
                val path = Path(snapshot)

                doAsync {
                    Timber.v("Loaded path data, loading completions...")
                    completions.clear()
                    path.getCompletions(firestore).toCollection(completions)
                    uiContext {
                        loadLists()
                    }
                }
            }
            .addOnFailureListener {
                Timber.e(it, "Could not get path data")
                finishActivityWithResult(RESULT_CODE_UNKNOWN_ERROR, null)
            }
    }

    /**
     * Loads the notes and comments recycler views with the contents loaded.
     * @author Arnau Mora
     * @since 20210501
     * @see completions
     */
    @UiThread
    private fun loadLists() {
        if (completions.isEmpty()) {
            Timber.w("Cannot load lists since completions is empty.")
            return
        }
        Timber.v("Processing completions to get comments and notes...")
        val comments = arrayListOf<MarkedDataInt>()
        val notes = arrayListOf<MarkedDataInt>()
        for (completion in completions) {
            if (completion.comment != null && completion.comment.isNotBlank())
                comments.add(completion)
            if (completion.notes != null && completion.notes.isNotBlank())
                notes.add(completion)
        }

        Timber.v("Preparing comments recycler view's adapter.")
        val commentsAdapter = CommentsAdapter(this, completions)
        Timber.v("Setting comments recycler view's adapter and layout.")
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.commentsRecyclerView.adapter = commentsAdapter
    }
}
