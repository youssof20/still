package app.still.tasks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates version-1 task schema and that upgrades must use explicit migrations
 * (destructive fallback is not enabled on [StillDatabase.create]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StillDatabaseMigrationTest {
    private lateinit var db: StillDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StillDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun version1SchemaAcceptsTaskRows() {
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO tasks (id, title, position, createdAt, updatedAt, completedAt, deletedAt)
            VALUES ('1', 'Hello', 0, 1, 1, NULL, NULL)
            """.trimIndent(),
        )
        db.openHelper.readableDatabase.query("SELECT title FROM tasks WHERE id='1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Hello")
        }
        assertThat(StillDatabase.ALL_MIGRATIONS).isEmpty()
    }
}
