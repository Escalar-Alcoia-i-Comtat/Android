package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import timber.log.Timber

@ExperimentalUnsignedTypes
class BottomPermissionAskerFragment(
    val activity: Activity,
    private val permissions: Array<String>,
    private val requestCode: Int,
    private val customMessage: String? = null
) : BottomSheetDialogFragment() {
    companion object {
        private const val MY_LOCATION_PERMISSION_EVENT_ID = "my_location_enabled"
        private const val DISMISS_MY_LOCATION_PERMISSION_EVENT_NAME = "dismiss_my_location"
        private const val ENABLE_MY_LOCATION_PERMISSION_EVENT_NAME = "enable_my_location"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottomsheet_permission, container, false)

        val cancelButton = view.findViewById<MaterialButton>(R.id.cancel_permission_button)
        val grantPermissionButton = view.findViewById<MaterialButton>(R.id.grant_permission_button)
        cancelButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(
                MY_LOCATION_PERMISSION_EVENT_ID,
                DISMISS_MY_LOCATION_PERMISSION_EVENT_NAME
            )

            dismiss()
        }
        grantPermissionButton.setOnClickListener {
            Timber.v("Asking for permissions: $permissions")
            val bundle = Bundle()
            bundle.putString(
                MY_LOCATION_PERMISSION_EVENT_ID,
                ENABLE_MY_LOCATION_PERMISSION_EVENT_NAME
            )

            ActivityCompat.requestPermissions(activity, permissions, requestCode)

            dismiss()
        }

        val messageTextView = view.findViewById<TextView>(R.id.permission_dialog_message_textView)
        messageTextView.text = customMessage
            ?: getString(
                R.string.dialog_permission_message,
                permissions
            )

        return view
    }
}
