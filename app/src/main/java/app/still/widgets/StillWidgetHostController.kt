package app.still.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle

/**
 * Owns the process [AppWidgetHost] listening lifecycle for this launcher.
 * Host ID is stable for the app; widget instance IDs are not portable across devices.
 */
class StillWidgetHostController(
    context: Context,
) {
    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager =
        AppWidgetManager.getInstance(appContext)
    val host: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    fun createView(appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView {
        return host.createView(appContext, appWidgetId, info).apply {
            setAppWidget(appWidgetId, info)
        }
    }

    fun providerInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun providerInfo(component: ComponentName): AppWidgetProviderInfo? =
        appWidgetManager.installedProviders.firstOrNull { it.provider == component }

    fun listProviders(): List<WidgetProviderOption> {
        return appWidgetManager.installedProviders.map { info ->
            val label = info.loadLabel(appContext.packageManager)?.toString()
                ?: info.provider.packageName
            val preview = loadPreviewDrawable(info)
                ?: runCatching {
                    appContext.packageManager.getApplicationIcon(info.provider.packageName)
                }.getOrNull()
            WidgetProviderOption(
                providerFlattened = info.provider.flattenToString(),
                label = label,
                packageName = info.provider.packageName,
                minWidthDp = info.minWidth,
                minHeightDp = info.minHeight,
                configure = info.configure != null,
                previewDrawable = preview,
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun loadPreviewDrawable(info: AppWidgetProviderInfo): Drawable? {
        if (Build.VERSION.SDK_INT >= 31) {
            return runCatching { info.loadPreviewImage(appContext, 0) }.getOrNull()
        }
        val resId = info.previewImage
        if (resId == 0) return null
        return runCatching {
            appContext.packageManager.getDrawable(info.provider.packageName, resId, null)
        }.getOrNull()
    }

    fun bindIfAllowed(appWidgetId: Int, provider: ComponentName, options: Bundle? = null): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider, options)
    }

    fun createBindIntent(appWidgetId: Int, provider: ComponentName): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
    }

    fun createConfigureIntent(appWidgetId: Int, configure: ComponentName): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    fun defaultOptions(span: WidgetSpan): Bundle {
        val width = if (span == WidgetSpan.Half) 180 else 360
        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, if (span == WidgetSpan.Half) 220 else 480)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 250)
        }
    }

    fun canUseHalfWidth(info: AppWidgetProviderInfo): Boolean {
        // Providers that need more than ~250dp minimum width stay full-width.
        return info.minWidth in 1..250
    }

    companion object {
        const val HOST_ID = 0x5354494C // 'STIL'
    }
}
