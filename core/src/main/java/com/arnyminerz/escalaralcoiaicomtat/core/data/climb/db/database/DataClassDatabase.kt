package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.converter.DateConverter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.AreasDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.PathsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.SectorsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.ZonesDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData

@Database(
    entities = [AreaData::class, ZoneData::class, SectorData::class, PathData::class],
    version = 1
)
@TypeConverters(DateConverter::class)
abstract class DataClassDatabase : RoomDatabase() {
    abstract fun areasDao(): AreasDatabaseDao

    abstract fun zonesDao(): ZonesDatabaseDao

    abstract fun sectorsDao(): SectorsDatabaseDao

    abstract fun pathsDao(): PathsDatabaseDao

    companion object {
        private var INSTANCE: DataClassDatabase? = null

        fun getInstance(context: Context): DataClassDatabase =
            synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    DataClassDatabase::class.java,
                    "DataClassDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }

}