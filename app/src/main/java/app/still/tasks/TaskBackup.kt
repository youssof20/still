package app.still.tasks

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned task backup using platform org.json (no extra dependency).
 * Format version 1. Failed parse leaves caller responsible for keeping existing data.
 */
object TaskBackup {
    const val FORMAT_VERSION = 1

    fun toJson(tasks: List<TaskEntity>): String {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("position", task.position)
            obj.put("createdAt", task.createdAt)
            obj.put("updatedAt", task.updatedAt)
            obj.put("completedAt", task.completedAt ?: JSONObject.NULL)
            obj.put("deletedAt", task.deletedAt ?: JSONObject.NULL)
            array.put(obj)
        }
        val root = JSONObject()
        root.put("format", "still.tasks")
        root.put("version", FORMAT_VERSION)
        root.put("tasks", array)
        return root.toString()
    }

    fun parseJson(raw: String): Result<List<TaskEntity>> = runCatching {
        val root = JSONObject(raw)
        val format = root.optString("format")
        require(format == "still.tasks") { "Unsupported backup format: $format" }
        val version = root.getInt("version")
        require(version == FORMAT_VERSION) { "Unsupported backup version: $version" }
        val array = root.getJSONArray("tasks")
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.getString("title").trim()
                require(title.isNotEmpty()) { "Task title must not be blank" }
                add(
                    TaskEntity(
                        id = obj.getString("id").also { require(it.isNotBlank()) },
                        title = title,
                        position = obj.getLong("position"),
                        createdAt = obj.getLong("createdAt"),
                        updatedAt = obj.getLong("updatedAt"),
                        completedAt = if (obj.isNull("completedAt")) null else obj.getLong("completedAt"),
                        deletedAt = if (obj.isNull("deletedAt")) null else obj.getLong("deletedAt"),
                    ),
                )
            }
        }
    }

    fun toMarkdown(tasks: List<TaskEntity>): String {
        val active = tasks.filter { it.deletedAt == null && it.completedAt == null }
            .sortedBy { it.position }
        val done = tasks.filter { it.deletedAt == null && it.completedAt != null }
            .sortedByDescending { it.completedAt }
        return buildString {
            appendLine("# Tasks")
            appendLine()
            if (active.isEmpty()) {
                appendLine("_No active tasks._")
            } else {
                for (task in active) {
                    appendLine("- [ ] ${escapeMd(task.title)}")
                }
            }
            if (done.isNotEmpty()) {
                appendLine()
                appendLine("## Done")
                appendLine()
                for (task in done) {
                    appendLine("- [x] ${escapeMd(task.title)}")
                }
            }
        }
    }

    private fun escapeMd(title: String): String = title.replace("\n", " ").trim()
}

enum class ImportMode {
    Replace,
    Merge,
}
