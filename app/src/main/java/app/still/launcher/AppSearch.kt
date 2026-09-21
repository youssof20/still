package app.still.launcher

/**
 * Framework-free search ranking for launchable targets.
 *
 * Matches against the alias when present and always against the original label, so aliasing
 * cannot erase original-label indexing. Never launches an app.
 */
object AppSearch {
    enum class MatchKind {
        Exact,
        Prefix,
        Substring,
        None,
    }

    data class RankedTarget(
        val target: AppTarget,
        val kind: MatchKind,
        val matchedOnAlias: Boolean,
    )

    fun filterAndRank(query: String, targets: List<AppTarget>): List<AppTarget> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) {
            return targets.sortedWith(catalogOrder())
        }

        return targets
            .mapNotNull { target -> rank(normalizedQuery, target) }
            .sortedWith(
                compareBy<RankedTarget> { it.kind.ordinal }
                    .thenByDescending { it.matchedOnAlias }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.target.displayLabel }
                    .thenBy { it.target.id.userSerialNumber }
                    .thenBy { it.target.id.packageName }
                    .thenBy { it.target.id.activityClassName },
            )
            .map { it.target }
    }

    fun rank(normalizedQuery: String, target: AppTarget): RankedTarget? {
        val alias = target.alias?.let(::normalize).orEmpty()
        val original = normalize(target.originalLabel)

        val aliasKind = matchKind(normalizedQuery, alias)
        val originalKind = matchKind(normalizedQuery, original)

        return when {
            aliasKind != MatchKind.None &&
                (originalKind == MatchKind.None || aliasKind.ordinal <= originalKind.ordinal) -> {
                RankedTarget(target, aliasKind, matchedOnAlias = true)
            }
            originalKind != MatchKind.None -> {
                RankedTarget(target, originalKind, matchedOnAlias = false)
            }
            else -> null
        }
    }

    fun normalize(value: String): String = value.trim().lowercase()

    private fun matchKind(query: String, candidate: String): MatchKind {
        if (candidate.isEmpty() || query.isEmpty()) return MatchKind.None
        return when {
            candidate == query -> MatchKind.Exact
            candidate.startsWith(query) -> MatchKind.Prefix
            candidate.contains(query) -> MatchKind.Substring
            else -> MatchKind.None
        }
    }

    fun catalogOrder(): Comparator<AppTarget> =
        Comparator { left, right ->
            val byLabel = String.CASE_INSENSITIVE_ORDER.compare(left.displayLabel, right.displayLabel)
            if (byLabel != 0) return@Comparator byLabel
            val bySerial = left.id.userSerialNumber.compareTo(right.id.userSerialNumber)
            if (bySerial != 0) return@Comparator bySerial
            val byPackage = left.id.packageName.compareTo(right.id.packageName)
            if (byPackage != 0) return@Comparator byPackage
            left.id.activityClassName.compareTo(right.id.activityClassName)
        }
}
