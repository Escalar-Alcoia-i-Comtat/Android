package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import android.app.SearchManager
import android.content.Intent
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.SearchableActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.ui.NearbyZones
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.collectAsState
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

@Composable
@ExperimentalBadgeUtils
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalPermissionsApi
fun MainActivity.ExploreScreen() {
    val focusManager = LocalFocusManager.current

    val areas = exploreViewModel.areas

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Card(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(32.dp),
        ) {
            val focusRequester = remember { FocusRequester() }
            var isFocused by remember { mutableStateOf(false) }
            var searchTextField by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchTextField,
                onValueChange = { searchTextField = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(32.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_hint),
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(visible = isFocused) {
                        IconButton(
                            onClick = {
                                if (searchTextField.isEmpty())
                                    focusManager.clearFocus()
                                else
                                    focusRequester.requestFocus()
                                searchTextField = ""
                            },
                        ) {
                            Icon(Icons.Rounded.Close, "Clear text")
                        }
                    }
                },
                placeholder = {
                    Text(text = stringResource(R.string.search_hint))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        launch(SearchableActivity::class.java) {
                            action = Intent.ACTION_SEARCH
                            putExtra(SearchManager.QUERY, searchTextField)
                        }
                    }
                )
            )
        }

        // Nearby Zones
        val nearbyZonesEnabled by collectAsState(Keys.nearbyZonesEnabled, true)
        if (nearbyZonesEnabled)
            NearbyZones()

        // Areas list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(
                        visible = areas.isEmpty(),
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            items(areas) { area ->
                Timber.d("Displaying $area...")
                DataClassItem(area) {
                    launch(DataClassActivity::class.java) {
                        putExtra(EXTRA_DATACLASS, area as Parcelable)
                    }
                }
            }
        }
    }

    if (areas.isEmpty())
        exploreViewModel.loadAreas()
}
