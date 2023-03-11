package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path

@Dao
interface PathsDatabaseDao {
    @Query("SELECT * FROM Paths")
    fun getAll(): LiveData<List<Path>>

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): Path?

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<Path?>

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): Path?

    @Query("SELECT * FROM Paths WHERE sector=:objectId")
    suspend fun getByParentId(objectId: String): List<Path>

    @Query("SELECT * FROM Paths")
    suspend fun all(): List<Path>

    @Insert
    suspend fun insert(item: Path)

    @Update
    suspend fun update(item: Path)

    @Delete
    suspend fun delete(item: Path)

    @Query("DELETE FROM Paths WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Paths WHERE sector=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Paths")
    suspend fun deleteAll()
}