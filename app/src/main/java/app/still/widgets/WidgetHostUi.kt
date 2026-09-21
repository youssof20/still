package app.still.widgets

import android.content.ComponentName
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.still.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetHostFrame(
    placement: WidgetPlacement,
    host: StillWidgetHostController,
    editing: Boolean,
    onRemove: () -> Unit,
    onToggleSpan: () -> Unit,
    onCycleAlignment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val component = remember(placement.providerFlattened) {
        ComponentName.unflattenFromString(placement.providerFlattened)
    }
    val info = remember(placement.appWidgetId, placement.providerFlattened) {
        host.providerInfo(placement.appWidgetId)
            ?: component?.let { host.providerInfo(it) }
    }
    var showEdit by remember(placement.appWidgetId) { mutableStateOf(false) }
    val density = LocalDensity.current
    val minHeightDp = (info?.minHeight ?: 110).coerceAtLeast(80)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = when (placement.alignment) {
            WidgetAlignment.Start -> Alignment.Start
            WidgetAlignment.Center -> Alignment.CenterHorizontally
            WidgetAlignment.End -> Alignment.End
            WidgetAlignment.Stretch -> Alignment.CenterHorizontally
        },
    ) {
        if (info == null || component == null) {
            BrokenWidgetCard(
                editing = editing,
                onRemove = onRemove,
                onLongPress = { if (editing) showEdit = true },
            )
        } else {
            val widthFraction = when {
                placement.span == WidgetSpan.Half && host.canUseHalfWidth(info) -> 0.5f
                else -> 1f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (placement.alignment == WidgetAlignment.Stretch) 1f else widthFraction)
                    .widthIn(max = if (placement.span == WidgetSpan.Half) 280.dp else 480.dp)
                    .heightIn(min = minHeightDp.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (editing) showEdit = !showEdit
                        },
                    ),
            ) {
                AndroidView(
                    factory = { context ->
                        val hostView = host.createView(placement.appWidgetId, info)
                        FrameLayout(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            addView(
                                hostView,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                ),
                            )
                        }
                    },
                    update = { frame ->
                        val child = frame.getChildAt(0)
                        if (child != null) {
                            val options = host.defaultOptions(placement.span)
                            host.appWidgetManager.updateAppWidgetOptions(
                                placement.appWidgetId,
                                options,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = with(density) { minHeightDp.dp }),
                )
            }
        }

        if (editing && showEdit) {
            WidgetEditControls(
                canHalf = info != null && host.canUseHalfWidth(info),
                span = placement.span,
                alignment = placement.alignment,
                onToggleSpan = onToggleSpan,
                onCycleAlignment = onCycleAlignment,
                onRemove = onRemove,
                onDone = { showEdit = false },
            )
        }
    }
}

@Composable
private fun BrokenWidgetCard(
    editing: Boolean,
    onRemove: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.widget_broken),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (editing) {
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.widget_remove))
            }
        }
    }
}

@Composable
private fun WidgetEditControls(
    canHalf: Boolean,
    span: WidgetSpan,
    alignment: WidgetAlignment,
    onToggleSpan: () -> Unit,
    onCycleAlignment: () -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canHalf) {
                TextButton(onClick = onToggleSpan) {
                    Text(
                        stringResource(
                            if (span == WidgetSpan.Half) {
                                R.string.widget_span_full
                            } else {
                                R.string.widget_span_half
                            },
                        ),
                    )
                }
            }
            TextButton(onClick = onCycleAlignment) {
                Text(stringResource(R.string.widget_alignment, alignment.name))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.widget_remove))
            }
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

@Composable
fun WidgetShelfSection(
    placements: List<WidgetPlacement>,
    host: StillWidgetHostController,
    editing: Boolean,
    onRemove: (Int) -> Unit,
    onUpdate: (WidgetPlacement) -> Unit,
    onAddWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (placements.isEmpty() && !editing) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.widgets_title),
            style = MaterialTheme.typography.titleMedium,
        )
        placements.forEach { placement ->
            key(placement.appWidgetId) {
                WidgetHostFrame(
                    placement = placement,
                    host = host,
                    editing = editing,
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
                        val next = order[(idx + 1) % order.size]
                        onUpdate(placement.copy(alignment = next))
                    },
                )
            }
        }
        if (editing) {
            OutlinedButton(onClick = onAddWidget) {
                Text(stringResource(R.string.widget_add))
            }
        }
    }
}

/** Ensures host listening follows the composition that displays widgets. */
@Composable
fun RememberWidgetHostListening(host: StillWidgetHostController) {
    DisposableEffect(host) {
        host.startListening()
        onDispose { host.stopListening() }
    }
}
