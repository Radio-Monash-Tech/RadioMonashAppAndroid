/** shared between the Journalism list and the article reader. */
package com.radmon.radmonradio.ui.screens.journalism

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.util.MelbourneTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val bylineDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(MelbourneTime.zone)

@Composable
fun ArticleByline(article: Article, modifier: Modifier = Modifier) {
    Text(
        "${article.author} · ${bylineDateFormatter.format(article.publishedAt)}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
