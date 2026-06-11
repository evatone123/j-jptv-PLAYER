package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEntity
import com.example.ui.components.VideoPlayer
import com.example.ui.viewmodel.IptvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiScreenView(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val streams by viewModel.multiScreenStreams.collectAsState()
    val layoutCount by viewModel.multiScreenLayout.collectAsState()
    val focusedAudioIndex by viewModel.focusedAudioIndex.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()

    var showChannelSelectorForIndex by remember { mutableStateOf<Int?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // #1C1B1F
    ) {
        // --- 1. Layout Control Tab ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // #2B2930
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Multi-Screen Viewing cockpit",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Compare and watch up to 4 live streams concurrently in split sync.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeColor = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // #D0BCFF
                        contentColor = MaterialTheme.colorScheme.onPrimary // #381E72
                    )
                    val inactiveColor = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary // #4A4458
                    )
                    
                    Button(
                        onClick = { viewModel.selectLayout(1) },
                        modifier = Modifier.weight(1f),
                        colors = if (layoutCount == 1) activeColor else inactiveColor,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Square, contentDescription = "1 Screen", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1 Stream", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.selectLayout(2) },
                        modifier = Modifier.weight(1f),
                        colors = if (layoutCount == 2) activeColor else inactiveColor,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Splitscreen, contentDescription = "2 Screens", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("2 Split", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.selectLayout(4) },
                        modifier = Modifier.weight(1f),
                        colors = if (layoutCount == 4) activeColor else inactiveColor,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "4 Screens", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("4 Grid", fontSize = 11.sp)
                    }
                }
            }
        }

        // --- 2. Live Grid Viewport Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            when (layoutCount) {
                1 -> {
                    // Single full screen slot
                    MultiViewSlot(
                        index = 0,
                        channel = streams[0],
                        isAudioFocused = focusedAudioIndex == 0,
                        onSlotTap = { viewModel.setFocusedAudio(0) },
                        onSelectChannel = { showChannelSelectorForIndex = 0 },
                        onClearSlot = { viewModel.clearGridSlot(0) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // Vertical split (2 streams stacked on top of each other)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MultiViewSlot(
                            index = 0,
                            channel = streams[0],
                            isAudioFocused = focusedAudioIndex == 0,
                            onSlotTap = { viewModel.setFocusedAudio(0) },
                            onSelectChannel = { showChannelSelectorForIndex = 0 },
                            onClearSlot = { viewModel.clearGridSlot(0) },
                            modifier = Modifier.weight(1f)
                        )
                        MultiViewSlot(
                            index = 1,
                            channel = streams[1],
                            isAudioFocused = focusedAudioIndex == 1,
                            onSlotTap = { viewModel.setFocusedAudio(1) },
                            onSelectChannel = { showChannelSelectorForIndex = 1 },
                            onClearSlot = { viewModel.clearGridSlot(1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                4 -> {
                    // 2x2 split grid of 4 streams
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MultiViewSlot(
                                index = 0,
                                channel = streams[0],
                                isAudioFocused = focusedAudioIndex == 0,
                                onSlotTap = { viewModel.setFocusedAudio(0) },
                                onSelectChannel = { showChannelSelectorForIndex = 0 },
                                onClearSlot = { viewModel.clearGridSlot(0) },
                                modifier = Modifier.weight(1f)
                            )
                            MultiViewSlot(
                                index = 1,
                                channel = streams[1],
                                isAudioFocused = focusedAudioIndex == 1,
                                onSlotTap = { viewModel.setFocusedAudio(1) },
                                onSelectChannel = { showChannelSelectorForIndex = 1 },
                                onClearSlot = { viewModel.clearGridSlot(1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MultiViewSlot(
                                index = 2,
                                channel = streams[2],
                                isAudioFocused = focusedAudioIndex == 2,
                                onSlotTap = { viewModel.setFocusedAudio(2) },
                                onSelectChannel = { showChannelSelectorForIndex = 2 },
                                onClearSlot = { viewModel.clearGridSlot(2) },
                                modifier = Modifier.weight(1f)
                            )
                            MultiViewSlot(
                                index = 3,
                                channel = streams[3],
                                isAudioFocused = focusedAudioIndex == 3,
                                onSlotTap = { viewModel.setFocusedAudio(3) },
                                onSelectChannel = { showChannelSelectorForIndex = 3 },
                                onClearSlot = { viewModel.clearGridSlot(3) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Inner Channel Selector Dialog Sheet ---
        showChannelSelectorForIndex?.let { index ->
            val indexLabel = index + 1
            AlertDialog(
                onDismissRequest = { 
                    showChannelSelectorForIndex = null
                    channelSearchQuery = ""
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // #2B2930
                title = { 
                    Text("Select stream for Screen #$indexLabel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = channelSearchQuery,
                            onValueChange = { channelSearchQuery = it },
                            placeholder = { Text("Filter channels info...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, 
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        val filteredSelectorList = channels.filter {
                            it.name.contains(channelSearchQuery, ignoreCase = true)
                        }

                        if (filteredSelectorList.isEmpty()) {
                            Box(
                                modifier = Modifier.height(200.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matches found", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth()
                            ) {
                                items(filteredSelectorList) { chan ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), // #1C1B1F
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.setStreamInGrid(index, chan)
                                                showChannelSelectorForIndex = null
                                                channelSearchQuery = ""
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Tv, contentDescription = "TV", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(chan.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(chan.groupTitle ?: "General", color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        showChannelSelectorForIndex = null
                        channelSearchQuery = ""
                    }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun MultiViewSlot(
    index: Int,
    channel: ChannelEntity?,
    isAudioFocused: Boolean,
    onSlotTap: () -> Unit,
    onSelectChannel: () -> Unit,
    onClearSlot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant) // #2B2930
            .border(
                width = if (isAudioFocused) 2.dp else 1.dp,
                color = if (isAudioFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSlotTap() }
    ) {
        if (channel != null) {
            // Render actual live stream video player
            VideoPlayer(
                url = channel.streamUrl,
                modifier = Modifier.fillMaxSize(),
                isMuted = !isAudioFocused,
                showControls = false // Keep the view minimal inside grids
            )

            // Overlays: Sound Active, Close button and Name tag
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Top controls line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Status Badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isAudioFocused) MaterialTheme.colorScheme.primary else Color.Black.copy(
                                    alpha = 0.6f
                                )
                            )
                            .clickable { onSlotTap() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAudioFocused) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Audio status",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Swap/Close buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onSelectChannel,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Swap stream", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        }
                        IconButton(
                            onClick = onClearSlot,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear slot", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // Bottom Channel Name Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Render unselected placeholder slot
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onSelectChannel() }
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Add stream",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Screen #${index + 1}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to load",
                    color = Color.DarkGray,
                    fontSize = 9.sp
                )
            }
        }
    }
}
