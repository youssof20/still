package app.still.launcher

/**
 * Hide modes are launcher UX only — not Android security.
 * Neither blocks Settings, notifications, links, or another launcher.
 */
enum class HideMode {
    None,
    /** Absent from ordinary app lists; still searchable. */
    FromBrowsing,
    /** Absent from normal lists and search; managed in Settings > Hidden apps. */
    FromLauncher,
}

object AppVisibility {
    fun visibleInBrowse(targetId: AppTargetId, hides: Map<AppTargetId, HideMode>): Boolean {
        return when (hides[targetId] ?: HideMode.None) {
            HideMode.None -> true
            HideMode.FromBrowsing, HideMode.FromLauncher -> false
        }
    }

    fun visibleInSearch(targetId: AppTargetId, hides: Map<AppTargetId, HideMode>): Boolean {
        return when (hides[targetId] ?: HideMode.None) {
            HideMode.None, HideMode.FromBrowsing -> true
            HideMode.FromLauncher -> false
        }
    }

    fun filterBrowse(targets: List<AppTarget>, hides: Map<AppTargetId, HideMode>): List<AppTarget> =
        targets.filter { visibleInBrowse(it.id, hides) }

    fun filterSearch(targets: List<AppTarget>, hides: Map<AppTargetId, HideMode>): List<AppTarget> =
        targets.filter { visibleInSearch(it.id, hides) }
}
