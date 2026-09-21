package app.still.launcher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore

/**
 * System intents used by Home info surfaces and gesture defaults.
 * Prefer launching a normal app activity over deep-linking into a single panel.
 */
object SystemAppIntents {
    private val CLOCK_PACKAGES = listOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock",
        "com.miui.clock",
        "com.huawei.deskclock",
    )

    private val CALENDAR_PACKAGES = listOf(
        "com.google.android.calendar",
        "com.android.calendar",
        "com.samsung.android.calendar",
        "com.oneplus.calendar",
        "com.xiaomi.calendar",
    )

    fun openDefaultClock(context: Context): Boolean {
        for (pkg in CLOCK_PACKAGES) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return runCatching { context.startActivity(launch); true }.getOrDefault(false)
            }
        }
        return runCatching {
            context.startActivity(
                Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    fun openDefaultCalendar(context: Context): Boolean {
        for (pkg in CALENDAR_PACKAGES) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return runCatching { context.startActivity(launch); true }.getOrDefault(false)
            }
        }
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(view); true }.getOrDefault(false)) return true
        val category = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(category); true }.getOrDefault(false)
    }

    fun openDefaultCamera(context: Context): Boolean {
        val capture = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(capture); true }.getOrDefault(false)) return true
        val main = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(main); true }.getOrDefault(false)
    }

    fun openDefaultPhone(context: Context): Boolean {
        val dial = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(dial); true }.getOrDefault(false)
    }

    fun requestUninstall(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    fun expandNotifications(context: Context): Boolean = expandStatusBar(context, notifications = true)

    fun expandQuickSettings(context: Context): Boolean = expandStatusBar(context, notifications = false)

    private fun expandStatusBar(context: Context, notifications: Boolean): Boolean {
        return runCatching {
            val service = context.getSystemService("statusbar") ?: return false
            val clazz = Class.forName("android.app.StatusBarManager")
            val methodName = if (notifications) {
                "expandNotificationsPanel"
            } else {
                "expandSettingsPanel"
            }
            val method = clazz.getMethod(methodName)
            method.invoke(service)
            true
        }.getOrDefault(false)
    }

    fun lockScreen(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return runCatching {
            dpm.lockNow()
            true
        }.getOrDefault(false)
    }

    fun resolveLaunchable(
        context: Context,
        packageName: String,
    ): ComponentName? {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return intent.component
    }
}
