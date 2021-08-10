package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.dataCollectionSetUp
import com.arnyminerz.escalaralcoiaicomtat.core.ui.animation.EnterAnimation
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Backdrop
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.PassiveAreasExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorsExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.ZonesExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.AreasViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.ui.theme.EscalarAlcoiaIComtatTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@ExperimentalFoundationApi
@ExperimentalCoilApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
class NewMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataCollectionSetUp()

        val auth = Firebase.auth
        if (auth.currentUser == null)
            auth.signInAnonymously()

        setContent {
            val areasViewModel = AreasViewModel(this)
            val areas: List<Area> by areasViewModel.items.observeAsState(listOf())

            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    if (areas.isEmpty())
                        LoadingScreen(areasViewModel.progress.value?.float() ?: 0f)
                    else
                        MainContent()
                }
            }
        }
    }

    @Composable
    fun LoadingScreen(progress: Float) {
        LinearProgressIndicator(progress, modifier = Modifier.fillMaxWidth())
    }

    @Composable
    fun MainContent() {
        val navController = rememberNavController()

        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.app_name),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.onPrimary,
                            style = MaterialTheme.typography.h1
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = 0.dp,
                    navigationIcon = {
                        val state by navController.currentBackStackEntryAsState()
                        AnimatedContent(targetState = state?.arguments != null) { visible ->
                            if (visible)
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back icon")
                                }
                        }
                    },
                    actions = {
                        IconButton(onClick = { expanded = !expanded }) {
                            AnimatedContent(
                                targetState = expanded,
                                transitionSpec = {
                                    if (targetState) {
                                        slideInHorizontally({ width -> width }) + fadeIn() with
                                                slideOutHorizontally({ width -> -width }) + fadeOut()
                                    } else {
                                        slideInHorizontally({ width -> -width }) + fadeIn() with
                                                slideOutHorizontally({ width -> width }) + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                }
                            ) { isExpanded ->
                                if (isExpanded)
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close Menu"
                                    )
                                else
                                    Icon(
                                        Icons.Rounded.AccountCircle,
                                        contentDescription = "Open Menu"
                                    )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                var selectedItem by remember { mutableStateOf(0) }
                val items = listOf(
                    "Areas" to (R.string.item_explore to Icons.Rounded.Explore),
                    "Map" to (R.string.item_map to Icons.Rounded.Map),
                    "Downloads" to (R.string.item_downloads to Icons.Rounded.Download),
                    "Settings" to (R.string.item_settings to Icons.Rounded.Settings),
                )

                BottomNavigation {
                    items.forEachIndexed { index, (path, item) ->
                        val textRes = item.first
                        val icon = item.second
                        BottomNavigationItem(
                            icon = { Icon(icon, contentDescription = stringResource(textRes)) },
                            label = { Text(stringResource(textRes)) },
                            selected = selectedItem == index,
                            onClick = {
                                selectedItem = index
                                navController.navigate(path)
                            }
                        )
                    }
                }
            }
        ) {
            Backdrop(expanded = expanded, modifier = Modifier.padding(bottom = 56.dp), items = {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(.2f)
                                .scale(1f)
                        ) {
                            Image(
                                painter = rememberImagePainter("https://t3.ftcdn.net/jpg/03/46/83/96/360_F_346839683_6nAPzbhpSkIpb8pmAwufkC7c5eD7wYws.jpg"),
                                contentDescription = "Profile image"
                            )
                        }
                        Column(
                            modifier = Modifier.padding(
                                start = 12.dp,
                                top = 8.dp,
                                end = 8.dp,
                                bottom = 8.dp
                            )
                        ) {
                            Text(
                                "Not logged in",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                            Text("Tap to sign in", fontWeight = FontWeight.Light)
                        }
                    }
                }
                ListItem(
                    text = { Text("Share", color = MaterialTheme.colors.onPrimary) },
                    modifier = Modifier.clickable {
                        toast(this@NewMainActivity, "Sharing...")
                    }
                )
                ListItem(
                    text = { Text("Settings", color = MaterialTheme.colors.onPrimary) },
                    modifier = Modifier.clickable {
                        toast(this@NewMainActivity, "Settings...")
                    }
                )
            }) {
                NavHost(
                    navController = navController,
                    startDestination = "Areas"
                ) {
                    composable("Areas") {
                        EnterAnimation {
                            PassiveAreasExplorer(this@NewMainActivity, navController)
                        }
                    }
                    composable("Areas/{areaId}") { backStackEntry ->
                        val areaId = backStackEntry.arguments?.getString("areaId")
                        if (areaId != null)
                            EnterAnimation {
                                ZonesExplorer(this@NewMainActivity, navController, areaId)
                            }
                        else
                            Text(text = "Could not navigate to area: $areaId")
                    }
                    composable("Areas/{areaId}/Zones/{zoneId}") { backStackEntry ->
                        val areaId = backStackEntry.arguments?.getString("areaId")
                        val zoneId = backStackEntry.arguments?.getString("zoneId")
                        if (areaId != null && zoneId != null)
                            EnterAnimation {
                                SectorsExplorer(this@NewMainActivity, navController, areaId, zoneId)
                            }
                        else
                            Text(text = "Could not navigate to zone Z/$zoneId in A/$areaId")
                    }
                    composable("Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}") { backStackEntry ->
                        val areaId = backStackEntry.arguments?.getString("areaId")
                        val zoneId = backStackEntry.arguments?.getString("zoneId")
                        val sectorId = backStackEntry.arguments?.getString("sectorId")
                        if (areaId != null && zoneId != null && sectorId != null)
                            EnterAnimation {
                                SectorExplorer(
                                    this@NewMainActivity,
                                    navController,
                                    areaId,
                                    zoneId,
                                    sectorId
                                )
                            }
                        else
                            Text(text = "Could not navigate to sector S/$sectorId in zone Z/$zoneId in A/$areaId")
                    }

                    composable("Map") {
                        Text("This is the map page")
                    }
                    composable("Downloads") {
                        Text("This is the downloads page")
                    }
                    composable("Settings") {
                        Text("This is the settings page")
                    }
                }
            }
        }
    }
}