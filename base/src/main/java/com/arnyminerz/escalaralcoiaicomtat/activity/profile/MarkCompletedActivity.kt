package com.arnyminerz.escalaralcoiaicomtat.activity.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.GRADES_LIST
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.CompletionType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.request.MarkCompletedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.request.MarkProjectData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PATH_DOCUMENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.finishActivityWithResult
import com.arnyminerz.escalaralcoiaicomtat.core.utils.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.core.worker.MarkCompletedWorker
import com.arnyminerz.escalaralcoiaicomtat.core.worker.MarkCompletedWorkerData
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityMarkCompletedBinding
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MARKED_AS_COMPLETE
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MARKED_AS_PROJECT
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.shared.RESULT_CODE_NOT_LOGGED_IN
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
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
    private lateinit var storage: FirebaseStorage

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
     * Stores the type index selected in [ActivityMarkCompletedBinding.typeTextView].
     * @author Arnau Mora
     * @since 20210502
     */
    private var typeSelectedIndex: Int = -1

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
        storage = Firebase.storage
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
        val app = application as App
        val areas = app.getAreas()
        Timber.v("Loading area $areaId...")
        area = areas[areaId!!]
        if (area == null) {
            // Could not find valid Area
            Timber.e("Could not find Area $areaId")
            onBackPressed()
            toast(R.string.toast_error_internal)
            return
        }

        Timber.v("Loading zone $zoneId...")
        area!!.getChildren()
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
        zone!!.getChildren()
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
        sector!!.getChildren()
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
            ArrayAdapter(this, R.layout.list_item, GRADES_LIST)
        )
        binding.gradeTextView.setText(path?.grade()?.toString())

        Timber.v("Initializing types list...")
        binding.typeTextView.setAdapter(
            ArrayAdapter(
                this,
                R.layout.list_item,
                listOf(
                    getString(R.string.mark_completed_type_first),
                    getString(R.string.mark_completed_type_top_rope)
                )
            )
        )

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
            binding.typeTextView.clearFocus()
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
        binding.typeTextView.setOnItemClickListener { _, _, position, _ ->
            binding.gradeTextField.isErrorEnabled = false
            binding.gradeTextField.error = null

            typeSelectedIndex = position
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
     * Shows an error in the desired [field] [TextInputLayout].
     * @author Arnau Mora
     * @since 20210502
     * @param field The field to show the error at
     * @param error The string resource of the error to show
     */
    private fun showError(field: TextInputLayout, @StringRes error: Int) {
        field.isErrorEnabled = true
        field.error = getString(error)
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

        if (isProject)
            markProject()
        else
            markCompleted()
    }

    /**
     * Marks the currently filled data as project.
     * @author Arnau Mora
     * @since 20210502
     */
    @MainThread
    private fun markProject() {
        Timber.v("Getting comment and notes...")
        val comment = binding.commentEditText.text?.toString()
        val notes = binding.notesEditText.text?.toString()

        Timber.v("Marking path as project.")
        doAsync {
            Timber.v("Preparing data...")
            val data = MarkProjectData(user!!, comment, notes)
            Timber.v("Running mark project request...")
            MarkCompletedWorker.schedule(
                this@MarkCompletedActivity,
                generateUUID(),
                MarkCompletedWorkerData(path!!, data)
            )
            finishActivityWithResult(
                RESULT_CODE_MARKED_AS_PROJECT,
                Intent()
                    .putExtra(EXTRA_PATH_DOCUMENT, path!!.documentPath)
            )
        }
    }

    /**
     * Checks all the corresponding form fields and show the error messages for marking the path as
     * completed.
     * @author Arnau Mora
     * @since 20210502
     * @return True if all the fields are correctly filled, false otherwise.
     */
    private fun checkCompletion(): Boolean {
        var error = false

        val attemptsText = binding.attemptsEditText.text?.toString()
        val attempts = attemptsText?.toIntOrNull()
        val fallsText = binding.fallsEditText.text?.toString()
        val falls = fallsText?.toIntOrNull()
        val grade = binding.gradeTextView.text?.toString()
        val typeText = binding.typeTextView.text?.toString()

        if (attempts == null || attemptsText.isBlank()) {
            showError(binding.attemptsTextField, R.string.mark_completed_attempts_required)
            error = true
        } else if (attempts < 0) {
            showError(binding.attemptsTextField, R.string.mark_completed_attempts_size)
            error = true
        }

        if (falls == null || fallsText.isBlank()) {
            showError(binding.fallsTextField, R.string.mark_completed_attempts_required)
            error = true
        } else if (falls < 0) {
            showError(binding.fallsTextField, R.string.mark_completed_attempts_size)
            error = true
        }

        if (grade == null || grade.isBlank()) {
            showError(binding.gradeTextField, R.string.mark_completed_grade_required)
            error = true
        }

        if (typeText == null || typeText.isBlank()) {
            showError(binding.typeTextField, R.string.mark_completed_type_required)
            error = true
        } else if (typeSelectedIndex < 0 || typeSelectedIndex > 1) {
            showError(binding.typeTextField, R.string.mark_completed_type_invalid)
            error = true
        }

        return error
    }

    /**
     * Marks the currently filled data as completed.
     * @author Arnau Mora
     * @since 20210502
     */
    private fun markCompleted() {
        val error = checkCompletion()

        if (!error) {
            Timber.v("Getting fields values...")
            val attempts = binding.attemptsEditText.text.toString().toInt()
            val falls = binding.fallsEditText.text.toString().toInt()
            val grade = binding.gradeTextView.text.toString()
            val comment = binding.commentEditText.text?.toString()
            val notes = binding.notesEditText.text?.toString()

            Timber.v("Processing completed type...")
            val completionType =
                if (typeSelectedIndex == 0) CompletionType.FIRST
                else CompletionType.TOP_ROPE

            Timber.v("Marking path as completed.")
            doAsync {
                Timber.v("Preparing data...")
                val data = MarkCompletedData(
                    user!!,
                    attempts to falls,
                    grade,
                    completionType,
                    comment,
                    notes
                )

                Timber.v("Running mark completed request...")
                MarkCompletedWorker.schedule(
                    this@MarkCompletedActivity,
                    generateUUID(),
                    MarkCompletedWorkerData(path!!, data)
                )

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
