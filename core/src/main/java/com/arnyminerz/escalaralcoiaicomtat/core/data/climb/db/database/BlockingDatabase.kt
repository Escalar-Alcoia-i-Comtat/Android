package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.converter.DateConverter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.dao.BlockingDatabaseDao
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData

@Database(entities = [BlockingData::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class BlockingDatabase : RoomDatabase() {
    abstract fun blockingDao(): BlockingDatabaseDao

    companion object {
        private var INSTANCE: BlockingDatabase? = null

        fun getInstance(context: Context): BlockingDatabase =
            synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    BlockingDatabase::class.java,
                    "BlockingDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }

}