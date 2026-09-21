package app.still.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstalledAppCatalog(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(LauncherApps::class.java)
            ?: error("LauncherApps is required")
    private val userManager =
        appContext.getSystemService(UserManager::class.java)
            ?: error("UserManager is required")

    private val _targets = MutableStateFlow<List<AppTarget>>(emptyList())
    val targets: StateFlow<List<AppTarget>> = _targets.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
    }

    init {
        launcherApps.registerCallback(callback)
        refresh()
    }

    fun refresh() {
        val profiles = launcherApps.profiles.ifEmpty { listOf(Process.myUserHandle()) }
        val collidingLabels = mutableMapOf<String, Int>()
        val loaded = mutableListOf<AppTarget>()

        for (profile in profiles) {
            val activities = runCatching {
                launcherApps.getActivityList(null, profile)
            }.getOrDefault(emptyList())
            for (activity in activities) {
                val target = toTarget(activity, profile, collidingLabels)
                loaded += target
            }
        }

        _targets.value = loaded.sortedWith(AppSearch.catalogOrder())
    }

    fun find(id: AppTargetId): AppTarget? = _targets.value.firstOrNull { it.id == id }

    fun resolveUserHandle(userSerialNumber: Long): UserHandle? {
        return launcherApps.profiles.firstOrNull { profile ->
            userManager.getSerialNumberForUser(profile) == userSerialNumber
        }
    }

    fun activityExists(id: AppTargetId): Boolean {
        val user = resolveUserHandle(id.userSerialNumber) ?: return false
        val component = ComponentName(id.packageName, id.activityClassName)
        return runCatching {
            launcherApps.isActivityEnabled(component, user)
        }.getOrDefault(false)
    }

    private fun toTarget(
        activity: LauncherActivityInfo,
        profile: UserHandle,
        collidingLabels: MutableMap<String, Int>,
    ): AppTarget {
        val component = activity.componentName
        val serial = userManager.getSerialNumberForUser(profile)
        val label = activity.label?.toString().orEmpty().ifBlank { component.packageName }
        val key = label.lowercase()
        val count = (collidingLabels[key] ?: 0) + 1
        collidingLabels[key] = count
        val indicator = if (count > 1 || profile != Process.myUserHandle()) {
            serial.toString()
        } else {
            null
        }
        return AppTarget(
            id = AppTargetId(
                packageName = component.packageName,
                activityClassName = component.className,
                userSerialNumber = serial,
            ),
            originalLabel = label,
            alias = null,
            profileIndicator = indicator,
            enabled = runCatching {
                launcherApps.isActivityEnabled(component, profile)
            }.getOrDefault(true),
        )
    }
}
