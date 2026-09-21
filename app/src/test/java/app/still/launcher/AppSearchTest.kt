package app.still.launcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSearchTest {
    private val camera = AppTarget(
        id = AppTargetId("com.android.camera", "com.android.camera.CameraActivity", 0L),
        originalLabel = "Camera",
    )
    private val phone = AppTarget(
        id = AppTargetId("com.android.dialer", "com.android.dialer.DialtactsActivity", 0L),
        originalLabel = "Phone",
    )
    private val maps = AppTarget(
        id = AppTargetId("com.example.maps", "com.example.maps.Main", 0L),
        originalLabel = "Maps",
        alias = "Navigation",
    )
    private val mailWork = AppTarget(
        id = AppTargetId("com.example.mail", "com.example.mail.Main", 10L),
        originalLabel = "Mail",
    )
    private val mailPersonal = AppTarget(
        id = AppTargetId("com.example.mail", "com.example.mail.Main", 0L),
        originalLabel = "Mail",
    )

    @Test
    fun emptyQueryReturnsDeterministicCatalogOrder() {
        val result = AppSearch.filterAndRank("", listOf(phone, camera, maps))
        // Alias becomes the display label used for empty-query ordering.
        assertThat(result.map { it.displayLabel })
            .containsExactly("Camera", "Navigation", "Phone")
            .inOrder()
    }

    @Test
    fun exactBeatsPrefixBeatsSubstring() {
        val calendar = AppTarget(
            id = AppTargetId("com.example.cal", "com.example.cal.Main", 0L),
            originalLabel = "Calendar",
        )
        val cal = AppTarget(
            id = AppTargetId("com.example.calapp", "com.example.calapp.Main", 0L),
            originalLabel = "Cal",
        )
        val local = AppTarget(
            id = AppTargetId("com.example.local", "com.example.local.Main", 0L),
            originalLabel = "Local Notes",
        )
        val ranked = AppSearch.filterAndRank("cal", listOf(local, calendar, cal))
        assertThat(ranked.map { it.originalLabel }).containsExactly("Cal", "Calendar", "Local Notes").inOrder()
    }

    @Test
    fun aliasMatchKeepsOriginalLabelIndexed() {
        val rankedByAlias = AppSearch.filterAndRank("nav", listOf(maps, camera))
        assertThat(rankedByAlias).containsExactly(maps)

        val rankedByOriginal = AppSearch.filterAndRank("maps", listOf(maps, camera))
        assertThat(rankedByOriginal).containsExactly(maps)
        assertThat(rankedByOriginal.single().originalLabel).isEqualTo("Maps")
        assertThat(rankedByOriginal.single().alias).isEqualTo("Navigation")
    }

    @Test
    fun singleResultDoesNotImplyLaunch_filterOnlyReturnsList() {
        val ranked = AppSearch.filterAndRank("camera", listOf(camera, phone))
        assertThat(ranked).hasSize(1)
        // Launch is a separate explicit call site; search returns data only.
    }

    @Test
    fun profileIdentityBreaksTiesDeterministically() {
        val ranked = AppSearch.filterAndRank("mail", listOf(mailWork, mailPersonal))
        assertThat(ranked.map { it.id.userSerialNumber }).containsExactly(0L, 10L).inOrder()
    }

    @Test
    fun nonLatinLabelsAreNotStripped() {
        val notes = AppTarget(
            id = AppTargetId("com.example.notes", "com.example.notes.Main", 0L),
            originalLabel = "メモ",
        )
        val ranked = AppSearch.filterAndRank("メモ", listOf(notes, camera))
        assertThat(ranked).containsExactly(notes)
    }
}
