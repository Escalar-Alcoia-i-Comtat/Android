package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteDir
import com.arnyminerz.escalaralcoiaicomtat.storage.filesDir
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_storage.*
import java.io.IOException

@ExperimentalUnsignedTypes
class StorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage)

        if(sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        clear_cache_button.setOnClickListener {
            try {
                deleteDir(cacheDir)
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(storage_layout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        clear_storage_button.setOnClickListener {
            try {
                deleteDir(cacheDir.parentFile)
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(storage_layout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        clear_downloads_button.setOnClickListener {
            try {
                deleteDir(filesDir(this))
            }catch (ex: IOException){
                ex.printStackTrace()
            }finally {
                Snackbar.make(storage_layout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
                refreshView()
            }
        }

        clear_settings_button.setOnClickListener {
            sharedPreferences?.edit()?.clear()?.apply()
            Snackbar.make(storage_layout, R.string.toast_clear_ok, Snackbar.LENGTH_SHORT).show()
            refreshView()
        }

        launch_app_button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        refreshView()
    }

    private fun refreshView(){
        clear_cache_button.isEnabled = cacheDir.exists()
        clear_storage_button.isEnabled = cacheDir.parentFile?.exists() ?: false
        clear_downloads_button.isEnabled = filesDir(this).exists()
        clear_settings_button.isEnabled = sharedPreferences != null
    }
}