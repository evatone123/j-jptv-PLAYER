package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.ui.components.VideoPlayer
import com.example.ui.viewmodel.IptvViewModel
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.filteredChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val statusMessage by viewModel.refreshStatusMessage.collectAsState()
    
    val activeChannel by viewModel.activeChannel.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    if (isFullscreen) {
        BackHandler {
            viewModel.setFullscreen(false)
        }
        activeChannel?.let { channel ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayer(
                    url = channel.streamUrl,
                    modifier = Modifier.fillMaxSize(),
                    isMuted = false
                )

                // Back / Exit Fullstream Button
                IconButton(
                    onClick = { viewModel.setFullscreen(false) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }

                // Header Overlay details
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } ?: run {
            viewModel.setFullscreen(false)
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // #1C1B1F
        ) {
            // --- 1. Video Player Slot ---
            activeChannel?.let { channel ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayer(
                        url = channel.streamUrl,
                        modifier = Modifier.fillMaxSize(),
                        isMuted = false
                    )

                    // Overlay Fullscreen toggle button
                    IconButton(
                        onClick = { viewModel.setFullscreen(true) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                // Channel Quick Details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // #2B2930
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.logoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(channel.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(Icons.Default.Tv, contentDescription = "TV", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = channel.groupTitle ?: "Uncategorized",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = { viewModel.toggleFavorite(channel) }) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (channel.isFavorite) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                }

                // Embedded Live program status
                ChannelScheduleWidget(viewModel = viewModel, channel = channel)
            } ?: run {
                // Placeholder when no channels available
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant), // #2B2930
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "No Stream",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a channel to play",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // --- 2. Browsing & Channel Selection List ---
            // Search & Refresh status bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search channels...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Horizontal Category Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { viewModel.selectGroup(null) },
                        label = { Text("All Channels") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedGroup == "Favorites",
                        onClick = { viewModel.selectGroup("Favorites") },
                        label = { Text("★ Favorites") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = Color(0xFFFFB300),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = Color.Black
                        ),
                        border = null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedGroup == "Recents",
                        onClick = { viewModel.selectGroup("Recents") },
                        label = { Text("🕒 Recents") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = null
                    )
                }
                items(groups) { groupName ->
                    FilterChip(
                        selected = selectedGroup == groupName,
                        onClick = { viewModel.selectGroup(groupName) },
                        label = { Text(groupName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                }
            }

            // Active List Display
            val activeChannelsToShow = when (selectedGroup) {
                "Favorites" -> favoriteChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
                "Recents" -> recentChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
                else -> channels
            }

            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = statusMessage ?: "Updating playlists...",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (activeChannelsToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Empty",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No channels match your search", color = Color.Gray, fontSize = 14.sp)
                        Text(text = "Select another group or refresh streams.", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(activeChannelsToShow) { channel ->
                        ChannelListRow(
                            viewModel = viewModel,
                            channel = channel,
                            isSelected = activeChannel?.id == channel.id,
                            onClick = { viewModel.selectChannel(channel) },
                            onFavoriteToggle = { viewModel.toggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListRow(
    viewModel: IptvViewModel,
    channel: ChannelEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // #2B2930
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Live",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.groupTitle ?: "General",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val schedule by viewModel.fetchUpcomingSchedule(channel.tvgId ?: channel.name).collectAsState(initial = emptyList())
                val now = remember { System.currentTimeMillis() }
                val currentProgram = remember(schedule, now) {
                    schedule.firstOrNull { it.startTime <= now && it.endTime > now }
                }

                if (currentProgram != null) {
                    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val startStr = sdfTime.format(Date(currentProgram.startTime))
                    val endStr = sdfTime.format(Date(currentProgram.endTime))
                    
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOW",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentProgram.title,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$startStr - $endStr",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (channel.isFavorite) Color(0xFFFFC107) else Color.DarkGray
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChannelScheduleWidget(
    viewModel: IptvViewModel,
    channel: ChannelEntity
) {
    val schedule by viewModel.fetchUpcomingSchedule(channel.tvgId ?: channel.name).collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        val now = System.currentTimeMillis()
        val currentProgram = schedule.firstOrNull { it.startTime <= now && it.endTime > now }
        val upcomingPrograms = schedule.filter { it.startTime > now }.take(2)

        if (currentProgram != null) {
            val totalDuration = currentProgram.endTime - currentProgram.startTime
            val elapsed = now - currentProgram.startTime
            val progress = if (totalDuration > 0) elapsed.toFloat() / totalDuration else 0f

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // #2B2930
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ON AIR: ${currentProgram.title}",
                            color = MaterialTheme.colorScheme.primary, // #D0BCFF
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Text(
                            text = "${timeFmt.format(Date(currentProgram.startTime))} - ${timeFmt.format(Date(currentProgram.endTime))}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary, // #D0BCFF
                        trackColor = MaterialTheme.colorScheme.outline     // #49454F
                    )
                    
                    if (!currentProgram.description.isNullOrEmpty()) {
                        Text(
                            text = currentProgram.description,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (upcomingPrograms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upcomingPrograms.forEach { subProg ->
                    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFmt.format(Date(subProg.startTime)),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subProg.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
