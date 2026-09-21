package app.still.tasks

import android.content.Context
import androidx.room.Room

object StillDatabaseProvider {
    @Volatile
    private var instance: StillDatabase? = null

    fun get(context: Context): StillDatabase {
        return instance ?: synchronized(this) {
            instance ?: StillDatabase.create(context).also { instance = it }
        }
    }

    fun createInMemory(context: Context): StillDatabase =
        Room.inMemoryDatabaseBuilder(context.applicationContext, StillDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
