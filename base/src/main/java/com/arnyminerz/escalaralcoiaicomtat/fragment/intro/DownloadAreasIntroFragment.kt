package com.arnyminerz.escalaralcoiaicomtat.fragment.intro

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.write
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonArrayFromURL
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

const val AREAS_URL = "$EXTENDED_API_URL/area/-1"

class DownloadAreasIntroFragment : Fragment() {
    companion object {
        @ExperimentalUnsignedTypes
        fun downloadAreasCache(
            activity: Activity,
            finishListener: (() -> Unit)? = null,
            errorListener: ((message: Int) -> Unit)? = null
        ) {
            val storageDataDir = activity.filesDir
            val areasDataFile = IntroActivity.cacheFile(activity)
            GlobalScope.launch {
                if (!storageDataDir.exists())
                    if (!storageDataDir.mkdirs()) {
                        Timber.e("Could not create data dir")
                        errorListener?.invoke(R.string.update_progress_error_data_dir_creation)
                        return@launch
                    }

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

                activity.runOnUiThread {
                    (activity as? IntroActivity)?.let {
                        it.fabStatus(true)
                        it.next()
                    }
                    finishListener?.invoke()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_intro_download, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}

    @ExperimentalUnsignedTypes
    override fun onResume() {
        super.onResume()

        (activity as? IntroActivity)?.fabStatus(false)

        downloadAreasCache(requireActivity())
    }
}