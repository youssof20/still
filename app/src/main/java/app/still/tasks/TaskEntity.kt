package app.still.tasks

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["position"]),
        Index(value = ["deletedAt"]),
        Index(value = ["completedAt"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val position: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
)
