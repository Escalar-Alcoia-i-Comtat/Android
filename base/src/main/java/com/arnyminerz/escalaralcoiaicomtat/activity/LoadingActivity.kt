package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreasFromCache
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoadingBinding
import com.parse.ParseObject
import timber.log.Timber

class LoadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressTextView.setText(R.string.status_downloading)
        ParseObject.unpinAllInBackground { error ->
            if (error != null)
                Timber.w(error, "Could not unpin data!")
            else {
                Timber.v("Unpinned all data.")
                loadAreasFromCache({ progress, max ->
                    Timber.i("Download progress: $progress / $max")
                    runOnUiThread {
                        if (max >= 0) {
                            binding.progressBar.max = max
                            binding.progressBar.setProgressCompat(progress, true)
                            binding.progressTextView.text =
                                getString(R.string.status_loading_progress, progress, max)
                        } else {
                            binding.progressBar.isIndeterminate = true
                            binding.progressTextView.setText(R.string.status_storing)
                        }
                    }
                }) {
                    if (AREAS.size > 0)
                        startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }
}
