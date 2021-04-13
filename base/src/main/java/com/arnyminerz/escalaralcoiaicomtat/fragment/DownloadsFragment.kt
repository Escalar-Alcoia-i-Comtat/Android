package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentDownloadsBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.sizeString
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.DownloadSectionsAdapter
import com.arnyminerz.escalaralcoiaicomtat.shared.SHOW_NON_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber
import java.util.concurrent.CompletableFuture.runAsync

class DownloadsFragment : Fragment() {
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

    /**
     * Reloads the size TextView with the current [dataDir] size.
     * @author Arnau Mora
     * @since 20210406
     */
    @MainThread
    fun reloadSizeTextView() {
        runAsync {
            val dataDir = dataDir(requireContext())
            val sizeString = dataDir.sizeString()
            runOnUiThread { binding.downloadSizeTextView.text = sizeString }
        }
    }

    override fun onResume() {
        super.onResume()

        reloadSizeTextView()

        val mainActivity = requireActivity() as MainActivity
        val firestore = mainActivity.firestore

        visibility(binding.downloadsRecyclerView, false)
        visibility(binding.noDownloadsTextView, false)

        runAsync {
            Timber.v("Getting downloaded sections list, SHOW_NON_DOWNLOADED: $SHOW_NON_DOWNLOADED")
            val sections = DownloadedSection.list(mainActivity, firestore, SHOW_NON_DOWNLOADED)
            val sectionsEmpty = sections.isEmpty()
            Timber.v("Got sections list with ${sections.size} elements.")

            runOnUiThread {
                visibility(binding.downloadsRecyclerView, !sectionsEmpty)
                visibility(binding.noDownloadsTextView, sectionsEmpty)

                binding.downloadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.downloadsRecyclerView.adapter =
                    DownloadSectionsAdapter(sections, requireActivity() as MainActivity)
                binding.loadingDownloadsProgressBar.visibility(false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
