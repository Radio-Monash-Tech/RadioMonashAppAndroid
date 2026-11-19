/** Parses an article's HTML body into blocks, off the main thread, once per article. */
package com.radmon.radmonradio.ui.screens.journalism.article

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.util.html.ArticleBlock
import com.radmon.radmonradio.util.html.ArticleHtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Null while [article]'s body hasn't been parsed yet (or there's no article to parse). */
@Composable
fun rememberArticleBlocks(article: Article?): List<ArticleBlock>? {
    var blocks by remember(article?.id) { mutableStateOf<List<ArticleBlock>?>(null) }
    LaunchedEffect(article?.id, article?.bodyHtml) {
        val html = article?.bodyHtml
        if (html != null) {
            blocks = withContext(Dispatchers.Default) { ArticleHtmlParser.blocks(html) }
        }
    }
    return blocks
}
