package app.still

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class OfflineManifestTest {
    @Test
    fun sourceManifestDoesNotDeclareInternetPermission() {
        val manifest = resolveManifest().readText()
        val internetUsesPermission =
            Regex("""<uses-permission[^>]*android:name="android\.permission\.INTERNET"""")
        assertThat(internetUsesPermission.containsMatchIn(manifest)).isFalse()
        assertThat(manifest).contains("android.intent.category.HOME")
        assertThat(manifest).contains("launchMode=\"singleTask\"")
    }

    private fun resolveManifest(): File {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("AndroidManifest.xml not found from ${File(".").absolutePath}")
    }
}
