package com.arnyminerz.escalaralcoiaicomtat.core.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingType
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class BlockingDatabaseTest : TestCase() {
    private lateinit var blockingDao: BlockingDatabaseDao

    private lateinit var db: BlockingDatabase

    @Before
    fun createDb() {
        Timber.i("Creating DB...")
        Timber.d("Getting context...")
        val context = ApplicationProvider.getApplicationContext<Context>()

        Timber.d("Building database...")
        db = Room.inMemoryDatabaseBuilder(context, BlockingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        Timber.d("Getting Blocking DAO...")
        blockingDao = db.blockingDao()
    }

    @After
    @Throws(IllegalStateException::class)
    fun deleteDb() {
        Timber.i("Deleting DB...")
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGet() = runBlocking {
        Timber.i("Running tests...")
        Timber.d("Initializing BlockingData...")
        val sampleBlockageId = "1234abc"
        val sampleBlockagePathId = "testingPath1"
        val sampleBlockage = BlockingData(
            sampleBlockageId,
            sampleBlockagePathId,
            BlockingType.DRY.idName,
            null,
        )

        Timber.d("Inserting sample blockage into DAO...")
        blockingDao.insert(sampleBlockage)

        Timber.d("Getting data by id...")
        val byIdData = blockingDao.getById(sampleBlockageId)
        Timber.d("Getting data by path id...")
        val byPathIdData = blockingDao.getByPathId(sampleBlockagePathId)
        Timber.d("Asserting results...")
        assert(byIdData == byPathIdData)
        assert(byIdData?.pathId == sampleBlockageId)

        Timber.d("Deleting all DAO blockages...")
        blockingDao.deleteAll()

        Timber.d("Getting blocking by id, should be null.")
        val nullObject = blockingDao.getById(sampleBlockageId)
        Timber.d("Asserting blockage...")
        assert(nullObject == null)
    }
}