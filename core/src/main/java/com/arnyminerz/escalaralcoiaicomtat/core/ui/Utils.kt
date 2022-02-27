package com.arnyminerz.escalaralcoiaicomtat.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.launch

/**
 * Used by [NavItems] for specifying the data of each item.
 * @author Arnau Mora
 * @since 20211227
 * @param text The text of the item.
 * @param icon The icon of the item.
 * @param contentDescription The content description of the icon of the item.
 */
sealed class Screen(
    val route: String,
    @StringRes val text: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    @StringRes val contentDescription: Int? = null
) {
    object Explore : Screen(
        "explore",
        R.string.item_explore,
        Icons.Outlined.Explore,
        Icons.Rounded.Explore,
        R.string.item_explore
    )

    object Map : Screen(
        "map",
        R.string.item_map,
        Icons.Outlined.Map,
        Icons.Rounded.Map,
        R.string.item_map
    )

    object Storage : Screen(
        "storage",
        R.string.item_storage,
        Icons.Outlined.Storage,
        Icons.Rounded.Storage,
        R.string.item_storage
    )

    object Settings : Screen(
        "settings",
        R.string.item_settings,
        Icons.Outlined.Settings,
        Icons.Rounded.Settings,
        R.string.item_settings
    )

    object Developer : Screen(
        "developer",
        R.string.item_developer,
        Icons.Outlined.BugReport,
        Icons.Rounded.BugReport,
        R.string.item_developer
    )
}

data class NavItem(val screen: Screen, val badgeCount: Int? = null)

@Composable
@ExperimentalPagerApi
fun RowScope.NavItems(pagerState: PagerState, items: List<NavItem>) {
    val scope = rememberCoroutineScope()
    items.forEachIndexed { index, item ->
        val screen = item.screen
        val selected = pagerState.currentPage == index
        NavigationBarItem(
            selected,
            icon = {
                if (item.badgeCount == null)
                    Icon(
                        if (selected) screen.selectedIcon ?: screen.icon else screen.icon,
                        screen.contentDescription?.let { stringResource(it) }
                    )
                else
                    BadgedBox(
                        badge = { Badge { Text(item.badgeCount.toString()) } }
                    ) {
                        Icon(
                            if (selected) screen.selectedIcon ?: screen.icon else screen.icon,
                            screen.contentDescription?.let { stringResource(it) }
                        )
                    }
            },
            label = { Text(text = stringResource(screen.text)) },
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )
    }
}
