package app.still.tasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND completedAt IS NULL
        ORDER BY position ASC, createdAt ASC
        """,
    )
    fun observeActive(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND completedAt IS NOT NULL
        ORDER BY completedAt DESC, updatedAt DESC
        """,
    )
    fun observeDone(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        """,
    )
    fun observeTrash(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        """,
    )
    suspend fun getTrash(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND completedAt IS NULL
        ORDER BY position ASC, createdAt ASC
        """,
    )
    suspend fun getActive(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND completedAt IS NULL
        ORDER BY position ASC, createdAt ASC
        LIMIT :limit
        """,
    )
    fun observeActivePreview(limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM tasks WHERE deletedAt IS NULL AND completedAt IS NULL")
    suspend fun maxActivePosition(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffEpochMs")
    suspend fun purgeTrashOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks ORDER BY position ASC, createdAt ASC")
    suspend fun getAll(): List<TaskEntity>

    @Transaction
    suspend fun replaceAll(tasks: List<TaskEntity>) {
        deleteAll()
        if (tasks.isNotEmpty()) {
            upsertAll(tasks)
        }
    }
}
