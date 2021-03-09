package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentDownloadsBinding
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.sizeString
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.DownloadSectionsAdapter
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir

@ExperimentalUnsignedTypes
class DownloadsFragment : NetworkChangeListenerFragment() {
    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun reloadSizeTextView() {
        val dataDir = dataDir(requireContext())
        binding.downloadSizeTextView.text = dataDir.sizeString()
    }

    override fun onResume() {
        super.onResume()

        reloadSizeTextView()

        val sections = DownloadedSection.list()

        binding.downloadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.downloadsRecyclerView.adapter =
            DownloadSectionsAdapter(sections, requireActivity() as MainActivity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
