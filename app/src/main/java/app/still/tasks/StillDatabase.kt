package app.still.tasks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StillDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "still.db"

        fun create(context: Context): StillDatabase =
            Room.databaseBuilder(context.applicationContext, StillDatabase::class.java, NAME)
                // Explicit migrations only. Do not enable destructive upgrade fallback.
                .addMigrations(*ALL_MIGRATIONS)
                .build()

        /**
         * When bumping [StillDatabase] version, add a Migration here and a corresponding test.
         * Never wipe user tasks to recover from a migration error.
         */
        val ALL_MIGRATIONS = emptyArray<androidx.room.migration.Migration>()
    }
}
