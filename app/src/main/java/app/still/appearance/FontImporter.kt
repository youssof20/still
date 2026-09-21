package app.still.appearance

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies user-picked TTF/OTF fonts into app-private storage.
 * Invalid or oversized files are rejected without changing the active selection.
 */
class FontImporter(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val fontsDir = File(appContext.filesDir, "imported_fonts").apply { mkdirs() }

    fun listImported(): List<ImportedFontInfo> {
        return fontsDir.listFiles()
            ?.filter { it.isFile && looksLikeFont(it.name) }
            ?.map { file ->
                ImportedFontInfo(
                    id = file.nameWithoutExtension,
                    displayName = readDisplayName(file),
                    fileName = file.name,
                )
            }
            ?.sortedBy { it.displayName.lowercase() }
            .orEmpty()
    }

    fun resolveFile(id: String): File? {
        val match = fontsDir.listFiles()?.firstOrNull {
            it.nameWithoutExtension == id && looksLikeFont(it.name)
        }
        return match?.takeIf { it.isFile && it.length() > 0L }
    }

    fun loadTypeface(id: String): Typeface? {
        val file = resolveFile(id) ?: return null
        return runCatching { Typeface.createFromFile(file) }.getOrNull()
    }

    fun delete(id: String): Boolean {
        val file = resolveFile(id) ?: return false
        return file.delete()
    }

    fun importFromUri(uri: Uri): Result<ImportedFontInfo> = runCatching {
        val resolver = appContext.contentResolver
        val type = resolver.getType(uri).orEmpty()
        require(typeIsFont(type) || uriLastSegmentLooksLikeFont(uri)) {
            "Not a font file"
        }
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        require(size in 1..MAX_BYTES) { "Font file is empty or larger than ${MAX_BYTES / (1024 * 1024)} MB" }

        val id = UUID.randomUUID().toString().replace("-", "")
        val ext = when {
            type.contains("otf", ignoreCase = true) || uri.toString().endswithIgnoreCase(".otf") -> "otf"
            else -> "ttf"
        }
        val dest = File(fontsDir, "$id.$ext")
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open font")

        require(dest.length() in 1..MAX_BYTES) {
            dest.delete()
            "Copied font failed size check"
        }
        // Validate parser acceptance before keeping the file.
        val face = runCatching { Typeface.createFromFile(dest) }.getOrNull()
        if (face == null) {
            dest.delete()
            error("Font could not be opened. Try another file.")
        }
        ImportedFontInfo(
            id = id,
            displayName = readDisplayName(dest),
            fileName = dest.name,
        )
    }

    private fun readDisplayName(file: File): String {
        // Filenames alone are weak; without a full font parser use a cleaned id label.
        // Real family name would require a larger decoder dependency.
        return "Imported ${file.nameWithoutExtension.take(8)}"
    }

    private fun looksLikeFont(name: String): Boolean =
        name.endswithIgnoreCase(".ttf") || name.endswithIgnoreCase(".otf")

    private fun typeIsFont(type: String): Boolean =
        type.contains("font", ignoreCase = true) ||
            type == "application/x-font-ttf" ||
            type == "application/x-font-otf" ||
            type == "application/octet-stream"

    private fun uriLastSegmentLooksLikeFont(uri: Uri): Boolean {
        val name = uri.lastPathSegment.orEmpty()
        return looksLikeFont(name)
    }

    private fun String.endswithIgnoreCase(suffix: String): Boolean =
        this.endsWith(suffix, ignoreCase = true)

    companion object {
        const val MAX_BYTES = 5L * 1024L * 1024L
    }
}
