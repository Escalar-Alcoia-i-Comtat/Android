package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.finishActivityWithResult
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityCommentsBinding
import com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter.CommentsAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter.NotesAdapter
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_UNKNOWN_ERROR
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

    /**
     * The adapter for the comments list.
     * @author Arnau Mora
     * @since 20210528
     */
    private var commentsAdapter: CommentsAdapter? = null

    /**
     * The adapter for the notes list.
     * @author Arnau Mora
     * @since 20210528
     */
    private var notesAdapter: NotesAdapter? = null

    /**
     * Stores all the listeners for when a [Path] gets marked as completed.
     * @author Arnau Mora
     * @since 20210719
     */
    private val completionListeners = arrayListOf<ListenerRegistration>()

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
        fetchPathData(pathDocument)
    }

    override fun onStop() {
        super.onStop()
        // Cancel all the listeners.
        Timber.v("Removing all the completion listeners...")
        for (completionListener in completionListeners) {
            completionListener.remove()
            completionListeners.remove(completionListener)
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
            if (completion.comment != null && completion.comment?.isNotBlank() == true)
                comments.add(completion)
            if (completion.notes != null && completion.notes?.isNotBlank() == true)
                notes.add(completion)
        }

        Timber.v("Preparing comments recycler view's adapter...")
        commentsAdapter = CommentsAdapter(this, comments)
        Timber.v("Setting comments recycler view's adapter and layout.")
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.commentsRecyclerView.adapter = commentsAdapter

        Timber.v("Preparing notes recycler view's adapter...")
        notesAdapter = NotesAdapter(this, notes)
        Timber.v("Settings notes recycler view's adapter and layout.")
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = notesAdapter
    }

    /**
     * Notifies the Activity that an element has been deleted, so the UI can be updated consequently.
     * If there are any more comments or notes, the Activity is exitted.
     * @author Arnau Mora
     * @since 20210528
     */
    fun notifyDeletion() {
        val commentsCount = commentsAdapter?.itemCount ?: 0
        val notesCount = notesAdapter?.itemCount ?: 0
        val count = commentsCount + notesCount

        if (count <= 0)
            finish()
        else if (notesCount <= 0)
            binding.notesCardView.hide()
    }

    /**
     * Fetches all the [Path]'s data from [firestore].
     * @author Arnau Mora
     * @since 20210719
     * @param pathDocument The path of the document.
     */
    private fun fetchPathData(pathDocument: String) {
        firestore.document(pathDocument)
            .get()
            .addOnSuccessListener { snapshot ->
                Timber.v("Got Path data, processing...")
                val path = Path(snapshot)

                doAsync {
                    Timber.v("Loaded path data, observing completions...")
                    completions.clear()

                    val listener = path.observeCompletions(
                        firestore,
                        this@CommentsActivity
                    ) { onCompletionAdded(it) }
                    completionListeners.add(listener)

                    uiContext { loadLists() }
                }
            }
            .addOnFailureListener {
                Timber.e(it, "Could not get path data")
                finishActivityWithResult(RESULT_CODE_UNKNOWN_ERROR, null)
            }
    }

    /**
     * This will get called whenever a [Path] gets marked as completed, as initialized in [fetchPathData].
     * @author Arnau Mora
     * @since 20210719
     * @param markedDataInt The [MarkedDataInt] instance of the new completion.
     */
    @UiThread
    fun onCompletionAdded(markedDataInt: MarkedDataInt) {
        completions.add(markedDataInt)
        loadLists()
    }
}
