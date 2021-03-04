package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.databinding.FragmentIntroDownloadBinding
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonArrayFromURL
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.runAsync
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import timber.log.Timber
import java.io.IOException

const val AREAS_URL = "$EXTENDED_API_URL/area/-1"

class DownloadAreasIntroFragment : Fragment() {
    companion object {
        var loading = false
            private set

        @ExperimentalUnsignedTypes
        @Throws(
            IOException::class
        )
        fun downloadAreasCache(
            context: Context,
            networkState: ConnectivityProvider.NetworkState,
            progressBar: ProgressBar? = null,
            connectionWaitingView: View? = null
        ) {
            loading = true
            context.onUiThread {
                progressBar?.visibility(true)
                connectionWaitingView?.visibility(false)
            }
            try {
                val storageDataDir = context.filesDir
                val areasDataFile = IntroActivity.cacheFile(context)
                val hasInternet = networkState.hasInternet
                if (!hasInternet) {
                    vibrate(context, 100)
                    toast(context, R.string.toast_error_no_internet)
                    progressBar?.visibility(false)
                    connectionWaitingView?.visibility(true)
                } else {
                    if (!storageDataDir.exists())
                        if (!storageDataDir.mkdirs())
                            throw IOException("Could not create data dir")

                    if (!areasDataFile.exists()) {
                        Timber.v("Downloading areas from \"$AREAS_URL\"...")
                        val areasJSON = jsonArrayFromURL(AREAS_URL)
                        val areasString = areasJSON.toString()

                        Timber.v("Writing areas to \"${areasDataFile.path}\"...")
                        if (!areasDataFile.createNewFile())
                            Timber.e("Could not create areas file")

                        areasDataFile.outputStream().use { it.write(areasString) }

                        if (!areasDataFile.exists())
                            Timber.e("Areas data file wasn't written")
                    }

                    context.onUiThread {
                        (context as? IntroActivity)?.next()
                    }
                }
            }catch (ex: Exception){
                loading = false
                throw ex
            } finally {
                loading = false
            }
        }
    }

    private var _binding: FragmentIntroDownloadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @ExperimentalUnsignedTypes
    override fun onResume() {
        super.onResume()

        val introActivity = if (activity is IntroActivity) (activity as IntroActivity) else null
        introActivity?.fabStatus(false)

        runAsync {
            downloadAreasCache(
                requireContext(),
                introActivity?.networkState ?: ConnectivityProvider.NetworkState.CONNECTED_NO_WIFI,
                binding.introDownloadSpinner,
                binding.internetWaitingLayout
            )
        }
    }
}