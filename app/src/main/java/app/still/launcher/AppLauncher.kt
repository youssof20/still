package app.still.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect

class AppLauncher(
    context: Context,
    private val catalog: InstalledAppCatalog,
) {
    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(LauncherApps::class.java)
            ?: error("LauncherApps is required")

    fun launch(id: AppTargetId): LaunchResult {
        val user = catalog.resolveUserHandle(id.userSerialNumber)
            ?: return LaunchResult.Failed(LaunchFailureReason.Missing)

        val component = ComponentName(id.packageName, id.activityClassName)
        val enabled = runCatching {
            launcherApps.isActivityEnabled(component, user)
        }.getOrNull()

        when (enabled) {
            null -> return LaunchResult.Failed(LaunchFailureReason.Unavailable)
            false -> {
                val packagePresent = runCatching {
                    appContext.packageManager.getPackageInfo(id.packageName, 0)
                    true
                }.getOrDefault(false)
                return if (!packagePresent) {
                    LaunchResult.Failed(LaunchFailureReason.Missing)
                } else {
                    LaunchResult.Failed(LaunchFailureReason.Disabled)
                }
            }
            true -> Unit
        }

        return runCatching {
            launcherApps.startMainActivity(component, user, null, null)
            LaunchResult.Success
        }.getOrElse {
            LaunchResult.Failed(LaunchFailureReason.Unavailable)
        }
    }

    fun openAppInfo(id: AppTargetId): LaunchResult {
        val user = catalog.resolveUserHandle(id.userSerialNumber)
            ?: return LaunchResult.Failed(LaunchFailureReason.Missing)
        val component = ComponentName(id.packageName, id.activityClassName)
        return runCatching {
            launcherApps.startAppDetailsActivity(component, user, Rect(), null)
            LaunchResult.Success
        }.getOrElse {
            LaunchResult.Failed(LaunchFailureReason.Unavailable)
        }
    }
}
