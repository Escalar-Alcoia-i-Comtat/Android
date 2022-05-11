package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData

@Dao
interface SectorsDatabaseDao {
    @Query("SELECT * FROM Sectors")
    fun getAll(): LiveData<List<SectorData>>

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): SectorData?

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<SectorData?>

    @Query("SELECT * FROM Sectors WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): SectorData?

    @Query("SELECT * FROM Sectors WHERE zone=:objectId")
    suspend fun getByParentId(objectId: String): List<SectorData>

    @Query("SELECT * FROM Sectors")
    suspend fun all(): List<SectorData>

    @Insert
    suspend fun insert(item: SectorData)

    @Update
    suspend fun update(item: SectorData)

    @Delete
    suspend fun delete(item: SectorData)

    @Query("DELETE FROM Sectors WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Sectors WHERE zone=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Sectors")
    suspend fun deleteAll()
}