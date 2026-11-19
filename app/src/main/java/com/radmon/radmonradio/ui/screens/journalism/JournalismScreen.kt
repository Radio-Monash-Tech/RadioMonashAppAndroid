/** The Journalism tab: topic filter chips, a lead feature card, then a list of article rows. */
package com.radmon.radmonradio.ui.screens.journalism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.radmon.radmonradio.JournalismPhase
import com.radmon.radmonradio.JournalismUiState
import com.radmon.radmonradio.JournalismViewModel
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.data.model.ArticleTopic
import com.radmon.radmonradio.util.squarespaceSized
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalismScreen(
    viewModel: JournalismViewModel,
    onOpenArticle: (Article) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        PullToRefreshBox(
            isRefreshing = uiState.phase == JournalismPhase.Loading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { JournalismTopBar() }
                item {
                    TopicChips(uiState.topic, viewModel::selectTopic)
                    Spacer(Modifier.height(12.dp))
                }
                uiState.offlineCopyAt?.let { savedAt ->
                    item {
                        OfflineNote(savedAt, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                journalismContent(uiState, onOpenArticle, onRetry = { viewModel.refresh() }, onLoadMore = { viewModel.loadMore() })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.journalismContent(
    uiState: JournalismUiState,
    onOpenArticle: (Article) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val articles = uiState.articles
    if (articles.isEmpty()) {
        when (val phase = uiState.phase) {
            is JournalismPhase.Idle, is JournalismPhase.Loading -> item { LoadingState() }
            is JournalismPhase.Failed ->
                item {
                    EmptyState(
                        icon = Icons.Filled.WifiOff,
                        title = "Articles unavailable",
                        description = phase.message,
                        actionLabel = "Try again",
                        onAction = onRetry
                    )
                }
            is JournalismPhase.Loaded ->
                item {
                    EmptyState(
                        icon = Icons.Filled.Newspaper,
                        title = "No Articles Yet",
                        description = "New pieces from the Radio Monash newsroom will appear here."
                    )
                }
        }
        return
    }

    item {
        LeadCard(
            article = articles.first(),
            onClick = { onOpenArticle(articles.first()) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
    items(articles.drop(1), key = { it.id }) { article ->
        ArticleRow(article, onClick = { onOpenArticle(article) })
        // load the next page when the last article appears
        if (article.id == articles.last().id) {
            LaunchedEffect(article.id) { onLoadMore() }
        }
    }
    if (uiState.isLoadingMore) {
        item {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else if (uiState.loadMoreFailed) {
        item {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Button(onClick = onLoadMore) { Text("Load More Articles") }
            }
        }
    }
}

// Top bar: title and subtitle

@Composable
private fun JournalismTopBar() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Journalism",
                color = cs.onBackground,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

//Topic filter chips (M3 FilterChip, 8 dp corners, check when selected)

@Composable
private fun TopicChips(
    selected: ArticleTopic?,
    onSelect: (ArticleTopic?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { TopicChip("All", on = selected == null, onClick = { onSelect(null) }) }
        items(ArticleTopic.entries) { topic ->
            TopicChip(topic.label, on = selected == topic, onClick = { onSelect(topic) })
        }
    }
}

@Composable
private fun TopicChip(label: String, on: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FilterChip(
        selected = on,
        onClick = onClick,
        label = { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
        leadingIcon = if (on) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = cs.secondaryContainer,
            selectedLabelColor = cs.onSecondaryContainer,
            selectedLeadingIconColor = cs.onSecondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = on,
            borderColor = cs.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

// Lead feature card

@Composable
private fun LeadCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Latest: ${article.title}, by ${article.author}" },
        shape = RoundedCornerShape(24.dp),
        color = cs.surfaceContainer
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(cs.surfaceContainerHigh)
            ) {
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl.squarespaceSized(1000),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    "LATEST",
                    color = cs.onPrimary,
                    fontSize = 11.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.primary)
                        .padding(horizontal = 10.dp)
                )
            }
            Column(Modifier.padding(16.dp)) {
                article.topic?.let {
                    Text(
                        it.label.uppercase(),
                        color = cs.primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                }
                Text(
                    article.title,
                    color = cs.onSurface,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (article.excerpt.isNotEmpty()) {
                    Text(
                        article.excerpt,
                        color = cs.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                ArticleByline(
                    article,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

// Article row: topic, two-line title, byline, 72 dp thumbnail

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${article.topic?.label.orEmpty()}: ${article.title}, by ${article.author}" }
    ) {
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                article.topic?.let {
                    Text(
                        it.label.uppercase(),
                        color = cs.primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                }
                Text(
                    article.title,
                    color = cs.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ArticleByline(article, modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainerHigh)
            ) {
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl.squarespaceSized(300),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// Loading, empty and offline states

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 16.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = cs.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Text(
            description,
            color = cs.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
private fun OfflineNote(savedAt: Instant, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Showing a saved copy from ${formatter.format(savedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
