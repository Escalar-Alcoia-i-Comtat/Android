package com.arnyminerz.escalaralcoiaicomtat.instant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.LifecycleOwner
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ERROR_REPORTING_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.theme.EscalarAlcoiaIComtatTheme
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.theme.ItemTextBackground
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel.AreasViewModel
import com.google.android.gms.instantapps.InstantApps
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import timber.log.Timber

const val STATUS_INSTALLED = "installed"
const val STATUS_INSTANT = "instant"
const val ANALYTICS_USER_PROP = "app_type"

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instantInfoSetup()
        dataCollectionSetUp()

        setContent {
            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainView(this)
                }
            }
        }
    }

    /**
     * Initializes the user-set data collection policy.
     * If debugging, data collection will always be disabled.
     * @author Arnau Mora
     * @since 20210617
     * @see SETTINGS_ERROR_REPORTING_PREF
     */
    @UiThread
    private fun dataCollectionSetUp() {
        val enableErrorReporting = SETTINGS_ERROR_REPORTING_PREF.get()

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Crashlytics collection enabled to $enableErrorReporting")

        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Analytics collection enabled to $enableErrorReporting")

        Firebase.performance.isPerformanceCollectionEnabled = enableErrorReporting
        Timber.v("Set Performance collection enabled to $enableErrorReporting")
    }

    /**
     * Sets an user property in Firebase Analytics for telling if the user is using instant or not.
     * @author Arnau Mora
     * @since 20210721
     */
    private fun instantInfoSetup() {
        val analytics = Firebase.analytics

        analytics.setUserProperty(
            ANALYTICS_USER_PROP,
            if (InstantApps.getPackageManagerCompat(this).isInstantApp)
                STATUS_INSTANT
            else
                STATUS_INSTALLED
        )
    }
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
fun MainView(activity: Activity) {
    MainContent(activity)
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
fun MainContent(activity: Activity) {
    var isLoading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val areasViewModel = AreasViewModel()
    val areasLiveData = areasViewModel.areas
    val areas: List<Area> by areasLiveData.observeAsState(listOf())

    areasLiveData.observe(activity as LifecycleOwner) {
        isLoading = it.isEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escalar Alcoià i Comtat") },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.primary,
        content = {
            Column {
                // Top Menu
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
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
                }

                // Content card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .animateContentSize(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        AnimatedVisibility(visible = isLoading, modifier = Modifier.size(52.dp)) {
                            CircularProgressIndicator()
                        }
                    }
                    AreasList(areas, R.drawable.ic_wide_placeholder)
                }
            }
        },
    )
}

@Composable
@ExperimentalCoilApi
fun AreasList(
    areas: List<Area>,
    @DrawableRes placeholder: Int
) {
    Timber.v("Loading areas list...")
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App contents
        items(areas) { area ->
            Timber.v("A/$area > Iterating...")
            val downloadUrl = area.downloadUrl
            if (downloadUrl == null)
                Timber.i("A/$area > Could not load image since downloadUrl is null")
            else
                area.AreaItem(placeholder, downloadUrl)
        }
    }
}

@Composable
@ExperimentalCoilApi
fun Area.AreaItem(@DrawableRes placeholder: Int, image: Uri) {
    val context = LocalContext.current
    var boxSize by remember { mutableStateOf(Size.Zero) }
    var height: Float by remember { mutableStateOf(0f) }

    Timber.v("$this > Composing area $displayName...")
    Timber.v("$this > Url: $image")
    Card(modifier = Modifier
        .clickable {
            toast(context, "Clicked $displayName")
        }
    ) {
        Box {
            Image(
                painter = rememberImagePainter(
                    data = image.toString(),
                    onExecute = { _, current ->
                        val size = current.size
                        val scale = boxSize.height / boxSize.width
                        height = size.height * scale
                        true
                    },
                    builder = {
                        placeholder(placeholder)
                    }
                ),
                contentScale = ContentScale.Crop,
                contentDescription = "Area image",
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .height(150.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        boxSize = layoutCoordinates.size.toSize()
                    }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItemTextBackground)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
