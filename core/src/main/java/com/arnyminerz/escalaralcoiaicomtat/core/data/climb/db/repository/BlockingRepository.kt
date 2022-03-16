package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository

import androidx.lifecycle.LiveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData

class BlockingRepository(
    private val blockingDatabaseDao: BlockingDatabaseDao,
) {
    var all: LiveData<List<BlockingData>> = blockingDatabaseDao.getAll()

    suspend fun add(blocking: BlockingData) = blockingDatabaseDao.insert(blocking)

    suspend fun addAll(blockages: List<BlockingData>) = blockages.forEach { add(it) }

    suspend fun update(blocking: BlockingData) = blockingDatabaseDao.update(blocking)

    suspend fun delete(blocking: BlockingData) = blockingDatabaseDao.delete(blocking)

    fun clear() = blockingDatabaseDao.deleteAll()
}