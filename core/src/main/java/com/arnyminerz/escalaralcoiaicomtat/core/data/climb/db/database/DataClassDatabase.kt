package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.converter.DateConverter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.AreasDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.PathsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.SectorsDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.ZonesDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone

@Database(
    entities = [Area::class, Zone::class, Sector::class, Path::class],
    version = 4,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = DataClassDatabase.AutoMigration1To2::class
        ),
        AutoMigration(
            from = 2,
            to = 3,
        ),
        AutoMigration(
            from = 3,
            to = 4,
        ),
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