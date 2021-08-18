package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SHOW_NON_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.sizeString
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentDownloadsBinding
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.DownloadSectionsAdapter
import kotlinx.coroutines.flow.toCollection
import timber.log.Timber

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
        doAsync {
            val dataDir = dataDir(requireContext())
            val sizeString = dataDir.sizeString()
            uiContext { binding.downloadSizeTextView.text = sizeString }
        }
    }

    override fun onResume() {
        super.onResume()

        reloadSizeTextView()

        val mainActivity = requireActivity() as MainActivity
        val storage = mainActivity.storage

        visibility(binding.downloadsRecyclerView, false)
        visibility(binding.noDownloadsTextView, false)
        visibility(binding.loadingDownloadsProgressBar, true)
        visibility(binding.loadingDownloadsProgressIndicator, true)

        doAsync {
            Timber.v("Getting downloaded sections list, SHOW_NON_DOWNLOADED: $SHOW_NON_DOWNLOADED")
            val sections = arrayListOf<DownloadedSection>()
            DownloadedSection.list(
                mainActivity.application as App,
                storage,
                SHOW_NON_DOWNLOADED
            ) { progress, max ->
                uiContext {
                    binding.loadingDownloadsProgressIndicator.progress = progress
                    binding.loadingDownloadsProgressIndicator.max = max
                }
            }.toCollection(sections)
            val sectionsEmpty = sections.isEmpty()
            Timber.v("Got sections list with ${sections.size} elements.")

            uiContext {
                visibility(binding.downloadsRecyclerView, !sectionsEmpty)
                visibility(binding.noDownloadsTextView, sectionsEmpty)

                binding.downloadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.downloadsRecyclerView.adapter =
                    DownloadSectionsAdapter(sections, requireActivity() as MainActivity)
                visibility(binding.loadingDownloadsProgressBar, false)
                visibility(binding.loadingDownloadsProgressIndicator, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
