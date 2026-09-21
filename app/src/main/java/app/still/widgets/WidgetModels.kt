package app.still.widgets

/**
 * Persisted widget placement intent. Instance IDs are device-local and are not
 * assumed portable across phones after backup restore.
 */
data class WidgetPlacement(
    val appWidgetId: Int,
    val providerFlattened: String,
    val span: WidgetSpan = WidgetSpan.Full,
    val alignment: WidgetAlignment = WidgetAlignment.Stretch,
) {
    init {
        require(appWidgetId > 0) { "appWidgetId must be positive" }
        require(providerFlattened.isNotBlank()) { "providerFlattened required" }
    }
}

enum class WidgetSpan {
    Full,
    Half,
}

enum class WidgetAlignment {
    Start,
    Center,
    End,
    Stretch,
}

data class WidgetProviderOption(
    val providerFlattened: String,
    val label: String,
    val packageName: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val configure: Boolean,
    val previewDrawable: android.graphics.drawable.Drawable?,
)
