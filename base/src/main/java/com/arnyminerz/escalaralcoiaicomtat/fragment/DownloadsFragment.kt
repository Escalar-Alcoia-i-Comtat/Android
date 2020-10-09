package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.sizeString
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.DownloadSectionsAdapter
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import kotlinx.android.synthetic.main.fragment_downloads.*

@ExperimentalUnsignedTypes
class DownloadsFragment : NetworkChangeListenerFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_downloads, container, false)

    fun reloadSizeTextView() {
        val dataDir = dataDir(requireContext())
        downloadSize_textView.text = dataDir.sizeString()
    }

    override fun onResume() {
        super.onResume()

        reloadSizeTextView()

        val sections = DownloadedSection.list()

        downloadsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        downloadsRecyclerView?.adapter =
            DownloadSectionsAdapter(sections, requireActivity() as MainActivity)
    }
}