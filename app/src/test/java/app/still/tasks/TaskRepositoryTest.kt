package app.still.tasks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TaskRepositoryTest {
    private lateinit var db: StillDatabase
    private lateinit var repository: TaskRepository
    private var now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StillDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepository(context, db.taskDao(), clock = { now })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun blankTitleRejectedAndDraftPreservedByCallerContract() = runBlocking {
        assertThat(repository.addTask("   ")).isEqualTo(AddTaskResult.BlankTitle)
    }

    @Test
    fun duplicateRapidSubmitIgnored() = runBlocking {
        val first = repository.addTask("Same")
        assertThat(first).isInstanceOf(AddTaskResult.Created::class.java)
        now += 100
        assertThat(repository.addTask("Same")).isEqualTo(AddTaskResult.DuplicateIgnored)
        now += 1_000
        assertThat(repository.addTask("Same")).isInstanceOf(AddTaskResult.Created::class.java)
    }

    @Test
    fun completionDistinctFromDeletion() = runBlocking {
        val created = (repository.addTask("Ship") as AddTaskResult.Created).task
        repository.setCompleted(created.id, true)
        val done = db.taskDao().getById(created.id)
        assertThat(done?.completedAt).isNotNull()
        assertThat(done?.deletedAt).isNull()
        repository.moveToTrash(created.id)
        val trashed = db.taskDao().getById(created.id)
        assertThat(trashed?.deletedAt).isNotNull()
    }

    @Test
    fun importFailurePathLeavesReplaceToCaller() = runBlocking {
        repository.addTask("Keep")
        val before = repository.exportAll()
        val bad = TaskBackup.parseJson("nope")
        assertThat(bad.isFailure).isTrue()
        // Caller must not write on failure — repository still has original rows.
        assertThat(repository.exportAll()).isEqualTo(before)
    }

    @Test
    fun jsonReplaceRoundTrip() = runBlocking {
        repository.addTask("One")
        val exported = TaskBackup.toJson(repository.exportAll())
        val parsed = TaskBackup.parseJson(exported).getOrThrow()
        repository.importReplace(parsed)
        assertThat(repository.exportAll().map { it.title }).contains("One")
    }
}
