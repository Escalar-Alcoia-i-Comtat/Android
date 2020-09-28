package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity.Companion.hasStoragePermission
import kotlinx.android.synthetic.main.fragment_intro_storage.view.*

@ExperimentalUnsignedTypes
class StorageIntroFragment : Fragment() {
    companion object {
        const val STORAGE_PERMISSION_REQUEST = 45
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_storage, container, false)

        view.grant_storage_permission_button.apply {
            setOnClickListener {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST
                )
                isEnabled = if(!hasStoragePermission(requireContext())) {
                    setText(R.string.status_permission_granted)
                    false
                }else{
                    setText(R.string.action_grant_permission)
                    true
                }
            }

            return view
        }
    }
}