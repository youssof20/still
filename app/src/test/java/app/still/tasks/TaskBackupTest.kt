package app.still.tasks

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TaskBackupTest {
    private val sample = listOf(
        TaskEntity(
            id = "a",
            title = "Buy milk",
            position = 0,
            createdAt = 1,
            updatedAt = 2,
            completedAt = null,
            deletedAt = null,
        ),
        TaskEntity(
            id = "b",
            title = "Read Tomorrow, and Tomorrow, and Tomorrow",
            position = 1,
            createdAt = 3,
            updatedAt = 4,
            completedAt = 5,
            deletedAt = null,
        ),
    )

    @Test
    fun jsonRoundTripPreservesFields() {
        val json = TaskBackup.toJson(sample)
        val parsed = TaskBackup.parseJson(json).getOrThrow()
        assertThat(parsed).isEqualTo(sample)
    }

    @Test
    fun invalidJsonDoesNotThrowFromResult() {
        val result = TaskBackup.parseJson("{not-json")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun wrongFormatRejected() {
        val result = TaskBackup.parseJson("""{"format":"other","version":1,"tasks":[]}""")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun markdownListsActiveAndDone() {
        val md = TaskBackup.toMarkdown(sample)
        assertThat(md).contains("- [ ] Buy milk")
        assertThat(md).contains("- [x] Read Tomorrow, and Tomorrow, and Tomorrow")
        assertThat(md).contains("## Done")
    }
}
