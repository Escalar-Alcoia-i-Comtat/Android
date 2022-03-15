package com.arnyminerz.escalaralcoiaicomtat.core.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

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
        val area = Area.SAMPLE
        assertEquals(area.objectId, Area.SAMPLE_AREA_OBJECT_ID)
        assertEquals(area.displayName, Area.SAMPLE_AREA_DISPLAY_NAME)
        assertEquals(area.timestampMillis, Area.SAMPLE_AREA_TIMESTAMP)
        assertEquals(area.imagePath, Area.SAMPLE_AREA_IMAGE_REF)
        assertEquals(area.kmzPath, Area.SAMPLE_AREA_KMZ_REF)
        assertEquals(area.webUrl, Area.SAMPLE_AREA_WEB_URL)

        val metadata = area.metadata
        assertEquals(metadata.objectId, Area.SAMPLE_AREA_OBJECT_ID)
        assertEquals(metadata.namespace, Area.NAMESPACE)
        assertEquals(metadata.webURL, Area.SAMPLE_AREA_WEB_URL)
        assertEquals(metadata.parentId, null)

        val displayOptions = area.displayOptions
        assertEquals(displayOptions.columns, 1)
        assertEquals(displayOptions.downloadable, false)
        assertEquals(displayOptions.placeholderDrawable, R.drawable.ic_wide_placeholder)
        assertEquals(displayOptions.errorPlaceholderDrawable, R.drawable.ic_wide_placeholder)
    }
}