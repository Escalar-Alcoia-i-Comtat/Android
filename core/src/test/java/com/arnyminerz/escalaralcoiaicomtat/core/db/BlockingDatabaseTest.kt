package com.arnyminerz.escalaralcoiaicomtat.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockingDatabaseTest {
    private lateinit var blockingDao: BlockingDatabaseDao

    private lateinit var db: BlockingDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, BlockingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        blockingDao = db.blockingDao()
    }

    @After
    @Throws(IllegalStateException::class)
    fun deleteDb() {
        db.close()
    }

    @Test
    fun insertAndGetArea() = runBlocking {
        val sampleBlockageId = "1234abc"
        val sampleBlockagePathId = "testingPath1"
        val sampleBlockage = BlockingData(
            sampleBlockageId,
            sampleBlockagePathId,
            BlockingType.DRY.idName,
            null,
        )

        blockingDao.insert(sampleBlockage)

        val byIdData = blockingDao.getById(sampleBlockageId)
        val byPathIdData = blockingDao.getByPathId(sampleBlockagePathId)
        assert(byIdData == byPathIdData)
        assert(byIdData?.pathId == sampleBlockageId)

        blockingDao.deleteAll()

        val nullObject = blockingDao.getById(sampleBlockageId)
        assert(nullObject == null)
    }
}