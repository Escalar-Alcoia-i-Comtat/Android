package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.appsearch.app.SearchSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageAppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_INDEXED_SEARCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.filesDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityStorageBinding
import com.google.android.material.snackbar.Snackbar
import java.io.IOException

@ExperimentalMaterial3Api
class StorageActivity : LanguageAppCompatActivity() {
    private lateinit var binding: ActivityStorageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.clearCacheButton.setOnClickListener {
            try {
                deleteDir(cacheDir)
            } catch (ex: IOException) {
                ex.printStackTrace()
            } finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        binding.clearStorageButton.setOnClickListener {
            try {
                deleteDir(cacheDir.parentFile)
            } catch (ex: IOException) {
                ex.printStackTrace()
            } finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        binding.clearDownloadsButton.setOnClickListener {
            try {
                deleteDir(filesDir(this))
            } catch (ex: IOException) {
                ex.printStackTrace()
            } finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT)
                    .show()
                refreshView()
            }
        }

        binding.clearSettingsButton.setOnClickListener {
            sharedPreferences.edit()?.clear()?.apply()
            Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT)
                .show()
            refreshView()
        }

        binding.clearDataButton.setOnClickListener {
            binding.clearDataButton.isEnabled = false
            doAsync {
                val searchSession = app.searchSession
                val searchSpec = SearchSpec.Builder()
                    .addFilterPackageNames(BuildConfig.APPLICATION_ID)
                    .setResultCountPerPage(10000)
                    .build()
                searchSession.remove("", searchSpec).await()
                PREF_INDEXED_SEARCH.put(false)
                uiContext {
                    binding.clearDataButton.isEnabled = true
                    Snackbar.make(
                        binding.storageLayout,
                        R.string.toast_clear_ok,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    refreshView()
                }
            }
        }

        binding.launchAppButton.setOnClickListener {
            launch(MainActivity::class.java)
        }

        binding.feedbackButton.setOnClickListener {
            launch(FeedbackActivity::class.java)
        }

        refreshView()
    }

    private fun refreshView() {
        with(binding) {
            clearCacheButton.isEnabled = cacheDir.exists()
            clearStorageButton.isEnabled = cacheDir.parentFile?.exists() ?: false
            clearDownloadsButton.isEnabled = filesDir(this@StorageActivity).exists()
            clearSettingsButton.isEnabled = true
        }
    }
}
