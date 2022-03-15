package com.arnyminerz.escalaralcoiaicomtat.core.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
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

        assert(Area.NAMESPACE == Namespace.AREA)
        assert(Zone.NAMESPACE == Namespace.ZONE)
        assert(Sector.NAMESPACE == Namespace.SECTOR)
        // assert(Path.NAMESPACE == Namespace.PATH)

        assertEquals(Area.NAMESPACE.ParentNamespace, null)
        assertEquals(Area.NAMESPACE.ChildrenNamespace, Zone.NAMESPACE)

        assertEquals(Zone.NAMESPACE.ParentNamespace, Area.NAMESPACE)
        assertEquals(Zone.NAMESPACE.ChildrenNamespace, Sector.NAMESPACE)

        assertEquals(Sector.NAMESPACE.ParentNamespace, Zone.NAMESPACE)
        // assertEquals(Sector.NAMESPACE.ChildrenNamespace, Path.NAMESPACE)

        // assertEquals(Path.NAMESPACE.ParentNamespace, Sector.NAMESPACE)
        // assertEquals(Path.NAMESPACE.ChildrenNamespace, null)
    }
}