package com.arnyminerz.escalaralcoiaicomtat.paging

import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import timber.log.Timber

/**
 * The paging source for loading the data for the Paging 3 library. Loads [DataClass]es content.
 * If all the IDs are null, the areas will be loaded.
 * Order of preference in case of multiple non-null parameters are specified is:
 * 1. [sectorId]
 * 2. [zoneId]
 * 3. [sectorId]
 * @author Arnau Mora
 * @since 20210819
 * @param searchSession The session for loading the data from.
 * @param areaId The ID of the area to load. Will load zone sectors.
 * @param zoneId The ID of the zone to load. Will load child sectors.
 * @param sectorId The ID of the sector to load. Will load child paths.
 */
class DataClassPagingSource(
    private val searchSession: AppSearchSession,
    val areaId: String? = null,
    val zoneId: String? = null,
    val sectorId: String? = null,
) : PagingSource<Int, DataClassImpl>() {
    data class SearchData<D : DataRoot<*>>(
        val id: String,
        val namespace: String,
        val dataClass: Class<D>
    )

    /**
     * Performs the search routine on the desired data.
     * @author Arnau Mora
     * @since 20210819
     * @param data The first param is the id to search for, the second one, the namespace where it's
     * stored at.
     */
    private suspend fun performSearch(
        data: SearchData<*>,
        loadSize: Int,
        offset: Int
    ): List<DataClassImpl> {
        Timber.v("Performing load...")
        // Will load the sector's contents, they are paths
        val id = data.id
        val namespace = data.namespace
        val dataClassType = data.dataClass
        val dataClassTypeName = dataClassType.simpleName
        // Set spec to search for paths
        Timber.v("Building SearchSpec...")
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(namespace)
            .setResultCountPerPage(offset + loadSize)
            .build()
        // Will search for sectorId, but in paths. This should be searching for parentSectorId
        Timber.v("Searching...")
        val searchResults = searchSession.search(id, searchSpec)
        Timber.v("Getting search results...")
        val nextPage = searchResults.nextPage.await()
        return arrayListOf<DataClassImpl>().apply {
            if (nextPage.size < offset)
                return@apply

            val page = nextPage.subList(offset, nextPage.size)
            for (searchResult in page) {
                val genericDocument = searchResult.genericDocument
                val schemaType = genericDocument.schemaType
                if (schemaType == dataClassTypeName) { // This should be true
                    val document = genericDocument.toDocumentClass(dataClassType).data()
                    add(document)
                } else // Just in case loaded data is not the correct type
                    Timber.w("Got invalid data in search. Schema: $schemaType")
            }
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DataClassImpl> {
        try {
            val loadSize = params.loadSize
            val key = params.key ?: 0 // This is offset
            val result: List<DataClassImpl> = when {
                sectorId != null ->
                    // Will load the sector's contents, they are paths
                    performSearch(
                        SearchData(sectorId, Path.NAMESPACE, PathData::class.java),
                        loadSize,
                        key
                    )
                zoneId != null ->
                    // Will load the zone's contents, they are sectors
                    performSearch(
                        SearchData(zoneId, Sector.NAMESPACE, SectorData::class.java),
                        loadSize,
                        key
                    )
                areaId != null ->
                    // Will load the area's contents, they are zones
                    performSearch(
                        SearchData(areaId, Zone.NAMESPACE, ZoneData::class.java),
                        loadSize,
                        key
                    )
                else ->
                    // Will load all the areas.
                    performSearch(
                        SearchData("", Area.NAMESPACE, AreaData::class.java),
                        loadSize,
                        key
                    )
            }
            return LoadResult.Page(
                data = result,
                prevKey = null, // Only paging forward
                nextKey = loadSize + key,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DataClassImpl>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}