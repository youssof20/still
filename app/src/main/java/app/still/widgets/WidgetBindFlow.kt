package app.still.widgets

import android.app.Activity
import android.content.ComponentName
import android.content.Intent

/**
 * Stateful bind/configure helpers for [StillWidgetHostController].
 * Callers own Activity Result contracts; this object only decides the next step.
 */
object WidgetBindFlow {
    data class Pending(
        val appWidgetId: Int,
        val providerFlattened: String,
    )

    sealed class NextStep {
        data class LaunchBind(val intent: Intent) : NextStep()
        data class LaunchConfigure(val intent: Intent) : NextStep()
        data class Complete(val placement: WidgetPlacement) : NextStep()
        data class Failed(val releaseId: Int) : NextStep()
    }

    fun begin(
        host: StillWidgetHostController,
        providerFlattened: String,
        span: WidgetSpan = WidgetSpan.Full,
    ): Pair<Pending, NextStep> {
        val component = ComponentName.unflattenFromString(providerFlattened)
            ?: return Pending(0, providerFlattened) to NextStep.Failed(0)
        val id = host.allocateId()
        val pending = Pending(id, providerFlattened)
        val options = host.defaultOptions(span)
        val allowed = host.bindIfAllowed(id, component, options)
        return if (allowed) {
            pending to afterBound(host, pending)
        } else {
            pending to NextStep.LaunchBind(host.createBindIntent(id, component))
        }
    }

    fun afterBindResult(
        host: StillWidgetHostController,
        pending: Pending,
        resultCode: Int,
    ): NextStep {
        if (resultCode != Activity.RESULT_OK) {
            return NextStep.Failed(pending.appWidgetId)
        }
        return afterBound(host, pending)
    }

    fun afterConfigureResult(
        pending: Pending,
        resultCode: Int,
    ): NextStep {
        if (resultCode != Activity.RESULT_OK) {
            return NextStep.Failed(pending.appWidgetId)
        }
        return NextStep.Complete(
            WidgetPlacement(
                appWidgetId = pending.appWidgetId,
                providerFlattened = pending.providerFlattened,
            ),
        )
    }

    private fun afterBound(
        host: StillWidgetHostController,
        pending: Pending,
    ): NextStep {
        val component = ComponentName.unflattenFromString(pending.providerFlattened)
            ?: return NextStep.Failed(pending.appWidgetId)
        val info = host.providerInfo(pending.appWidgetId) ?: host.providerInfo(component)
        val configure = info?.configure
        return if (configure != null) {
            NextStep.LaunchConfigure(host.createConfigureIntent(pending.appWidgetId, configure))
        } else {
            NextStep.Complete(
                WidgetPlacement(
                    appWidgetId = pending.appWidgetId,
                    providerFlattened = pending.providerFlattened,
                ),
            )
        }
    }
}
