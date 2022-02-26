package com.arnyminerz.escalaralcoiaicomtat.core.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ChildrenNamespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ParentNamespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for the @Namespace annotation.
 * @author Arnau Mora
 * @since 20220226
 */
@RunWith(MockitoJUnitRunner::class)
class NamespaceAnnotUnitTests {
    @Test
    fun testNamespaceExtension() {
        println("Testing namespace extensions")
        val areaNamespace = Area.NAMESPACE
        val zoneNamespace = Zone.NAMESPACE
        val sectorNamespace = Sector.NAMESPACE
        val pathNamespace = Path.NAMESPACE

        assertEquals(areaNamespace.ParentNamespace, "")
        assertEquals(areaNamespace.ChildrenNamespace, zoneNamespace)

        assertEquals(zoneNamespace.ParentNamespace, areaNamespace)
        assertEquals(zoneNamespace.ChildrenNamespace, sectorNamespace)

        assertEquals(sectorNamespace.ParentNamespace, zoneNamespace)
        assertEquals(sectorNamespace.ChildrenNamespace, pathNamespace)

        assertEquals(pathNamespace.ParentNamespace, sectorNamespace)
        assertEquals(pathNamespace.ChildrenNamespace, "")
    }
}