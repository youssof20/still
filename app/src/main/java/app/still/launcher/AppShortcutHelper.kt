package app.still.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserManager

/**
 * Published and pinned shortcuts for an app target. Availability depends on the
 * Home role and OEM support; empty lists are a normal outcome, not an error.
 */
data class AppShortcutItem(
    val packageName: String,
    val shortcutId: String,
    val userSerialNumber: Long,
    val label: String,
)

class AppShortcutHelper(
    context: Context,
    private val catalog: InstalledAppCatalog,
) {
    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(LauncherApps::class.java)
            ?: error("LauncherApps is required")
    private val userManager =
        appContext.getSystemService(UserManager::class.java)
            ?: error("UserManager is required")

    fun listShortcuts(id: AppTargetId): List<AppShortcutItem> {
        val user = catalog.resolveUserHandle(id.userSerialNumber) ?: return emptyList()
        if (Build.VERSION.SDK_INT < 25) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(id.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val shortcuts = runCatching {
            launcherApps.getShortcuts(query, user)
        }.getOrNull().orEmpty()
        return shortcuts.mapNotNull { info ->
            val label = info.shortLabel?.toString()
                ?: info.longLabel?.toString()
                ?: return@mapNotNull null
            AppShortcutItem(
                packageName = info.`package`,
                shortcutId = info.id,
                userSerialNumber = id.userSerialNumber,
                label = label,
            )
        }
    }

    fun startShortcut(item: AppShortcutItem): LaunchResult {
        val user = catalog.resolveUserHandle(item.userSerialNumber)
            ?: return LaunchResult.Failed(LaunchFailureReason.Missing)
        return runCatching {
            launcherApps.startShortcut(
                item.packageName,
                item.shortcutId,
                null,
                null,
                user,
            )
            LaunchResult.Success
        }.getOrElse {
            LaunchResult.Failed(LaunchFailureReason.Unavailable)
        }
    }

    /** Work-profile quiet mode for any managed profile visible to this launcher. */
    fun workProfilesPaused(): Boolean {
        return launcherApps.profiles.any { profile ->
            val serial = userManager.getSerialNumberForUser(profile)
            if (serial == userManager.getSerialNumberForUser(android.os.Process.myUserHandle())) {
                return@any false
            }
            runCatching { userManager.isQuietModeEnabled(profile) }.getOrDefault(false)
        }
    }

    fun hasWorkProfile(): Boolean {
        val self = android.os.Process.myUserHandle()
        return launcherApps.profiles.any { it != self }
    }
}

/** Explicit Product stance until Private Space lifecycle tests pass. */
object PrivateSpaceStatus {
    const val SUPPORTED = false
}
