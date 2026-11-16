package com.example.neatgrid.data

import java.text.Collator

interface LibraryEntry {
    val packageName: String
    val label: String
    val platform: String?
}

data class LibrarySections<T>(val visible: List<T>, val hidden: List<T>)

fun <T : LibraryEntry> arrangeLibrary(
    items: List<T>,
    hiddenPackages: Set<String>,
    sortMode: LibrarySortMode,
    collator: Collator = Collator.getInstance()
): LibrarySections<T> {
    val comparator = libraryComparator<T>(sortMode, collator)
    val (hidden, visible) = items.partition { it.packageName in hiddenPackages }
    return LibrarySections(
        visible = visible.sortedWith(comparator),
        hidden = hidden.sortedWith(comparator)
    )
}

private fun <T : LibraryEntry> libraryComparator(
    sortMode: LibrarySortMode,
    collator: Collator
): Comparator<T> {
    val byTitle = Comparator<T> { left, right -> collator.compare(left.label, right.label) }
    return when (sortMode) {
        LibrarySortMode.TITLE_ASCENDING -> byTitle
        LibrarySortMode.TITLE_DESCENDING -> byTitle.reversed()
        LibrarySortMode.PLATFORM -> compareBy<T> { it.platform == null }
            .thenComparator { left, right ->
                collator.compare(left.platform.orEmpty(), right.platform.orEmpty())
            }
            .then(byTitle)
    }
}
