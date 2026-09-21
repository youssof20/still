package app.still.launcher

import app.still.prefs.LauncherPreferences

object CatalogOverlay {
    fun apply(targets: List<AppTarget>, prefs: LauncherPreferences): List<AppTarget> =
        targets.map { target ->
            val alias = prefs.aliases[target.id]
            if (alias.isNullOrBlank()) target else target.copy(alias = alias)
        }

    fun resolveFavorites(
        prefs: LauncherPreferences,
        catalog: List<AppTarget>,
    ): List<FavoriteEntry> {
        val byId = catalog.associateBy { it.id }
        return prefs.favoriteIds.map { id ->
            val target = byId[id]
            FavoriteEntry(
                id = id,
                target = target,
                missing = target == null,
            )
        }
    }
}

data class FavoriteEntry(
    val id: AppTargetId,
    val target: AppTarget?,
    val missing: Boolean,
) {
    val displayLabel: String
        get() = target?.displayLabel ?: id.flattenedComponent()
}
