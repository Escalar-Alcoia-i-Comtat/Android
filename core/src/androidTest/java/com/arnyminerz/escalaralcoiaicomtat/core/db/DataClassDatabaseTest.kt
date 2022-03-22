package com.arnyminerz.escalaralcoiaicomtat.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.AreasDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.PathsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.SectorsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.ZonesDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.DataClassDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataClassDatabaseTest {
    private lateinit var areasDao: AreasDatabaseDao
    private lateinit var zonesDao: ZonesDatabaseDao
    private lateinit var sectorsDao: SectorsDatabaseDao
    private lateinit var pathsDao: PathsDatabaseDao

    private lateinit var db: DataClassDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, DataClassDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        areasDao = db.areasDao()
        zonesDao = db.zonesDao()
        sectorsDao = db.sectorsDao()
        pathsDao = db.pathsDao()
    }

    @After
    @Throws(IllegalStateException::class)
    fun deleteDb() {
        db.close()
    }

    @Test
    fun insertAndGetArea() = runBlocking {
        val area = Area.SAMPLE
        val areaData = area.data()
        areasDao.insert(areaData)

        var dbData = areasDao.getByObjectId(Area.SAMPLE_AREA_OBJECT_ID)
        assert(dbData == areaData)

        areasDao.deleteAll()

        dbData = areasDao.getByObjectId(Area.SAMPLE_AREA_OBJECT_ID)
        assert(dbData == null)
    }

    @Test
    fun insertAndGetZone() = runBlocking {
        val zone = Zone.SAMPLE
        val zoneData = zone.data()
        zonesDao.insert(zoneData)

        var dbData = zonesDao.getByObjectId(Zone.SAMPLE_OBJECT_ID)
        assert(dbData == zoneData)

        zonesDao.deleteAll()

        dbData = zonesDao.getByObjectId(Zone.SAMPLE_OBJECT_ID)
        assert(dbData == null)
    }

    @Test
    fun insertAndGetSector() = runBlocking {
        val sector = Sector.SAMPLE
        val sectorData = sector.data()
        sectorsDao.insert(sectorData)

        var dbData = sectorsDao.getByObjectId(Sector.SAMPLE_OBJECT_ID)
        assert(dbData == sectorData)

        sectorsDao.deleteAll()

        dbData = sectorsDao.getByObjectId(Zone.SAMPLE_OBJECT_ID)
        assert(dbData == null)
    }

    @Test
    fun insertAndGetPath() = runBlocking {
        val path = Path.SAMPLE_PATH
        val pathData = path.data()
        pathsDao.insert(pathData)

        var dbData = pathsDao.getByObjectId(Path.SAMPLE_PATH_OBJECT_ID)
        assert(dbData == pathData)

        pathsDao.deleteAll()

        dbData = pathsDao.getByObjectId(Path.SAMPLE_PATH_OBJECT_ID)
        assert(dbData == null)
    }
}