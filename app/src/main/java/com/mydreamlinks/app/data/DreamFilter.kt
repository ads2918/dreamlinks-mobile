package com.mydreamlinks.app.data

import androidx.compose.runtime.saveable.listSaver
import java.net.URLEncoder

/**
 * What a dream list is narrowed and sorted by, sent as query parameters (see the API's
 * DreamModel::filtered). Blank values mean "any". [from] is feed only, [visibility] journal only.
 */
data class DreamFilter(
    val q: String = "",
    /** days back: 0 (any time), 7, 30 or 365 */
    val since: Int = 0,
    /** newest, oldest or resonance */
    val sort: String = "newest",
    /** "links" keeps the feed to linked dreamers */
    val from: String = "",
    /** public, friends or private */
    val visibility: String = "",
) {
    val isDefault get() = this == DreamFilter()

    fun query(): String = buildString {
        if (q.isNotBlank()) append("&q=").append(URLEncoder.encode(q.trim(), "UTF-8"))
        if (since > 0) append("&since=").append(since)
        if (sort != "newest") append("&sort=").append(sort)
        if (from.isNotEmpty()) append("&from=").append(from)
        if (visibility.isNotEmpty()) append("&visibility=").append(visibility)
    }

    companion object {
        /** Keeps the filter across rotation and tab switches. */
        val Saver = listSaver<DreamFilter, Any>(
            save = { listOf(it.q, it.since, it.sort, it.from, it.visibility) },
            restore = { DreamFilter(it[0] as String, it[1] as Int, it[2] as String, it[3] as String, it[4] as String) },
        )
    }
}
