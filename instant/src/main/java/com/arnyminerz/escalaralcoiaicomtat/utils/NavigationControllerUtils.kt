package com.arnyminerz.escalaralcoiaicomtat.utils

import android.net.Uri
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.appsearch.localstorage.LocalStorage
import androidx.navigation.NavController
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import timber.log.Timber

/**
 * Navigates to the specified location but performing a search through AppSearch.
 * @author Arnau Mora
 * @since 20210730
 * @param uri The path to navigate to.
 */
fun NavController.searchNavigation(uri: Uri) {
    val strPath = uri.path ?: return
    val path = strPath.substring(1) // This removes the initial "/"
    if (path.isEmpty() || path == "imatge-inici.html" || path == "inici.html")
        return // Just load the Areas section that is selected by default
    else doAsync {
        Timber.v("Initializing search session...")
        val sessionFuture = LocalStorage.createSearchSession(
            // TODO: Database name should not be hardcoded
            LocalStorage.SearchContext.Builder(context, "escalaralcoiaicomtat")
                .build()
        )
        val session = sessionFuture.await()
        try {
            Timber.i("Searching for \"$uri\"")
            val searchResults = session.search(
                uri.toString(),
                SearchSpec.Builder()
                    .addFilterPackageNames(BuildConfig.APPLICATION_ID)
                    .build()
            )
            val searchPage = searchResults.nextPage.await()
            if (searchPage.isEmpty())
                Timber.w("Could not navigate to $uri. Could not find any results.")
            else {
                val page = searchPage[0]
                val genericDocument: GenericDocument = page.genericDocument
                val schemaType = genericDocument.schemaType
                val documentPath = when (schemaType) {
                    "AreaData" -> genericDocument.toDocumentClass(AreaData::class.java).documentPath
                    "ZoneData" -> genericDocument.toDocumentClass(ZoneData::class.java).documentPath
                    "SectorData" -> genericDocument.toDocumentClass(SectorData::class.java).documentPath
                    "PathData" -> genericDocument.toDocumentClass(PathData::class.java).documentPath
                    else -> null
                }
                if (documentPath == null)
                    Timber.w("The provided schema $schemaType is not valid")
                else
                    try {
                        Timber.v("Navigating for $documentPath")
                        uiContext { navigate(documentPath) }
                    } catch (e: NullPointerException) {
                        Timber.e(e, "Could not navigate to $documentPath.")
                    } catch (e: IllegalArgumentException) {
                        Timber.e(e, "Could not navigate to $documentPath.")
                    }
            }
        } catch (e: AppSearchException) {
            Timber.e(e, "There was an exception while parsing the search result")
        } finally {
            session.close()
        }
    }
}
