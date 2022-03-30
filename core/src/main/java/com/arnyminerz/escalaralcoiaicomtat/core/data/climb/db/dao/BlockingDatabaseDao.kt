package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData

@Dao
interface BlockingDatabaseDao {
    @Query("SELECT * FROM Blocking")
    fun getAll(): LiveData<List<BlockingData>>

    @Query("SELECT * FROM Blocking")
    suspend fun getAllOnce(): List<BlockingData>

    @Query("SELECT * FROM Blocking WHERE id=:id LIMIT 1")
    fun getById(id: String): BlockingData?

    @Query("SELECT * FROM Blocking WHERE path=:objectId LIMIT 1")
    fun getByPathId(objectId: String): BlockingData?

    @Insert
    suspend fun insert(item: BlockingData)

    @Update
    suspend fun update(item: BlockingData)

    @Delete
    suspend fun delete(item: BlockingData)

    @Query("DELETE FROM Blocking")
    fun deleteAll()
}