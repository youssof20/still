package app.still.home

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.still.R
import app.still.widgets.StillWidgetHostController
import app.still.widgets.WidgetHostFrame
import app.still.widgets.WidgetPlacement
import app.still.widgets.WidgetProviderOption
import app.still.widgets.WidgetSpan
import app.still.widgets.WidgetAlignment

@Composable
fun WidgetsSurface(
    modifier: Modifier = Modifier,
    placements: List<WidgetPlacement>,
    providers: List<WidgetProviderOption>,
    host: StillWidgetHostController,
    pickingProvider: Boolean,
    onStartAdd: () -> Unit,
    onCancelPick: () -> Unit,
    onSelectProvider: (WidgetProviderOption) -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (WidgetPlacement) -> Unit,
) {
    if (pickingProvider) {
        WidgetPickerList(
            modifier = modifier,
            providers = providers,
            onSelect = onSelectProvider,
            onCancel = onCancelPick,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.widgets_explainer),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onStartAdd) {
            Text(stringResource(R.string.widget_add))
        }
        if (placements.isEmpty()) {
            Text(
                text = stringResource(R.string.widgets_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            placements.forEach { placement ->
                androidx.compose.runtime.key(placement.appWidgetId) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WidgetHostFrame(
                            placement = placement,
                            host = host,
                            editing = true,
                            onRemove = { onRemove(placement.appWidgetId) },
                            onToggleSpan = {
                                val next = if (placement.span == WidgetSpan.Half) {
                                    WidgetSpan.Full
                                } else {
                                    WidgetSpan.Half
                                }
                                onUpdate(placement.copy(span = next))
                            },
                            onCycleAlignment = {
                                val order = WidgetAlignment.entries
                                val idx = order.indexOf(placement.alignment)
                                onUpdate(placement.copy(alignment = order[(idx + 1) % order.size]))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerList(
    modifier: Modifier,
    providers: List<WidgetProviderOption>,
    onSelect: (WidgetProviderOption) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.cancel))
        }
        if (providers.isEmpty()) {
            Text(
                text = stringResource(R.string.widgets_no_providers),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(providers, key = { it.providerFlattened }) { option ->
                    WidgetProviderRow(option = option, onClick = { onSelect(option) })
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderRow(
    option: WidgetProviderOption,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            option.previewDrawable?.let { drawable ->
                val bitmap = remember(drawable) { drawable.toSafeBitmap() }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(option.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.widget_provider_meta,
                        option.packageName,
                        option.minWidthDp,
                        option.minHeightDp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (option.configure) {
                    Text(
                        text = stringResource(R.string.widget_needs_configure),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun Drawable.toSafeBitmap() = runCatching {
    toBitmap(width = intrinsicWidth.coerceAtLeast(48), height = intrinsicHeight.coerceAtLeast(48))
}.getOrNull()
