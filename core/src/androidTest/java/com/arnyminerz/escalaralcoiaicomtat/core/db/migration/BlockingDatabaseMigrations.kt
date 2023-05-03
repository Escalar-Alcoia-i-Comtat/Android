package com.arnyminerz.escalaralcoiaicomtat.core.db.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.BlockingDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BlockingDatabaseMigrations {
    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BlockingDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Tests the migration from 1 to 2, which changed the id type from String to Long. The migration
     * method is destructive, which means removing all the old entries.
     */
    @Test
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)
        db.apply {
            // Insert a sample blocking
            execSQL("INSERT INTO Blocking (id, path, type, end_date) VALUES (\"1234\", \"1234\", \"bird\", null)")
            close()
        }
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, BlockingDatabase.Companion.Migration1To2)

        db.query("SELECT * FROM Blocking").use { cur ->
            // Make sure all entries have been removed after migration
            assertEquals(cur.count, 0)
        }
    }
}