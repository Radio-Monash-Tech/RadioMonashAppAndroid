/** Draws one parsed block of an article's body: paragraph, heading, quote, list item, or image. */
package com.radmon.radmonradio.ui.screens.journalism.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.radmon.radmonradio.util.html.ArticleBlock
import com.radmon.radmonradio.util.squarespaceSized

@Composable
fun ArticleBlockView(block: ArticleBlock) {
    val cs = MaterialTheme.colorScheme
    when (block) {
        is ArticleBlock.Paragraph ->
            Text(
                block.text,
                color = cs.onSurface,
                fontFamily = FontFamily.Serif,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        is ArticleBlock.Heading -> {
            val (fontSize, lineHeight) = headingSizes(block.level)
            Text(
                block.text,
                color = cs.onSurface,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                lineHeight = lineHeight,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
        is ArticleBlock.Quote ->
            Row(modifier = Modifier.padding(bottom = 16.dp).height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(cs.primary)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    block.text,
                    color = cs.onSurface,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 19.sp,
                    lineHeight = 26.sp
                )
            }
        is ArticleBlock.ListItem ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    block.marker,
                    color = cs.onSurface,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    block.text,
                    color = cs.onSurface,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
            }
        is ArticleBlock.Image ->
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                AsyncImage(
                    model = block.url.squarespaceSized(1000),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceContainerHigh)
                )
                block.caption?.let { caption ->
                    Text(
                        caption,
                        color = cs.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
    }
}

// level 1 is the biggest
private fun headingSizes(level: Int): Pair<TextUnit, TextUnit> = when (level) {
    1 -> 24.sp to 30.sp
    2 -> 21.sp to 27.sp
    else -> 18.sp to 24.sp
}
