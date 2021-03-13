package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentIntroStorageBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.isPermissionGranted

class StorageIntroFragment : Fragment() {
    companion object {
        const val STORAGE_PERMISSION_REQUEST = 45
    }

    private var _binding: FragmentIntroStorageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroStorageBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.grantStoragePermissionButton.apply {
            setOnClickListener {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST
                )
                isEnabled = if (!requireContext().isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    setText(R.string.status_permission_granted)
                    false
                } else {
                    setText(R.string.action_grant_permission)
                    true
                }
            }

            return view
        }
    }
}
