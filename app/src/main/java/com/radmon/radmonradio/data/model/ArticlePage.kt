package com.radmon.radmonradio.data.model

/** One page of articles and where the next one starts. */
data class ArticlePage(
    val articles: List<Article>,
    val nextOffset: Long?
)
