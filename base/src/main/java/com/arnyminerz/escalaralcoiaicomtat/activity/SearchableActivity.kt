package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Application
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone.Companion.SAMPLE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.SearchItemTypeColor
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.LoadingIndicator
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.google.android.material.badge.ExperimentalBadgeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * An Activity that provides the user the possibility to perform searches. It's configured to be the
 * default search activity, for the search widgets.
 * @author Arnau Mora
 * @since 20210811
 */
@ExperimentalBadgeUtils
@ExperimentalMaterial3Api
class SearchableActivity : AppCompatActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    val searchQuery = if (intent.action == Intent.ACTION_SEARCH)
                        intent.getStringExtra(SearchManager.QUERY)?.trim()
                    else
                        null

                    val searchViewModel = SearchViewModel(application)
                    val list by searchViewModel.itemList.observeAsState()

                    if (searchQuery != null && searchQuery.isNotBlank())
                        searchViewModel.search(searchQuery)
                    else
                        Timber.w("Search query is null, won't search for anything.")

                    Column {
                        Timber.v("Search query: $searchQuery")
                        SearchBar(searchQuery ?: "") { query ->
                            Timber.v("New search query: $query")
                            searchViewModel.search(query)
                        }
                        Timber.v("Search results: $list")
                        if (list?.isEmpty() == true)
                            Text(
                                stringResource(R.string.search_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        else
                            SearchResultsView(list)
                    }
                }
            }
        }
    }

    /**
     * The sample parameter provider for the [SearchBar] preview.
     * Provides some sample strings to fill the search bar's content.
     * @author Arnau Mora
     * @since 20210811
     */
    class SampleSearchProvider(
        override val values: Sequence<String> = sequenceOf("Default", "Query", "Sample")
    ) : PreviewParameterProvider<String> {
        override val count: Int = values.count()
    }

    /**
     * A search bar for providing the user the option to perform a new search within the
     * [SearchableActivity].
     * @author Arnau Mora
     * @since 20210811
     * @param query The text that will be inside the text field.
     * @param search This will get called whenever the user performs a new search.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Preview(name = "Search bar preview", showBackground = true)
    @Composable
    fun SearchBar(
        @PreviewParameter(SampleSearchProvider::class) query: String,
        search: ((query: String) -> Unit)? = null
    ) {
        var value by remember { mutableStateOf(query) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide() ?: Timber.w("Keyboard controller is null")
                    search?.invoke(value)
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            leadingIcon = { Icon(Icons.Rounded.Search, "Search icon") },
            trailingIcon = {
                IconButton(onClick = { value = ""; focusRequester.requestFocus() }) {
                    Icon(Icons.Rounded.Close, "Clear text")
                }
            },
            placeholder = { Text(stringResource(R.string.search_hint)) }
        )
    }

    /**
     * A view that contains all the results from a search.
     * @author Arnau Mora
     * @since 0210811
     * @param results The results to show to the user.
     * @see SearchResult(dataClassImpl = ) This is the element that will render each result.
     */
    @Composable
    @OptIn(ExperimentalAnimationApi::class)
    private fun SearchResultsView(results: List<DataClassImpl>?) {
        Timber.v("Showing ${results?.size ?: "no"} results to the user...")

        Timber.v("Displaying LoadingIndicator ${results?.isNotEmpty() != true}")
        LoadingIndicator(isLoading = results?.isNotEmpty() != true)

        if (results != null) {
            Timber.v("Displaying LazyColumn...")
            LazyColumn {
                items(results) { result -> SearchResult(result) }
            }
        }
    }

    /**
     * For displaying a search result to the user.
     * @author Arnau Mora
     * @since 20210811
     * @param dataClassImpl The [DataClassImpl] that contains the searched data.
     */
    @Composable
    fun SearchResult(dataClassImpl: DataClassImpl) {
        val context = LocalContext.current

        Timber.v("Displaying a SearchResult...")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                val dataClassType = when (val type = dataClassImpl::class.java.simpleName) {
                    "Area" -> stringResource(R.string.data_type_area)
                    "Zone" -> stringResource(R.string.data_type_zone)
                    "Sector" -> stringResource(R.string.data_type_sector)
                    "Path" -> stringResource(R.string.data_type_path)
                    else -> type
                }

                Text(
                    text = dataClassImpl.displayName,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 26.sp,
                    fontFamily = CabinFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = SearchItemTypeColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append(dataClassType) }
                        val parent = runBlocking {
                            when (dataClassImpl) {
                                is Area -> null
                                is Zone -> dataClassImpl.getParent<Area>(this@SearchableActivity)
                                is Sector -> dataClassImpl.getParent<Zone>(this@SearchableActivity)
                                is Path -> dataClassImpl.getParent(app)
                                else -> null
                            }
                        }
                        if (parent != null)
                            append(" - ${parent.displayName}")
                        else Timber.e("Could not find parent for $dataClassImpl")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = async {
                        val intent = Intent(context, DataClassActivity::class.java)
                        var dataClass = if (dataClassImpl is Path)
                            dataClassImpl.getParent(app)
                        else
                            dataClassImpl

                        if (dataClass is Sector) {
                            val children = dataClass.getChildren(context) { it.displayName }
                            children
                                .indexOfFirst { it.objectId == dataClassImpl.objectId }
                                .takeIf { it >= 0 }
                                ?.let { intent.putExtra(EXTRA_INDEX, it) }
                            intent.putExtra(EXTRA_CHILDREN_COUNT, children.size)

                            dataClass = dataClass.getParent(context)
                        }

                        if (dataClass == null) {
                            Timber.e("Could not find a valid DataClass to launch")
                            uiContext { toast(R.string.toast_error_internal) }
                            return@async
                        }

                        intent.putExtra(EXTRA_DATACLASS, dataClass)

                        uiContext { launch(intent) }
                    }
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Enter element",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Preview(name = "Zone search result preview", device = Devices.DEFAULT, showBackground = true)
    @Composable
    fun SearchResultPreview() {
        SearchResult(SAMPLE)
    }

    class SearchViewModel(application: Application) : AndroidViewModel(application) {
        /**
         * The internal [MutableLiveData] that contains the search result.
         * @author Arnau Mora
         * @since 20210811
         */
        private val _itemList = MutableLiveData<List<DataClassImpl>>()

        /**
         * This should be observed since it will get updated when calling [search].
         * @author Arnau Mora
         * @since 20210811
         */
        val itemList: LiveData<List<DataClassImpl>>
            get() = _itemList

        /**
         * Performs a search for providing a result to the searchable activity.
         * @author Arnau Mora
         * @since 20210811
         * @param query The query for performing the search
         */
        @WorkerThread
        private suspend fun performSearch(query: String): List<DataClassImpl> {
            val searchResults = DataSingleton.getInstance(context)
                .repository
                .find(query)

            Timber.v("Got ${searchResults.size} results.")
            return searchResults
        }

        /**
         * Performs a search, and updates [itemList] with the results.
         * @author Arnau Mora
         * @since 20210811
         * @param query The [String] to perform the search with.
         */
        fun search(query: String) {
            viewModelScope.launch(Dispatchers.IO) {
                Timber.v("Searching for \"$query\"...")
                val result = performSearch(query)
                Timber.v("Search got ${result.size} results.")
                uiContext { _itemList.value = result }
            }
        }
    }
}
