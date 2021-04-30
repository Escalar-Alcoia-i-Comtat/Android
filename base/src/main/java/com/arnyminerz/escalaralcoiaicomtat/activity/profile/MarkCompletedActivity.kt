package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.get
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.GRADES_LIST
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkCompletedData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkProjectData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMarkCompletedBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.doAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.finishActivityWithResult
import com.arnyminerz.escalaralcoiaicomtat.generic.getExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_PATH
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MARKED_AS_COMPLETE
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MARKED_AS_PROJECT
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_NOT_LOGGED_IN
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.toCollection
import timber.log.Timber

/**
 * Shows the user the possibility to mark a set [Path] as completed. Note that there should be a
 * logged in user.
 * The required extras are:
 * - [EXTRA_AREA]: The Area id that contains the path.
 * - [EXTRA_ZONE]: The Zone id inside [EXTRA_AREA] that contains the path.
 * - [EXTRA_SECTOR_INDEX]: The index of the path inside [EXTRA_ZONE] that contains the path.
 * - [EXTRA_PATH]: The id of the path to load inside [EXTRA_SECTOR_INDEX].
 * If any extra is missing, result code [RESULT_CODE_MISSING_DATA] will be returned.
 * If the user is not logged in [RESULT_CODE_NOT_LOGGED_IN] will be returned.
 * @author Arnau Mora
 * @since 20210429
 * @see RESULT_CODE_MISSING_DATA
 * @see RESULT_CODE_NOT_LOGGED_IN
 * @see EXTRA_AREA
 * @see EXTRA_ZONE
 * @see EXTRA_SECTOR_INDEX
 * @see EXTRA_PATH
 */
class MarkCompletedActivity : AppCompatActivity() {
    /**
     * The view binding of the Activity.
     * @author Arnau Mora
     * @since 20210429
     */
    private lateinit var binding: ActivityMarkCompletedBinding

    /**
     * The id of the [Area] where the [Path] to mark is in. May be loaded from Intent.
     * @author Arnau Mora
     * @since 20210429
     */
    private var areaId: String? = null

    /**
     * The id of the [Zone] where the [Path] to mark is in. May be loaded from Intent.
     * @author Arnau Mora
     * @since 20210429
     */
    private var zoneId: String? = null

    /**
     * The index of the [Sector] inside [areaId] where the [Path] to mark is in. May be loaded from
     * Intent.
     * @author Arnau Mora
     * @since 20210429
     */
    private var sectorIndex: Int? = null

    /**
     * The id of the [Path] to mark. May be loaded from Intent.
     * @author Arnau Mora
     * @since 20210429
     */
    private var pathId: String? = null

    /**
     * The loaded data of the [Area] where [Path] is in. Gets loaded in [loadPath].
     * @author Arnau Mora
     * @since 20210429
     */
    private var area: Area? = null

    /**
     * The loaded data of the [Zone] where [Path] is in. Gets loaded in [loadPath].
     * @author Arnau Mora
     * @since 20210429
     */
    private var zone: Zone? = null

    /**
     * The loaded data of the [Sector] where [Path] is in. Gets loaded in [loadPath].
     * @author Arnau Mora
     * @since 20210429
     */
    private var sector: Sector? = null

    /**
     * The loaded data of the [Path] to mark. Gets loaded in [loadPath].
     * @author Arnau Mora
     * @since 20210429
     */
    private var path: Path? = null

    /**
     * A reference to the Firestore instance to get the data from.
     * @author Arnau Mora
     * @since 20210429
     */
    private lateinit var firestore: FirebaseFirestore

    /**
     * A reference to the Firebase auth instance to load the user's data.
     * @author Arnau Mora
     * @since 20210429
     */
    private lateinit var auth: FirebaseAuth

    /**
     * The logged in user that wants to mark the path as completed
     * @author Arnau Mora
     * @since 20210429
     */
    private var user: FirebaseUser? = null

