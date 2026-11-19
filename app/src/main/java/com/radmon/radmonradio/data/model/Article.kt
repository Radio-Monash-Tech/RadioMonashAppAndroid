/** One journalism article from the Radio Monash newsroom. Mirrors the iOS app's Article. */
package com.radmon.radmonradio.data.model

import java.time.Instant

data class Article(
    val id: String,
    val title: String,
    val excerpt: String = "",
    val author: String,
    val publishedAt: Instant,
    val imageUrl: String? = null,
    val webUrl: String? = null,
    val bodyHtml: String = "",
    val readMinutes: Int = 1,
    val topic: ArticleTopic? = null
) {
    /** "Christopher Hall" becomes "CH" */
    val authorInitials: String
        get() = author.split(" ")
            .mapNotNull { it.firstOrNull() }
            .take(2)
            .joinToString("")
}
