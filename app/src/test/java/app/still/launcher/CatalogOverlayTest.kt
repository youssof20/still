package app.still.launcher

import app.still.prefs.LauncherPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogOverlayTest {
    private val id = AppTargetId("com.mail", "com.mail.Main", 0L)
    private val missingId = AppTargetId("com.gone", "com.gone.Main", 0L)

    @Test
    fun aliasOverlayKeepsOriginalLabel() {
        val catalog = listOf(AppTarget(id, originalLabel = "Mail"))
        val prefs = LauncherPreferences(aliases = mapOf(id to "Inbox"))
        val overlaid = CatalogOverlay.apply(catalog, prefs).single()
        assertThat(overlaid.alias).isEqualTo("Inbox")
        assertThat(overlaid.originalLabel).isEqualTo("Mail")
        assertThat(overlaid.displayLabel).isEqualTo("Inbox")
    }

    @Test
    fun missingFavoriteRemainsRecoverableSlot() {
        val prefs = LauncherPreferences(favoriteIds = listOf(id, missingId))
        val catalog = listOf(AppTarget(id, "Mail"))
        val favorites = CatalogOverlay.resolveFavorites(prefs, catalog)
        assertThat(favorites).hasSize(2)
        assertThat(favorites[0].missing).isFalse()
        assertThat(favorites[1].missing).isTrue()
        assertThat(favorites[1].id).isEqualTo(missingId)
    }
}
