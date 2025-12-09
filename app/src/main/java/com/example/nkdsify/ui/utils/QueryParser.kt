package com.example.nkdsify.ui.utils

/**
 * Data class to hold the parsed components of a search query.
 *
 * @property includedTags A set of tags that must be present in the media item.
 * @property excludedTags A set of tags that must NOT be present in the media item.
 * @property searchTerms A list of regular text terms to search for in the media item's name.
 */
data class ParsedQuery(
    val includedTags: Set<String>,
    val excludedTags: Set<String>,
    val searchTerms: List<String>
)

/**
 * Parses a raw search query string into a [ParsedQuery] object.
 *
 * This function implements the logic for a sophisticated search that allows users to filter by
 * including specific tags, excluding others, and searching for plain text in the filename.
 *
 * ### Search Syntax:
 * - **Inclusion:** To require a tag, prefix it with a `+`. For example, `+travel` will only show items with the "travel" tag.
 * - **Exclusion:** To exclude a tag, prefix it with a `-`. For example, `-summer` will hide all items with the "summer" tag.
 * - **Text Search:** Any word without a `+` or `-` prefix is treated as a plain text search term and will be matched against the filename.
 *
 * ### How it Works:
 * The function iterates through each word in the query string, separated by spaces.
 * 1. If a word starts with `+`, its remainder is added to the `includedTags` set.
 * 2. If a word starts with `-`, its remainder is added to the `excludedTags` set.
 * 3. Otherwise, the word is considered a normal search term and added to the `searchTerms` list.
 *
 * ### Example Usage:
 * A query like `+mountains -2022 trip` would be parsed into:
 * - `includedTags` = `{"mountains"}`
 * - `excludedTags` = `{"2022"}`
 * - `searchTerms` = `["trip"]`
 * This would find all items tagged with "mountains", not tagged with "2022", and containing "trip" in their filename.
 *
 * @param query The raw string from the search input field.
 * @return A [ParsedQuery] object containing the structured search criteria.
 */
fun parseQueryString(query: String): ParsedQuery {
    if (query.isBlank()) {
        return ParsedQuery(emptySet(), emptySet(), emptyList())
    }

    val includedTags = mutableSetOf<String>()
    val excludedTags = mutableSetOf<String>()
    val searchTerms = mutableListOf<String>()

    // Split the query by spaces, but handle multiple spaces gracefully
    val parts = query.split(' ').filter { it.isNotBlank() }

    for (part in parts) {
        when {
            part.startsWith('+') && part.length > 1 -> includedTags.add(part.substring(1))
            part.startsWith('-') && part.length > 1 -> excludedTags.add(part.substring(1))
            else -> searchTerms.add(part)
        }
    }
    return ParsedQuery(includedTags, excludedTags, searchTerms)
}
