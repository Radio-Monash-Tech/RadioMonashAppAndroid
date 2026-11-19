/** Topic label, title, and byline at the top of the article reader. */
package com.radmon.radmonradio.ui.screens.journalism.article

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.ui.screens.journalism.ArticleByline

@Composable
fun ArticleHeader(article: Article) {
    val cs = MaterialTheme.colorScheme
    Column {
        article.topic?.let {
            Text(
                it.label.uppercase(),
                color = cs.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Text(
            article.title,
            color = cs.onSurface,
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Column(Modifier.padding(top = 16.dp, bottom = 20.dp)) {
            Text(article.author, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            ArticleByline(article, modifier = Modifier)
            Text(
                "${article.readMinutes} min read",
                color = cs.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
