package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector

@Dao
interface SectorsDatabaseDao {
    @Query("SELECT * FROM Sectors")
    fun getAll(): LiveData<List<Sector>>

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): Sector?

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<Sector?>

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): Sector?

    @Query("SELECT * FROM Sectors WHERE zone=:objectId")
    suspend fun getByParentId(objectId: String): List<Sector>

    @Query("SELECT * FROM Sectors")
    suspend fun all(): List<Sector>

    @Insert
    suspend fun insert(item: Sector)

    @Update
    suspend fun update(item: Sector)

    @Delete
    suspend fun delete(item: Sector)

    @Query("DELETE FROM Sectors WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Sectors WHERE zone=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Sectors")
    suspend fun deleteAll()
}