package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData

@Dao
interface AreasDatabaseDao {
    @Query("SELECT * FROM Areas")
    fun getAll(): LiveData<List<AreaData>>

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): AreaData?

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<AreaData?>

    @Query("SELECT * FROM Areas")
    suspend fun all(): List<AreaData>

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): AreaData?

    @Insert
    suspend fun insert(item: AreaData)

    @Update
    suspend fun update(item: AreaData)

    @Delete
    suspend fun delete(item: AreaData)

    @Query("DELETE FROM Areas WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Areas")
    suspend fun deleteAll()
}