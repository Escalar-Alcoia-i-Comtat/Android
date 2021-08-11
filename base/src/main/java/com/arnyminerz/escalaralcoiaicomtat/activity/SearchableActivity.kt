package com.arnyminerz.escalaralcoiaicomtat.activity

import android.app.Application
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.WorkerThread
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.appsearch.localstorage.LocalStorage
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SEARCH_DATABASE_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.LoadingIndicator
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.context
import com.arnyminerz.escalaralcoiaicomtat.ui.theme.EscalarAlcoiaIComtatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchableActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val searchQuery = if (intent.action == Intent.ACTION_SEARCH)
                        intent.getStringExtra(SearchManager.QUERY)
                    else null

                    val searchViewModel = SearchViewModel(application)
                    val list: State<List<DataClassImpl>?> =
                        searchViewModel.itemList.observeAsState()
                    if (searchQuery != null)
                        searchViewModel.search(searchQuery)
                    else
                        Timber.w("Search query is null, won't search for anything.")

                    SearchResultsView(list.value)
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalAnimationApi::class)
    private fun SearchResultsView(results: List<DataClassImpl>?) {
        LoadingIndicator(isLoading = results != null || results?.isEmpty() == true)

        if (results != null)
            LazyColumn {
                items(results) { result ->
                    Text(text = result.namespace + "/" + result.objectId)
                }
            }
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
            val searchItems = arrayListOf<DataClassImpl>()

            Timber.v("Creating search session...")
            val session = LocalStorage.createSearchSession(
                LocalStorage.SearchContext.Builder(context, SEARCH_DATABASE_NAME).build()
            ).await()
            Timber.v("Creating search spec...")
            val searchSpec = SearchSpec.Builder()
                .addFilterPackageNames(BuildConfig.APPLICATION_ID)
                .build()
            Timber.v("Performing search...")
            val searchResults = session.search(query, searchSpec)
            Timber.v("Getting next page...")
            val searchPage = searchResults.nextPage.await()
            Timber.v("Got ${searchPage.size} results.")
            for (searchResult in searchPage) {
                val genericDocument = searchResult.genericDocument
                val schemaType = genericDocument.schemaType
                val dataClassImpl: DataClassImpl? = try {
                    when (schemaType) {
                        "AreaData" -> genericDocument.toDocumentClass(AreaData::class.java)
                            .area()
                        "ZoneData" -> genericDocument.toDocumentClass(ZoneData::class.java)
                            .zone()
                        "SectorData" -> genericDocument.toDocumentClass(SectorData::class.java)
                            .sector()
                        "PathData" -> genericDocument.toDocumentClass(PathData::class.java)
                            .path()
                        else -> null
                    }
                } catch (e: AppSearchException) {
                    Timber.e(e, "Failed to convert the data to a valid dataClass.")
                    null
                }
                if (dataClassImpl != null)
                    searchItems.add(dataClassImpl)
                else Timber.i("Could not add result since dataClassImpl is null")
            }

            return searchItems
        }

        /**
         * Performs a search, and updates [itemList] with the results.
         * @author Arnau Mora
         * @since 20210811
         * @param query The [String] to perform the search with.
         */
        fun search(query: String) {
            viewModelScope.launch(Dispatchers.IO) {
                _itemList.value = performSearch(query)
            }
        }
    }
}
