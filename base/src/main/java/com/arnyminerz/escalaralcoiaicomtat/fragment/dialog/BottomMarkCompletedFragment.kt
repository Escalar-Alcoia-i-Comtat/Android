package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.work.*
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.user
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.work.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.angrybyte.numberpicker.view.ActualNumberPicker
import timber.log.Timber
import java.util.*

interface MarkListener {
    fun onMarkCompleted()
    fun onMarkFailed()
}

@ExperimentalUnsignedTypes
class BottomMarkCompletedFragment(
    val activity: Activity,
    private val path: Path,
    private val markListener: MarkListener
) : BottomSheetDialogFragment() {
    private var attempts = 1
    private var hangs = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottomsheet_mark_completed, container, false)

        if (user() == null) {
            dismiss()
            return null
        }

        val numberPicker = view.findViewById<ActualNumberPicker>(R.id.mark_completed_numberPicker)
        val dateTextView = view.findViewById<TextView>(R.id.date_textView)
        val nameTextView = view.findViewById<TextView>(R.id.zone_name_textView)
        val attemptsButtons = view.findViewById<MaterialButton>(R.id.mark_completed_attempts_button)
        val hangsButtons = view.findViewById<MaterialButton>(R.id.mark_completed_hangs_button)
        val markCompletedButton = view.findViewById<MaterialButton>(R.id.mark_completed_button)
        val markCompletedDatePicker =
            view.findViewById<FloatingActionButton>(R.id.mark_completed_date_picker)
        val progressBar = view.findViewById<ProgressBar>(R.id.mark_completed_progressBar)
        val nameFAB = view.findViewById<FloatingActionButton>(R.id.mark_completed_name_fab)

        val completedTypeToggleGroup =
            view.findViewById<MaterialButtonToggleGroup>(R.id.mark_completed_type_toggleButton)
        val attemptsHangsToggleGroup =
            view.findViewById<MaterialButtonToggleGroup>(R.id.mark_completed_ah_toggleButton)

        fun updateAttempts() {
            attemptsButtons.text = getString(R.string.completed_attemps_label, attempts.toString())
            hangsButtons.text = getString(R.string.completed_hangs_label, hangs.toString())
        }

        visibility(numberPicker, false)
        visibility(progressBar, false)

        attemptsButtons.setOnClickListener {
            visibility(numberPicker, false)
            numberPicker.value = attempts
            numberPicker.minValue = 1
            visibility(numberPicker, attemptsButtons.isChecked)
        }
        hangsButtons.setOnClickListener {
            visibility(numberPicker, false)
            numberPicker.value = hangs
            numberPicker.minValue = 0
            visibility(numberPicker, hangsButtons.isChecked)
        }
        numberPicker.setListener { _, newValue ->
            if (hangsButtons.isChecked)
                hangs = newValue
            else if (attemptsButtons.isChecked)
                attempts = newValue
            updateAttempts()
        }
        updateAttempts()

        markCompletedDatePicker.setOnClickListener {
            val builder = MaterialDatePicker.Builder.datePicker()
            val picker = builder.build()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setEnd(System.currentTimeMillis())
            builder.setCalendarConstraints(constraintsBuilder.build())

            picker.addOnPositiveButtonClickListener { millis ->
                val date = Date(millis)
                dateTextView.text = android.text.format.DateFormat.format(
                    getString(R.string.short_date_format),
                    date
                )
            }

            picker.show(parentFragmentManager, picker.toString())
        }

        dateTextView.text = android.text.format.DateFormat.format(
            getString(R.string.short_date_format),
            Date(System.currentTimeMillis())
        )
        nameTextView.text = path.displayName

        fun enabled(enabled: Boolean) {
            markCompletedDatePicker.isEnabled = !enabled
            completedTypeToggleGroup.isEnabled = !enabled
            attemptsHangsToggleGroup.isEnabled = !enabled
            markCompletedButton.isEnabled = !enabled
            nameFAB.isEnabled = !enabled
            visibility(numberPicker, false)

            visibility(progressBar, enabled)
        }

        markCompletedButton.setOnClickListener {
            enabled(true)
            val completedType = when (completedTypeToggleGroup.checkedButtonId) {
                R.id.mark_completed_type_first -> 0
                R.id.mark_completed_type_top_rope -> 1
                R.id.mark_completed_type_lead -> 2
                else -> -1
            }

            var error = false
            if (completedType < 0)
                error = true

            if (error) {
                Toast.makeText(
                    activity,
                    getString(R.string.toast_mark_completed_fill_all, completedType.toString()),
                    Toast.LENGTH_SHORT
                ).show()
                enabled(false)
            } else {
                // New Uploading schedule method: WorkManager
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                Toast.makeText(requireContext(), R.string.toast_marking, Toast.LENGTH_SHORT).show()

                val onetimeJob = OneTimeWorkRequestBuilder<MarkCompletedJob>()
                    .setConstraints(constraints)
                    .setInputData(
                        Data.Builder().apply {
                            putInt(MARK_COMPLETED_JOB_PATH, path.id)
                            putString(MARK_COMPLETED_JOB_PATH_DISPLAY_NAME, path.displayName)
                            putInt(MARK_COMPLETED_JOB_COMPLETED_TYPE, completedType)
                            putInt(MARK_COMPLETED_JOB_ATTEMPTS, attempts)
                            putInt(MARK_COMPLETED_JOB_HANGS, hangs)
                            putString(MARK_COMPLETED_JOB_USER_UID, user()!!.uid)
                        }.build()
                    )
                    .build()

                val workManagerInstance = WorkManager.getInstance(requireContext())
                workManagerInstance
                    .getWorkInfoByIdLiveData(onetimeJob.id)
                    .observe(viewLifecycleOwner, androidx.lifecycle.Observer { workInfo ->
                        if(workInfo == null) {
                            Timber.e("Got null workInfo")
                            return@Observer
                        }

                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> markListener.onMarkCompleted()
                            WorkInfo.State.FAILED -> markListener.onMarkFailed()
                            WorkInfo.State.RUNNING -> runOnUiThread {

                            }
                            else -> Timber.v("Got lifecycle update. State: %s", workInfo.state)
                        }
                    })
                workManagerInstance
                    .enqueue(onetimeJob)

                this.dismiss()
            }
        }

        return view
    }
}