    /**
     * Initializes the Activity's contents, and calls the data fetch from intent, and the path data
     * loading.
     * @author Arnau Mora
     * @since 20210429
     * @see getFromIntent
     * @see loadPath
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the view binding
        binding = ActivityMarkCompletedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.v("Showing progress indicator...")
        loadingStatus(true)

        // Load the data from the intent, and if it's not complete, exit the activity and show a
        //   toast message.
        if (!getFromIntent()) {
            // Any extra missing
            Timber.e("Going back, one or more Intent's extra were missing")
            toast(R.string.toast_error_internal)
            finishActivityWithResult(RESULT_CODE_MISSING_DATA, null)
            return
        }

        // Fetch the Firestore instance
        firestore = Firebase.firestore
        auth = Firebase.auth

        // Get the logged in user
        user = auth.currentUser

        // Check if there's a logged in user
        if (user == null) {
            Timber.e("Going back, there's any logged in user.")
            toast(R.string.toast_error_login_required)
            finishActivityWithResult(RESULT_CODE_NOT_LOGGED_IN, null)
            return
        }

        // Load asyncronously
        doAsync {
            // Request to load the Path data
            loadPath()

            uiContext {
                // Update the UI
                initializeUI()
            }
        }
    }

    /**
     * Loads all the parameters from [getIntent] extras.
     * @author Arnau Mora
     * @since 20210429
     * @return True if all the parameters have been loaded, false otherwise, and the activity should
     * be exitted.
     */
    private fun getFromIntent(): Boolean {
        areaId = intent.getExtra(EXTRA_AREA)
        zoneId = intent.getExtra(EXTRA_ZONE)
        sectorIndex = intent.getExtra(EXTRA_SECTOR_INDEX)
        pathId = intent.getExtra(EXTRA_PATH)

        return areaId != null && zoneId != null && sectorIndex != null && pathId != null
    }

