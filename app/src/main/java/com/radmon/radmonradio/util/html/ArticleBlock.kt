/** One chunk of an article, ready for ArticleScreen to draw. */
package com.radmon.radmonradio.util.html

import androidx.compose.ui.text.AnnotatedString

sealed class ArticleBlock {
    data class Paragraph(val text: AnnotatedString) : ArticleBlock()
    // level 1 is the biggest
    data class Heading(val text: AnnotatedString, val level: Int) : ArticleBlock()
    data class Quote(val text: AnnotatedString) : ArticleBlock()
    // marker is "•" or "1.", "2." ...
    data class ListItem(val text: AnnotatedString, val marker: String) : ArticleBlock()
    // caption comes from the figcaption after the image
    data class Image(val url: String, val caption: AnnotatedString? = null) : ArticleBlock()
}
