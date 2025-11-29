/**
 * Favorites screen displaying user-liked tracks with swipe-to-delete.
 */
package com.radmon.radmonradio.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radmon.radmonradio.RadioViewModel
import kotlinx.coroutines.launch
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onNavigateBack: () -> Unit, viewModel: RadioViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("favorites_filters", android.content.Context.MODE_PRIVATE) }
    var artistFilter by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.Recent) }
    LaunchedEffect(Unit) {
        artistFilter = prefs.getString("artist", null)
        sortOption = prefs.getString("sortOption", SortOption.Recent.name)?.let { SortOption.valueOf(it) } ?: SortOption.Recent
    }
    LaunchedEffect(artistFilter, sortOption) {
        prefs.edit()
            .putString("artist", artistFilter)
            .putString("sortOption", sortOption.name)
            .apply()
    }
    val artists = remember(favorites) { favorites.map { it.artist }.distinct().sorted() }
    val filtered = favorites.filter { ft ->
        artistFilter == null || ft.artist.equals(artistFilter, true)
    }
    val sorted = when (sortOption) {
        SortOption.Recent -> filtered.sortedByDescending { it.likedAt }
        SortOption.Oldest -> filtered.sortedBy { it.likedAt }
        SortOption.TitleAZ -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        SortOption.ArtistAZ -> filtered.sortedBy { it.artist.lowercase(Locale.getDefault()) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val total = favorites.size
                    val count = sorted.size
                    Text(
                        text = if (total == count) "Liked Songs ($count)" else "Liked Songs ($count of $total)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {  }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = artistFilter ?: "All Artists",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Artist") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).widthIn(min = 160.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("All Artists") }, onClick = {
                            artistFilter = null; expanded = false
                        })
                        artists.forEach { artist ->
                            DropdownMenuItem(text = { Text(artist) }, onClick = { artistFilter = artist; expanded = false })
                        }
                    }
                }
                var sortExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = !sortExpanded }) {
                    OutlinedTextField(
                        value = when (sortOption) {
                            SortOption.Recent -> "Recent"
                            SortOption.Oldest -> "Oldest"
                            SortOption.TitleAZ -> "Title A–Z"
                            SortOption.ArtistAZ -> "Artist A–Z"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sort") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).widthIn(min = 140.dp)
                    )
                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        DropdownMenuItem(text = { Text("Recent") }, onClick = { sortOption = SortOption.Recent; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Oldest") }, onClick = { sortOption = SortOption.Oldest; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Title A–Z") }, onClick = { sortOption = SortOption.TitleAZ; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Artist A–Z") }, onClick = { sortOption = SortOption.ArtistAZ; sortExpanded = false })
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sorted, key = { it.title + it.artist + it.likedAt }) { ft ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                viewModel.toggleFavorite(ft)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Removed from Liked Songs",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.toggleFavorite(ft)
                                    }
                                }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            val color = Color(0xFFB00020)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        },
                        content = {
                            ListItem(
                                headlineContent = { Text("${ft.title} - ${ft.artist}") }
                            )
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        }
    }
}
private enum class SortOption { Recent, Oldest, TitleAZ, ArtistAZ }
