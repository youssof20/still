package app.still.launcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppTargetIdTest {
    @Test
    fun equalityUsesComponentAndProfileNotLabel() {
        val personal = AppTargetId("com.example", "com.example.Main", 0L)
        val work = AppTargetId("com.example", "com.example.Main", 10L)
        val renamedSameId = AppTarget(
            id = personal,
            originalLabel = "Mail",
            alias = "Inbox",
        )

        assertThat(personal).isNotEqualTo(work)
        assertThat(renamedSameId.id).isEqualTo(personal)
        assertThat(personal.flattenedComponent()).isEqualTo("com.example/com.example.Main")
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankPackageRejected() {
        AppTargetId("", "com.example.Main", 0L)
    }
}
