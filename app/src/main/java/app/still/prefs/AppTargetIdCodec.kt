package app.still.prefs

import app.still.launcher.AppTargetId

/** Shared string encoding for [AppTargetId]. Corrupt payloads return null; callers keep other prefs. */
object AppTargetIdCodec {
    const val FIELD_SEP = "\u0001"
    const val RECORD_SEP = "\n"
    const val VALUE_SEP = "\u0002"

    fun encode(id: AppTargetId): String =
        listOf(id.packageName, id.activityClassName, id.userSerialNumber.toString())
            .joinToString(FIELD_SEP)

    fun decode(raw: String?): AppTargetId? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(FIELD_SEP)
        if (parts.size != 3) return null
        val serial = parts[2].toLongOrNull() ?: return null
        return runCatching {
            AppTargetId.parse(parts[0], parts[1], serial)
        }.getOrNull()
    }

    fun encodeList(ids: List<AppTargetId>): String =
        ids.joinToString(RECORD_SEP) { encode(it) }

    fun decodeList(raw: String?): List<AppTargetId> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { decode(it) }
    }
}
