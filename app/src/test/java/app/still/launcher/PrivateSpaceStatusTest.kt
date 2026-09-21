package app.still.launcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivateSpaceStatusTest {
    @Test
    fun privateSpaceRemainsUnsupportedUntilLifecycleTestsPass() {
        assertThat(PrivateSpaceStatus.SUPPORTED).isFalse()
    }
}
