package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.converter.DateConverter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData

@Database(
    entities = [BlockingData::class],
    version = 2
)
@TypeConverters(DateConverter::class)
abstract class BlockingDatabase : RoomDatabase() {
    abstract fun blockingDao(): BlockingDatabaseDao

    companion object {
        @VisibleForTesting(VisibleForTesting.PRIVATE)
        var INSTANCE: BlockingDatabase? = null

        @VisibleForTesting(VisibleForTesting.PRIVATE)
        object Migration1To2 : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Blocking;")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Blocking` (`id` INTEGER NOT NULL, `path` TEXT NOT NULL, `type` TEXT NOT NULL, `end_date` INTEGER, PRIMARY KEY(`id`))")
            }
        }

        fun getInstance(context: Context): BlockingDatabase =
            synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    BlockingDatabase::class.java,
                    "BlockingDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .addMigrations(Migration1To2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}