package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.currentUrl
import com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.storage.StorageException
import timber.log.Timber
import java.io.IOException

class SectorsViewModel<A : Activity>(
    activity: A,
    private val areaId: String,
    private val zoneId: String
) : DataClassViewModel<Sector, A>(activity) {
    override val columnsPerRow: Int = 1

    override val fixedHeight: Dp = 200.dp

    override val items: LiveData<List<Sector>> = liveData(asyncCoroutineScope.coroutineContext) {
        val areas = app.getAreas()
        if (areas.isEmpty()) {
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application as App, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        }
        val zone = areas[areaId]?.get(app.searchSession, zoneId)
        uiContext { currentUrl.value = zone?.webUrl }
        val sectors = zone?.getChildren(app.searchSession)
        if (sectors != null) {
            for (sector in sectors)
                try {
                    sector.image(context, storage)
                } catch (e: StorageException) {
                    val exception = handleStorageException(e) ?: return@liveData
                    uiContext { context.toast(exception.first) }
                    Timber.e(e, exception.second)
                } catch (e: IOException) {
                    val stringRes = when (e) {
                        is AccessDeniedException -> R.string.toast_error_io_access_denied
                        else -> return@liveData
                    }
                    uiContext {
                        val str = context.getString(stringRes)
                        context.toast(
                            context.getString(
                                R.string.toast_error_load_image_sector,
                                str
                            )
                        )
                    }
                } catch (e: ArithmeticException) {

                }
            emit(sectors)
        } else Timber.e("Could not find Z/$zoneId in A/$areaId")
    }
}
