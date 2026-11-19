/** The JSON from /journalism-all?format=json. Mirrors the iOS app's SquarespaceResponses. */
package com.radmon.radmonradio.data.network

import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.data.model.ArticleTopic
import com.radmon.radmonradio.util.html.ArticleHtmlParser
import java.net.URI
import java.time.Instant

data class SquarespaceCollectionResponse(
    val items: List<SquarespaceItem>?,
    val pagination: SquarespacePagination?
)

data class SquarespacePagination(
    val nextPage: Boolean?,
    val nextPageOffset: Long?
)

// one article as squarespace sends it
data class SquarespaceItem(
    val id: String,
    val title: String?,
    val fullUrl: String?,
    // milliseconds since 1970
    val publishOn: Double?,
    val addedOn: Double?,
    val excerpt: String?,
    val body: String?,
    val assetUrl: String?,
    val author: SquarespaceAuthor?,
    val tags: List<String>?
)

data class SquarespaceAuthor(
    val displayName: String?
)

private val siteUrl = URI("https://www.radiomonash.online")

// the app's own article, null if it has no title
fun SquarespaceItem.toArticle(): Article? {
    val trimmedTitle = title?.trim().orEmpty()
    if (trimmedTitle.isEmpty()) return null

    val bodyHtml = body.orEmpty()
    val wordCount = ArticleHtmlParser.plainText(bodyHtml).split(Regex("\\s+")).count { it.isNotEmpty() }
    // 200 words a minute, at least 1 minute
    val readMinutes = maxOf(1, Math.ceil(wordCount / 200.0).toInt())

    // the author's name, or the station's if there isn't one
    val byline = author?.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Radio Monash"

    val publishedMillis = (publishOn ?: addedOn ?: 0.0).toLong()

    // the link to the article on the website
    val webUrl = fullUrl?.let { runCatching { siteUrl.resolve(it).toString() }.getOrNull() }

    return Article(
        id = id,
        title = trimmedTitle,
        excerpt = ArticleHtmlParser.plainText(excerpt.orEmpty()),
        author = byline,
        publishedAt = Instant.ofEpochMilli(publishedMillis),
        imageUrl = assetUrl,
        webUrl = webUrl,
        bodyHtml = bodyHtml,
        readMinutes = readMinutes,
        topic = ArticleTopic.fromTags(tags.orEmpty())
    )
}
