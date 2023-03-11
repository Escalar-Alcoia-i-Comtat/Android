package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

@Dao
interface ZonesDatabaseDao {
    @Query("SELECT * FROM Zones")
    fun getAll(): LiveData<List<Zone>>

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): Zone?

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<Zone?>

    @Query("SELECT * FROM Zones WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): Zone?

    @Query("SELECT * FROM Zones WHERE area=:objectId")
    suspend fun getByParentId(objectId: String): List<Zone>

    @Query("SELECT * FROM Zones")
    suspend fun all(): List<Zone>

    @Insert
    suspend fun insert(item: Zone)

    @Update
    suspend fun update(item: Zone)

    @Delete
    suspend fun delete(item: Zone)

    @Query("DELETE FROM Zones WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Zones WHERE area=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Zones")
    suspend fun deleteAll()
}