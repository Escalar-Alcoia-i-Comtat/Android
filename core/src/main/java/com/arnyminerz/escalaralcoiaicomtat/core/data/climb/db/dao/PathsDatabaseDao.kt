package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData

@Dao
interface PathsDatabaseDao {
    @Query("SELECT * FROM Paths")
    fun getAll(): LiveData<List<PathData>>

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    suspend fun get(objectId: String): PathData?

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    fun getLiveData(objectId: String): LiveData<PathData?>

    @Query("SELECT * FROM Paths WHERE objectId=:objectId LIMIT 1")
    suspend fun getByObjectId(objectId: String): PathData?

    @Query("SELECT * FROM Paths WHERE sector=:objectId")
    suspend fun getByParentId(objectId: String): List<PathData>

    @Query("SELECT * FROM Paths")
    suspend fun all(): List<PathData>

    @Insert
    suspend fun insert(item: PathData)

    @Update
    suspend fun update(item: PathData)

    @Delete
    suspend fun delete(item: PathData)

    @Query("DELETE FROM Paths WHERE objectId IN(:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("DELETE FROM Paths WHERE sector=:objectId")
    suspend fun deleteByParentId(objectId: String)

    @Query("DELETE FROM Paths")
    suspend fun deleteAll()
}