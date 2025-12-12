package com.example.nkdsify.ui.utils

/**
 * Data class to hold the parsed components of a search query.
 *
 * @property includedTagGroups A list of tag groups. Media must match all groups (AND).
 *                             Within a group, media must match at least one tag (OR).
 * @property excludedTags A set of tags that must NOT be present in the media item.
 * @property searchTerms A list of regular text terms to search for in the media item's name.
 */
data class ParsedQuery(
    val includedTagGroups: List<Set<String>>,
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
 * - **AND Group (starts a new group):** To require a tag or a group of tags, prefix the first tag with `+`.
 *   e.g., `+travel` looks for "travel". `+travel +france` looks for "travel" AND "france".
 * - **OR Tag (adds to existing group):** To add an alternative tag to the last group, prefix it with `=`.
 *   e.g., `+travel =vacation` looks for "travel" OR "vacation".
 * - **Exclusion:** To exclude a tag, prefix it with a `-`. e.g., `-summer`.
 * - **Text Search:** Any word without a prefix is a text search term.
 *
 * ### How it Works:
 * - `+` starts a new AND-connected group of OR-tags.
 * - `=` adds a tag to the most recent OR-group.
 * - `-` adds a tag to the exclusion list.
 * - Other words are added to the text search list.
 *
 * ### Example Usage:
 * A query like `+mountains =hills -2022 trip` would be parsed into:
 * - `includedTagGroups` = `[["mountains", "hills"]]`
 * - `excludedTags` = `{"2022"}`
 * - `searchTerms` = `["trip"]`
 * This finds items tagged with "mountains" OR "hills", NOT tagged with "2022", and containing "trip" in their filename.
 *
 * A query `+summer +france` is parsed as:
 * - `includedTagGroups` = `[["summer"], ["france"]]`
 * which means items must have tag "summer" AND tag "france".
 *
 * @param query The raw string from the search input field.
 * @return A [ParsedQuery] object containing the structured search criteria.
 */
fun parseQueryString(query: String): ParsedQuery {
    if (query.isBlank()) {
        return ParsedQuery(emptyList(), emptySet(), emptyList())
    }

    val includedTagGroups = mutableListOf<MutableSet<String>>()
    val excludedTags = mutableSetOf<String>()
    val searchTerms = mutableListOf<String>()

    // Split the query by spaces, but handle multiple spaces gracefully
    val parts = query.split(' ').filter { it.isNotBlank() }

    for (part in parts) {
        when {
            part.startsWith('+') && part.length > 1 -> {
                includedTagGroups.add(mutableSetOf(part.substring(1)))
            }
            part.startsWith('=') && part.length > 1 -> {
                if (includedTagGroups.isEmpty()) {
                    includedTagGroups.add(mutableSetOf(part.substring(1)))
                } else {
                    includedTagGroups.last().add(part.substring(1))
                }
            }
            part.startsWith('-') && part.length > 1 -> excludedTags.add(part.substring(1))
            else -> searchTerms.add(part)
        }
    }
    return ParsedQuery(includedTagGroups, excludedTags, searchTerms)
}
