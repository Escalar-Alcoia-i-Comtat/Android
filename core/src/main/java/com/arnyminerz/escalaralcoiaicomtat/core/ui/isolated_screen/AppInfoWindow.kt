package com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Github

@Composable
fun ApplicationInfoWindow(
    @DrawableRes appIconResource: Int,
    appName: String,
    appBuild: Int,
    appVersion: String,
    githubLink: String,
) {
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, appIconResource)
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = rememberDrawablePainter(drawable),
            contentDescription = "",
            modifier = Modifier
                .size(128.dp)
                .padding(top = 128.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = appName,
            modifier = Modifier
                .fillMaxWidth(.9f)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = "$appBuild - $appVersion",
            modifier = Modifier
                .fillMaxWidth(.7f)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
        Button(
            onClick = {
                context.launch(Intent(Intent.ACTION_VIEW)) {
                    data = Uri.parse(githubLink)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
            colors = ButtonDefaults.textButtonColors()
        ) {
            Column {
                Image(
                    FontAwesomeIcons.Brands.Github,
                    contentDescription = "Github",
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "GitHub",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ApplicationInfoWindowPreview() {
    ApplicationInfoWindow(
        R.drawable.round_close_24,
        "Escalar Alcoià i Comtat",
        1,
        "1.0.0",
        ""
    )
}
