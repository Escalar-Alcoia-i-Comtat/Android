package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.AreasDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.PathsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.SectorsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.ZonesDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

class DataClassRepository(
    private val areasDatabaseDao: AreasDatabaseDao,
    private val zonesDatabaseDao: ZonesDatabaseDao,
    private val sectorsDatabaseDao: SectorsDatabaseDao,
    private val pathsDatabaseDao: PathsDatabaseDao,
) {
    var readAllAreas: LiveData<List<Area>> = areasDatabaseDao.getAll()
    var readAllZones: LiveData<List<Zone>> = zonesDatabaseDao.getAll()
    var readAllSectors: LiveData<List<Sector>> = sectorsDatabaseDao.getAll()
    var readAllPaths: LiveData<List<Path>> = pathsDatabaseDao.getAll()

    suspend fun add(area: Area) = areasDatabaseDao.insert(area)

    suspend fun getArea(@ObjectId objectId: String) = areasDatabaseDao.get(objectId)

    suspend fun update(area: Area) = areasDatabaseDao.update(area)

    suspend fun delete(area: Area) = areasDatabaseDao.delete(area)

    suspend fun getAreas() = areasDatabaseDao.all()

    suspend fun clearAreas() = areasDatabaseDao.deleteAll()


    suspend fun add(zone: Zone) = zonesDatabaseDao.insert(zone)

    suspend fun getZone(@ObjectId objectId: String) = zonesDatabaseDao.get(objectId)

    suspend fun update(zone: Zone) = zonesDatabaseDao.update(zone)

    suspend fun delete(zone: Zone) = zonesDatabaseDao.delete(zone)

    suspend fun getZones() = zonesDatabaseDao.all()

    suspend fun clearZones() = zonesDatabaseDao.deleteAll()


    suspend fun add(sector: Sector) = sectorsDatabaseDao.insert(sector)

    suspend fun getSector(@ObjectId objectId: String) = sectorsDatabaseDao.get(objectId)

    suspend fun update(sector: Sector) = sectorsDatabaseDao.update(sector)

    suspend fun delete(sector: Sector) = sectorsDatabaseDao.delete(sector)

    suspend fun getSectors() = sectorsDatabaseDao.all()

    suspend fun clearSectors() = sectorsDatabaseDao.deleteAll()


    suspend fun add(path: Path) = pathsDatabaseDao.insert(path)

    suspend fun getPath(@ObjectId objectId: String) = pathsDatabaseDao.get(objectId)

    suspend fun update(path: Path) = pathsDatabaseDao.update(path)

    suspend fun delete(path: Path) = pathsDatabaseDao.delete(path)

    suspend fun getPaths() = pathsDatabaseDao.all()

    suspend fun clearPaths() = pathsDatabaseDao.deleteAll()


    /**
     * Gets all the elements there are in the set [namespace].
     * @author Arnau Mora
     * @since 20220316
     */
    suspend fun getAll(namespace: Namespace) =
        when (namespace) {
            Namespace.AREA -> getAreas()
            Namespace.ZONE -> getZones()
            Namespace.SECTOR -> getSectors()
            Namespace.PATH -> getPaths()
        }

    /**
     * Gets a DataClass from its [namespace] and [objectId].
     * @author Arnau Mora
     * @since 20220316
     */
    suspend fun get(namespace: Namespace, @ObjectId objectId: String) =
        when (namespace) {
            Namespace.AREA -> getArea(objectId)
            Namespace.ZONE -> getZone(objectId)
            Namespace.SECTOR -> getSector(objectId)
            Namespace.PATH -> getPath(objectId)
        }

    /**
     * Updates all the elements from [items] into the database.
     * @author Arnau Mora
     * @since 20220316
     */
    suspend fun updateAll(items: List<DataClassImpl>) {
        for (item in items)
            when (item) {
                is Area -> update(item)
                is Zone -> update(item)
                is Sector -> update(item)
                is Path -> update(item)
            }
    }


    /**
     * Searches in areas, zones, sectors and paths for the [query]. Display name, object id, or web
     * url. Display name may contain [query], others must match.
     * @author Arnau Mora
     * @since 20220316
     */
    @WorkerThread
    suspend fun find(query: String): List<DataClassImpl> {
        val areas = areasDatabaseDao.all()
            .filter { data ->
                data.displayName.contains(
                    query,
                    true
                ) || data.objectId == query || data.webUrl == query
            }
        val zones = zonesDatabaseDao.all()
            .filter { data ->
                data.displayName.contains(
                    query,
                    true
                ) || data.objectId == query || data.webUrl == query
            }
        val sectors = sectorsDatabaseDao.all()
            .filter { data ->
                data.displayName.contains(query, true) || data.objectId == query
            }
        val paths = pathsDatabaseDao.all()
            .filter { data ->
                data.displayName.contains(query, true) || data.objectId == query
            }
        return listOf(areas, zones, sectors, paths).flatten()
    }

    /**
     * Finds a DataClass from its [namespace] and [objectId]. May return null if the target is not
     * found.
     * @author Arnau Mora
     * @si  20220316
     * @param namespace The namespace of the object to find.
     * @param objectId The id of the object to find.
     */
    @WorkerThread
    suspend fun find(namespace: Namespace, @ObjectId objectId: String) =
        when (namespace) {
            Namespace.AREA -> areasDatabaseDao.getByObjectId(objectId)
            Namespace.ZONE -> zonesDatabaseDao.getByObjectId(objectId)
            Namespace.SECTOR -> sectorsDatabaseDao.getByObjectId(objectId)
            Namespace.PATH -> pathsDatabaseDao.getByObjectId(objectId)
        }

    /**
     * Adds something to the corresponding list according to [D].
     * @author Arnau Mora
     * @since 20220316
     * @param item The object to add.
     */
    suspend fun <D : DataClassImpl> add(item: D) = also {
        when (item) {
            is Area -> areasDatabaseDao.insert(item)
            is Zone -> zonesDatabaseDao.insert(item)
            is Sector -> sectorsDatabaseDao.insert(item)
            is Path -> pathsDatabaseDao.insert(item)
        }
    }

    /**
     * Runs update on the specific dao according to [D].
     * @author Arnau Mora
     * @since 20220316
     * @param item The item to update.
     */
    suspend fun <D : DataClassImpl> update(item: D) = also {
        when (item) {
            is Area -> areasDatabaseDao.update(item)
            is Zone -> zonesDatabaseDao.update(item)
            is Sector -> sectorsDatabaseDao.update(item)
            is Path -> pathsDatabaseDao.update(item)
        }
    }

    /**
     * Adds all the elements of [items] to the corresponding list according to its type.
     * @author Arnau Mora
     * @since 20220316
     * @param items The items to add.
     */
    suspend fun <D : DataClassImpl> addAll(items: Iterable<D>) = also { items.forEach { add(it) } }

    /**
     * Gets the children elements of the element in [namespace] with id [objectId].
     * @author Arnau Mora
     * @since 20220316
     * @param namespace The namespace of the object to get the children from.
     * @param objectId The id of the object to fetch the children from.
     * @return The list of the found elements, or null if [namespace] does not have children.
     */
    @WorkerThread
    suspend fun getChildren(namespace: Namespace, @ObjectId objectId: String) =
        when (namespace) {
            Namespace.AREA -> zonesDatabaseDao.getByParentId(objectId)
            Namespace.ZONE -> sectorsDatabaseDao.getByParentId(objectId)
            Namespace.SECTOR -> pathsDatabaseDao.getByParentId(objectId)
            else -> null
        }

    /**
     * Deletes all the elements at [namespace] with parent an element with id [parentObjectId].
     * @author Arnau Mora
     * @since 20220316
     * @param namespace The namespace of the elements to delete.
     * @param parentObjectId The id of the parent element.
     */
    @WorkerThread
    suspend fun deleteFromParentId(namespace: Namespace, @ObjectId parentObjectId: String) {
        when (namespace) {
            Namespace.ZONE -> zonesDatabaseDao.deleteByParentId(parentObjectId)
            Namespace.SECTOR -> sectorsDatabaseDao.deleteByParentId(parentObjectId)
            Namespace.PATH -> pathsDatabaseDao.deleteByParentId(parentObjectId)
            else -> {}
        }
    }
}