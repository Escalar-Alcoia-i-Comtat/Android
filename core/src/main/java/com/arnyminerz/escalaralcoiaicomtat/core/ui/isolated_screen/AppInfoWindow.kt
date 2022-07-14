package com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.SemVer
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXPECTED_SERVER_VERSION
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.IconButtonWithText
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launchStore
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Github

@Composable
@ExperimentalMaterial3Api
fun ApplicationInfoWindow(
    @DrawableRes appIconResource: Int,
    appName: String,
    appBuild: Int,
    appVersion: String,
    serverVersion: SemVer,
    serverProduction: Boolean,
    githubLink: String,
    websiteLink: String,
) {
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, appIconResource)
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = rememberDrawablePainter(drawable),
            contentDescription = "",
            modifier = Modifier
                .size(128.dp)
                .padding(top = 64.dp, bottom = 8.dp)
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
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                stringResource(R.string.pref_info_server_version),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                serverVersion.toString(),
                modifier = Modifier
                    .padding(start = 4.dp),
                fontStyle = if (serverProduction) FontStyle.Normal else FontStyle.Italic,
                style = MaterialTheme.typography.labelMedium,
            )
            if (!serverProduction)
                Text(
                    stringResource(R.string.pref_info_server_version_non_production),
                    modifier = Modifier
                        .padding(start = 2.dp),
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.labelMedium,
                )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
        ) {
            IconButtonWithText(
                onClick = {
                    context.launch(Intent(Intent.ACTION_VIEW)) {
                        data = Uri.parse(githubLink)
                    }
                },
                icon = FontAwesomeIcons.Brands.Github,
                text = "Github",
            )
            IconButtonWithText(
                onClick = {
                    context.launch(Intent(Intent.ACTION_VIEW)) {
                        data = Uri.parse(websiteLink)
                    }
                },
                icon = Icons.Rounded.Public,
                text = stringResource(R.string.pref_info_website),
            )
        }

        if (serverVersion != EXPECTED_SERVER_VERSION)
            Card(
                colors = CardDefaults.cardColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp, horizontal = 16.dp),
            ) {
                Text(
                    stringResource(R.string.pref_info_server_version_warning_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(
                    stringResource(R.string.pref_info_server_version_warning_message)
                        .format(
                            EXPECTED_SERVER_VERSION.toString(),
                            serverVersion.toString(),
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                OutlinedButton(
                    onClick = { context.launchStore() },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                ) {
                    Text(stringResource(R.string.action_open_store))
                }
            }
    }
}

@Preview(showSystemUi = true)
@Composable
@ExperimentalMaterial3Api
fun ApplicationInfoWindowPreview() {
    ApplicationInfoWindow(
        R.drawable.round_close_24,
        "Escalar Alcoià i Comtat",
        1,
        "1.0.0",
        SemVer(1, 0, 4),
        false,
        "",
        "",
    )
}
