package app.still.widgets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetPlacementRepositoryTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val list = listOf(
            WidgetPlacement(1, "com.example/.Widget", WidgetSpan.Half, WidgetAlignment.Center),
            WidgetPlacement(2, "com.other/.Clock", WidgetSpan.Full, WidgetAlignment.Stretch),
        )
        val encoded = WidgetPlacementRepository.encodeList(list)
        val decoded = WidgetPlacementRepository.decodeList(encoded)
        assertThat(decoded).isEqualTo(list)
    }

    @Test
    fun decodeEmptyAndCorrupt() {
        assertThat(WidgetPlacementRepository.decodeList(null)).isEmpty()
        assertThat(WidgetPlacementRepository.decodeList("")).isEmpty()
        assertThat(WidgetPlacementRepository.decodeList("not-a-record")).isEmpty()
        assertThat(WidgetPlacementRepository.decodeList("0\u0001com.example/.W")).isEmpty()
    }

    @Test
    fun decodeDefaultsMissingOptionalFields() {
        val raw = "7\u0001com.example/.Widget"
        val decoded = WidgetPlacementRepository.decodeList(raw)
        assertThat(decoded).containsExactly(
            WidgetPlacement(7, "com.example/.Widget", WidgetSpan.Full, WidgetAlignment.Stretch),
        )
    }
}
