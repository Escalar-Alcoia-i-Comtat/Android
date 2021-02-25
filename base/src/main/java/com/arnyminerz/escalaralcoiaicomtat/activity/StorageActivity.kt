package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityStorageBinding
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteDir
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.google.android.material.snackbar.Snackbar
import java.io.IOException

@ExperimentalUnsignedTypes
class StorageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStorageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if(sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding.clearCacheButton.setOnClickListener {
            try {
                deleteDir(cacheDir)
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        binding.clearStorageButton.setOnClickListener {
            try {
                deleteDir(cacheDir.parentFile)
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        binding.clearDownloadsButton.setOnClickListener {
            try {
                deleteDir(filesDir(this))
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        binding.clearSettingsButton.setOnClickListener {
            sharedPreferences?.edit()?.clear()?.apply()
            Snackbar.make(binding.storageLayout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
            refreshView()
        }

        binding.launchAppButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        refreshView()
    }

    private fun refreshView(){
        with(binding) {
            clearCacheButton.isEnabled = cacheDir.exists()
            clearStorageButton.isEnabled = cacheDir.parentFile?.exists() ?: false
            clearDownloadsButton.isEnabled = filesDir(this@StorageActivity).exists()
            clearSettingsButton.isEnabled = sharedPreferences != null
        }
    }
}