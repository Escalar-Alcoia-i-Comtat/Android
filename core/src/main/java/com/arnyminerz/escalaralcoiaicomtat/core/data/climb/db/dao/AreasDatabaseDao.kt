package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area

@Dao
interface AreasDatabaseDao {
    @Query("SELECT * FROM Areas")
    fun getAll(): LiveData<List<Area>>

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): Area?

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<Area?>

    @Query("SELECT * FROM Areas")
    suspend fun all(): List<Area>

    @Query("SELECT * FROM Areas WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): Area?

    @Insert
    suspend fun insert(item: Area)

    @Update
    suspend fun update(item: Area)

    @Delete
    suspend fun delete(item: Area)

    @Query("DELETE FROM Areas WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Areas")
    suspend fun deleteAll()
}