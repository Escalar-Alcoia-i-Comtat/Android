package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
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
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = DataClassDatabase.AutoMigration1To2::class
        )
    ]
)
@TypeConverters(DateConverter::class)
abstract class DataClassDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "Zones", columnName = "downloaded")
    @DeleteColumn(tableName = "Zones", columnName = "downloadSize")
    @DeleteColumn(tableName = "Sectors", columnName = "downloaded")
    @DeleteColumn(tableName = "Sectors", columnName = "downloadSize")
    class AutoMigration1To2 : AutoMigrationSpec

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