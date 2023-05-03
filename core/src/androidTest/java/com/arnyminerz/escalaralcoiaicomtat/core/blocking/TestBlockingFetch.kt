package com.arnyminerz.escalaralcoiaicomtat.core.blocking

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.worker.BlockStatusWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TestBlockingFetch {
    companion object {
        private val blocking1 = JSONObject().apply {
            // {"id":3,"blocked":true,"type":"old","endDate":null}
            put("id", 3)
            put("blocked", true)
            put("type", "old")
            put("endDate", null)
        }
        private val blocking2 = JSONObject().apply {
            // {"id":5,"blocked":true,"type":"build","endDate":"2023-08-03T00:00:00.000Z"}
            put("id", 5)
            put("blocked", true)
            put("type", "build")
            put("endDate", "2023-08-03T00:00:00.000Z")
        }
        private val blocking3 = JSONObject().apply {
            put("id", 5)
            put("blocked", false)
            put("type", "build")
            put("endDate", "2023-08-03T00:00:00.000Z")
        }

        private val DefaultBlockages = JSONObject().apply {
            put("result", JSONObject().apply {
                put("1", blocking1)
                put("2", blocking2)
                put("3", blocking3)
            })
        }

        private val CutBlockages = JSONObject().apply {
            put("result", JSONObject().apply {
                put("1", blocking1)
                put("3", blocking3)
            })
        }
    }

    private val applicationContext by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

    private lateinit var db: BlockingDatabase

    private lateinit var dao: BlockingDatabaseDao

    /**
     * Initializes [db] to an in-memory database, and overrides [BlockingDatabase.INSTANCE] to be
     * this one, so all calls to [BlockingDatabase.getInstance] return the in-memory instance.
     */
    @Before
    fun initInMemoryDatabase() {
        db = Room.inMemoryDatabaseBuilder(applicationContext, BlockingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        BlockingDatabase.INSTANCE = db

        dao = db.blockingDao()
    }

    /**
     * Closes the database being used ([db]).
     */
    @After
    fun closeDatabase() {
        db.close()
    }

    /**
     * Runs [BlockStatusWorker.blockStatusFetchRoutine], which also runs
     * [BlockStatusWorker.fetchAllBlockagesFromServer], which is mocked in this case.
     */
    @Test
    fun test_blockStatusFetchRoutine() {
        mockkObject(BlockStatusWorker)
        coEvery { BlockStatusWorker.fetchAllBlockagesFromServer(any()) } returns DefaultBlockages

        // Run the routine
        runBlocking { BlockStatusWorker.blockStatusFetchRoutine(applicationContext) }

        // Verify that the mock was called
        coVerify { BlockStatusWorker.fetchAllBlockagesFromServer(any()) }

        runBlocking { dao.getAllOnce() }.let { blockingData ->
            // Size must match DefaultBlockagesList
            assertEquals(2, blockingData.size)

            // Make sure the loaded data matches the defined one in JSON (the last one is not blocked)
            blockingData[0].let { data ->
                assertEquals(3, data.id)
                assertEquals("1", data.pathId)
                assertNull(data.endDate)
                assertEquals("old", data.rawBlockingType)
                assertEquals("old", data.blockingType.idName)
            }
            blockingData[1].let { data ->
                assertEquals(5, data.id)
                assertEquals("2", data.pathId)
                assertEquals(1691013600000, data.endDate?.time)
                assertEquals("build", data.rawBlockingType)
                assertEquals("build", data.blockingType.idName)
            }
        }

        // Test that existing entries are updated correctly
        runBlocking { BlockStatusWorker.blockStatusFetchRoutine(applicationContext) }


        // Now the "server" should return all elements except one, check deletion
        coEvery { BlockStatusWorker.fetchAllBlockagesFromServer(any()) } returns CutBlockages

        // Run the routine
        runBlocking { BlockStatusWorker.blockStatusFetchRoutine(applicationContext) }

        runBlocking { dao.getAllOnce() }.let { blockingData ->
            // Size must match CutBlockages, one should have been deleted
            assertEquals(1, blockingData.size)

            // Make sure the loaded data matches the defined one in JSON (the last one is not blocked)
            blockingData[0].let { data ->
                assertEquals(3, data.id)
                assertEquals("1", data.pathId)
                assertNull(data.endDate)
                assertEquals("old", data.rawBlockingType)
                assertEquals("old", data.blockingType.idName)
            }
        }
    }
}