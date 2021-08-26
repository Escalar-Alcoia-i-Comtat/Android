package com.arnyminerz.escalaralcoiaicomtat.core.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

/**
 * Contains the unit tests for checking [Area]-related issues.
 * @author Arnau Mora
 * @since 20210826
 */
@RunWith(MockitoJUnitRunner::class)
class AreaUnitTests {
    @Test
    fun testAreaInitialization() {
        println("Testing Area initialization...")
        val area = Area.SAMPLE_AREA
        assertEquals(area.objectId, Area.SAMPLE_AREA_OBJECT_ID)
        assertEquals(area.displayName, Area.SAMPLE_AREA_DISPLAY_NAME)
        assertEquals(area.timestampMillis, Area.SAMPLE_AREA_TIMESTAMP)
        assertEquals(area.imageReferenceUrl, Area.SAMPLE_AREA_IMAGE_REF)
        assertEquals(area.kmzReferenceUrl, Area.SAMPLE_AREA_KMZ_REF)
        assertEquals(area.documentPath, Area.SAMPLE_AREA_DOC_PATH)
        assertEquals(area.webUrl, Area.SAMPLE_AREA_WEB_URL)

        val uiMetadata = area.uiMetadata
        assertEquals(uiMetadata.placeholderDrawable, R.drawable.ic_wide_placeholder)
        assertEquals(uiMetadata.errorPlaceholderDrawable, R.drawable.ic_wide_placeholder)

        val metadata = area.metadata
        assertEquals(metadata.objectId, Area.SAMPLE_AREA_OBJECT_ID)
        assertEquals(metadata.namespace, Area.NAMESPACE)
        assertEquals(metadata.parentNamespace, null)
        assertEquals(metadata.childNamespace, Zone.NAMESPACE)
        assertEquals(metadata.documentPath, Area.SAMPLE_AREA_DOC_PATH)
        assertEquals(metadata.webURL, Area.SAMPLE_AREA_WEB_URL)
        assertEquals(metadata.parentId, null)

        val displayOptions = area.displayOptions
        assertEquals(displayOptions.columns, 1)
        assertEquals(displayOptions.downloadable, false)
    }
}