    /**
     * Loads the [Path] ([path]) data from the specified [areaId], [zoneId], [sectorIndex] and [pathId].
     * It is required that all the parameters are checked to be non-null, or [NullPointerException]
     * will be thrown.
     * @author Arnau Mora
     * @since 20210429
     * @throws NullPointerException When any of the parameters ([areaId], [zoneId], [sectorIndex] or
     * [pathId]) are null.
     */
    @Throws(NullPointerException::class)
    private suspend fun loadPath() {
        Timber.v("Loading area $areaId...")
        area = AREAS[areaId!!]
        if (area == null) {
            // Could not find valid Area
            Timber.e("Could not find Area $areaId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading zone $zoneId...")
        val zones = arrayListOf<Zone>()
        area!!.getChildren(firestore).toCollection(zones)
        try {
            zone = area!![zoneId!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Zone $zoneId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading sector #$sectorIndex...")
        val sectors = arrayListOf<Sector>()
        zone!!.getChildren(firestore).toCollection(sectors)
        try {
            sector = zone!![sectorIndex!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Sector #$sectorIndex")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading path $pathId...")
        val paths = arrayListOf<Path>()
        sector!!.getChildren(firestore).toCollection(paths)
        try {
            path = sector!![pathId!!]
        } catch (_: IndexOutOfBoundsException) {
            // Could not find valid Zone
            Timber.e("Could not find Path $pathId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }
    }

    /**
     * Refreshes the UI from the parameters loaded in [loadPath]. Also will initialize the grade
     * dropdown.
     * @author Arnau Mora
     * @since 20210429
     */
    @UiThread
    private fun initializeUI() {
        // Initialize the grades list
        Timber.v("Initializing grades list...")
        binding.gradeTextView.setAdapter(
            ArrayAdapter(
                this,
                R.layout.list_item,
                GRADES_LIST
            )
        )
        binding.gradeTextView.setText(path?.grade()?.toString())

        // Update the path name indicator
        val pathName = path?.displayName
        Timber.v("Updating path name ($pathName)...")
        binding.pathNameEditText.setText(pathName)

        // Add listener for project
        binding.projectCheckbox.setOnCheckedChangeListener { _, checked ->
            visibility(binding.noProjectLayout, !checked)
            binding.attemptsEditText.clearFocus()
            binding.fallsEditText.clearFocus()
            binding.gradeTextView.clearFocus()
        }

        // Add all editor (ime) listeners
        binding.attemptsEditText.setOnEditorActionListener { _, _, _ ->
            binding.fallsEditText.requestFocus()
            true
        }
        binding.fallsEditText.setOnEditorActionListener { _, _, _ ->
            binding.gradeTextField.requestFocus()
            true
        }
        binding.notesEditText.setOnEditorActionListener { _, _, _ ->
            binding.commentEditText.requestFocus()
            true
        }
        binding.commentEditText.setOnEditorActionListener { _, _, _ ->
            binding.commentEditText.clearFocus()
            true
        }

        // Add the submit listener
        binding.markCompletedButton.setOnClickListener {
            binding.attemptsEditText.clearFocus()
            binding.fallsEditText.clearFocus()
            binding.gradeTextView.clearFocus()
            binding.notesEditText.clearFocus()
            binding.commentEditText.clearFocus()

            submitForm()
        }

        // Error clear listeners
        binding.attemptsEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.attemptsTextField.isErrorEnabled = false
                binding.attemptsTextField.error = null
            }
        }
        binding.fallsEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.fallsTextField.isErrorEnabled = false
                binding.fallsTextField.error = null
            }
        }
        binding.gradeTextView.setOnItemClickListener { _, _, _, _ ->
            binding.gradeTextField.isErrorEnabled = false
            binding.gradeTextField.error = null
        }

        Timber.v("Hiding progress indicator...")
        loadingStatus(false)
    }

    /**
     * Updates the loading indicator and form visibility according to [isLoading].
     * @author Arnau Mora
     * @since 20210429
     * @param isLoading If true, the loading indicator will be shown, and the form won't, if false,
     * at the opposite.
     */
    @UiThread
    private fun loadingStatus(isLoading: Boolean) {
        binding.progressIndicator.visibility(isLoading)
        binding.formLayout.visibility(!isLoading)
    }

    /**
     * Checks that all the required fields of the form are filled up, and generates a bundle of data
     * for marking the path as complete.
     * Then, if everything is fine, submits the request.
     * @author Arnau Mora
     * @since 20210430
     */
    @UiThread
    private fun submitForm() {
        Timber.v("Checking form...")
        loadingStatus(true)

        val isProject = binding.projectCheckbox.isChecked
        val comment = binding.commentEditText.text?.toString()
        val notes = binding.notesEditText.text?.toString()

        if (isProject) {
            Timber.v("Marking path as project.")
            doAsync {
                Timber.v("Preparing data...")
                val data = MarkProjectData(user!!, comment, notes)
                Timber.v("Running mark project request...")
                path?.markProject(firestore, data)
                finishActivityWithResult(
                    RESULT_CODE_MARKED_AS_PROJECT,
                    Intent()
                        .putExtra(EXTRA_PATH_DOCUMENT, path!!.documentPath)
                )
            }
        } else {
            var error = false

            val attemptsText = binding.attemptsEditText.text?.toString()
            val attempts = attemptsText?.toIntOrNull()
            val fallsText = binding.fallsEditText.text?.toString()
            val falls = fallsText?.toIntOrNull()
            val grade = binding.gradeTextView.text?.toString()

            if (attempts == null || attemptsText.isBlank()) {
                binding.attemptsTextField.isErrorEnabled = true
                binding.attemptsTextField.error =
                    getString(R.string.mark_completed_attempts_required)
                error = true
            } else if (attempts < 0) {
                binding.attemptsTextField.isErrorEnabled = true
                binding.attemptsTextField.error = getString(R.string.mark_completed_attempts_size)
                error = true
            }

            if (falls == null || fallsText.isBlank()) {
                binding.fallsTextField.isErrorEnabled = true
                binding.fallsTextField.error = getString(R.string.mark_completed_attempts_required)
                error = true
            } else if (falls < 0) {
                binding.fallsTextField.isErrorEnabled = true
                binding.fallsTextField.error = getString(R.string.mark_completed_attempts_size)
                error = true
            }

            if (grade == null || grade.isBlank()) {
                binding.gradeTextField.isErrorEnabled = true
                binding.gradeTextField.error = getString(R.string.mark_completed_attempts_required)
                error = true
            }

            if (!error) {
                Timber.v("Marking path as completed.")
                doAsync {
                    Timber.v("Preparing data...")
                    val data = MarkCompletedData(user!!, attempts!!, falls!!, comment, notes)
                    Timber.v("Running mark completed request...")
                    path?.markCompleted(firestore, data)
                    finishActivityWithResult(
                        RESULT_CODE_MARKED_AS_COMPLETE,
                        Intent()
                            .putExtra(EXTRA_PATH_DOCUMENT, path!!.documentPath)
                    )
                }
            } else
                loadingStatus(false)
        }
    }
}
