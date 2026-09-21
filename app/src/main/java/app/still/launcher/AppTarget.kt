package app.still.launcher

/**
 * Stable identity for a launchable activity in a specific Android profile.
 *
 * [userSerialNumber] is device-local. Do not assume it is portable across phones after backup restore.
 * Display labels and aliases are never used as primary keys.
 */
data class AppTargetId(
    val packageName: String,
    val activityClassName: String,
    val userSerialNumber: Long,
) {
    init {
        require(packageName.isNotBlank()) { "packageName must not be blank" }
        require(activityClassName.isNotBlank()) { "activityClassName must not be blank" }
    }

    fun flattenedComponent(): String = "$packageName/$activityClassName"

    companion object {
        fun parse(
            packageName: String,
            activityClassName: String,
            userSerialNumber: Long,
        ): AppTargetId = AppTargetId(
            packageName = packageName.trim(),
            activityClassName = activityClassName.trim(),
            userSerialNumber = userSerialNumber,
        )
    }
}

/**
 * Catalog entry. [originalLabel] is preserved for search even when [alias] is set later.
 */
data class AppTarget(
    val id: AppTargetId,
    val originalLabel: String,
    val alias: String? = null,
    val profileIndicator: String? = null,
    val enabled: Boolean = true,
) {
    val displayLabel: String
        get() = alias?.takeIf { it.isNotBlank() } ?: originalLabel
}
