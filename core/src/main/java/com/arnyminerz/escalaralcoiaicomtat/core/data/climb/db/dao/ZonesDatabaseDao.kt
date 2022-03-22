package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData

@Dao
interface ZonesDatabaseDao {
    @Query("SELECT * FROM Zones")
    fun getAll(): LiveData<List<ZoneData>>

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): ZoneData?

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<ZoneData?>

    @Query("SELECT * FROM Zones WHERE downloaded=:downloaded")
    suspend fun getAllByDownloaded(downloaded: Boolean): List<ZoneData>

    @Query("SELECT * FROM Zones WHERE objectId=:objectId AND downloaded=:downloaded")
    suspend fun getAllByDownloadedId(objectId: String, downloaded: Boolean): List<ZoneData>

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): ZoneData?

    @Query("SELECT * FROM Zones WHERE area=:objectId")
    suspend fun getByParentId(objectId: String): List<ZoneData>

    @Query("SELECT * FROM Zones")
    suspend fun all(): List<ZoneData>

    @Insert
    suspend fun insert(item: ZoneData)

    @Update
    suspend fun update(item: ZoneData)

    @Delete
    suspend fun delete(item: ZoneData)

    @Query("DELETE FROM Zones WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Zones WHERE area=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Zones")
    suspend fun deleteAll()
}