package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.arnyminerz.escalaralcoiaicomtat.activity.LoadingActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen.StorageManagerWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme

class StorageActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Scaffold { paddingValues ->
                    StorageManagerWindow(paddingValues, {
                        startActivity(Intent(this, LoadingActivity::class.java))
                    }) {
                        startActivity(Intent(this, FeedbackActivity::class.java))
                    }
                }
            }
        }
    }
}