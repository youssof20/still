package app.still.launcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVisibilityTest {
    private val maps = AppTargetId("com.maps", "com.maps.Main", 0L)
    private val mail = AppTargetId("com.mail", "com.mail.Main", 0L)
    private val phone = AppTargetId("com.phone", "com.phone.Main", 0L)

    private val targets = listOf(
        AppTarget(maps, "Maps"),
        AppTarget(mail, "Mail"),
        AppTarget(phone, "Phone"),
    )

    @Test
    fun browsingHideRemovesFromBrowseButKeepsSearch() {
        val hides = mapOf(maps to HideMode.FromBrowsing)
        assertThat(AppVisibility.filterBrowse(targets, hides).map { it.id })
            .containsExactly(mail, phone)
            .inOrder()
        assertThat(AppVisibility.filterSearch(targets, hides).map { it.id })
            .containsExactly(maps, mail, phone)
            .inOrder()
    }

    @Test
    fun launcherHideRemovesFromBrowseAndSearch() {
        val hides = mapOf(mail to HideMode.FromLauncher)
        assertThat(AppVisibility.filterBrowse(targets, hides).map { it.id })
            .containsExactly(maps, phone)
            .inOrder()
        assertThat(AppVisibility.filterSearch(targets, hides).map { it.id })
            .containsExactly(maps, phone)
            .inOrder()
    }
}
