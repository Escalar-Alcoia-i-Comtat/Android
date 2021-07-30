package com.arnyminerz.escalaralcoiaicomtat.ui.elements

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.gms.instantapps.InstantApps

@Composable
fun ColumnScope.InstallButton(activity: Activity) {
    Button(
        onClick = {
            val postInstall = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage("com.arnyminerz.escalaralcoiaicomtat")
            InstantApps.showInstallPrompt(activity, postInstall, 0, null)
        },
        modifier = Modifier.align(Alignment.End),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
        ),
    ) {
        Text("Install App")
    }
}
