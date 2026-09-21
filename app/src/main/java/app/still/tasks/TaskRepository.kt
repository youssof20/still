package app.still.tasks

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

private val Context.taskDraftStore by preferencesDataStore(name = "task_draft")

class TaskRepository(
    context: Context,
    private val dao: TaskDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val appContext = context.applicationContext
    private val lastSubmitAt = AtomicLong(0L)
    private val lastSubmitTitle = AtomicReferenceSafe()

    val active: Flow<List<TaskEntity>> = dao.observeActive()
    val done: Flow<List<TaskEntity>> = dao.observeDone()
    val trash: Flow<List<TaskEntity>> = dao.observeTrash()

    fun preview(limit: Int): Flow<List<TaskEntity>> = dao.observeActivePreview(limit)

    val draft: Flow<String> = appContext.taskDraftStore.data.map { prefs ->
        prefs[DRAFT_KEY].orEmpty()
    }

    suspend fun setDraft(text: String) {
        appContext.taskDraftStore.edit { prefs ->
            if (text.isEmpty()) prefs.remove(DRAFT_KEY) else prefs[DRAFT_KEY] = text
        }
    }

    suspend fun clearDraft() = setDraft("")

    suspend fun addTask(rawTitle: String): AddTaskResult {
        val title = rawTitle.trim()
        if (title.isEmpty()) return AddTaskResult.BlankTitle
        val now = clock()
        val previousTitle = lastSubmitTitle.get()
        val previousAt = lastSubmitAt.get()
        if (title == previousTitle && now - previousAt < DUPLICATE_WINDOW_MS) {
            return AddTaskResult.DuplicateIgnored
        }
        val position = dao.maxActivePosition() + 1
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            position = position,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(task)
        lastSubmitTitle.set(title)
        lastSubmitAt.set(now)
        clearDraft()
        return AddTaskResult.Created(task)
    }

    suspend fun updateTitle(id: String, rawTitle: String): Boolean {
        val title = rawTitle.trim()
        if (title.isEmpty()) return false
        val existing = dao.getById(id) ?: return false
        dao.update(existing.copy(title = title, updatedAt = clock()))
        return true
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        val existing = dao.getById(id) ?: return
        val now = clock()
        dao.update(
            existing.copy(
                completedAt = if (completed) now else null,
                updatedAt = now,
            ),
        )
    }

    suspend fun moveToTrash(id: String) {
        val existing = dao.getById(id) ?: return
        val now = clock()
        dao.update(existing.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun restoreFromTrash(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(deletedAt = null, updatedAt = clock()))
    }

    suspend fun permanentlyDelete(id: String) {
        dao.deleteById(id)
    }

    suspend fun permanentlyDeleteAllTrash() {
        dao.getTrash().forEach { dao.deleteById(it.id) }
    }

    suspend fun moveActive(id: String, towardStart: Boolean) {
        val active = dao.getActive().toMutableList()
        val index = active.indexOfFirst { it.id == id }
        if (index < 0) return
        val swapWith = if (towardStart) index - 1 else index + 1
        if (swapWith !in active.indices) return
        val a = active[index]
        val b = active[swapWith]
        val now = clock()
        dao.update(a.copy(position = b.position, updatedAt = now))
        dao.update(b.copy(position = a.position, updatedAt = now))
    }

    suspend fun purgeExpiredTrash(retentionDays: Int = TRASH_RETENTION_DAYS): Int {
        val cutoff = clock() - retentionDays * DAY_MS
        return dao.purgeTrashOlderThan(cutoff)
    }

    suspend fun exportAll(): List<TaskEntity> = dao.getAll()

    suspend fun importReplace(tasks: List<TaskEntity>) {
        dao.replaceAll(tasks)
    }

    suspend fun importMerge(tasks: List<TaskEntity>) {
        val existing = dao.getAll().associateBy { it.id }.toMutableMap()
        for (task in tasks) {
            existing[task.id] = task
        }
        dao.replaceAll(existing.values.sortedBy { it.position })
    }

    companion object {
        private val DRAFT_KEY = stringPreferencesKey("draft_title")
        const val TRASH_RETENTION_DAYS = 30
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val DUPLICATE_WINDOW_MS = 800L
    }
}

sealed class AddTaskResult {
    data class Created(val task: TaskEntity) : AddTaskResult()
    data object BlankTitle : AddTaskResult()
    data object DuplicateIgnored : AddTaskResult()
}

private class AtomicReferenceSafe {
    @Volatile private var value: String? = null
    fun get(): String? = value
    fun set(next: String?) {
        value = next
    }
}
