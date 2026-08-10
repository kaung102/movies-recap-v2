package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.RecapSegment
import com.example.data.video.ExportState
import com.example.ui.components.MultitrackTimeline
import com.example.ui.components.SegmentEditorCard
import com.example.ui.components.VideoPlayerCanvas
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodels.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspaceScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExportDetail: (String) -> Unit
) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isOriginalAudioMuted by viewModel.isOriginalAudioMuted.collectAsStateWithLifecycle()
    val syncOffsetMs by viewModel.syncOffsetMs.collectAsStateWithLifecycle()
    val activeSegment by viewModel.activeSegment.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var customAiPrompt by remember { mutableStateOf("") }
    var showAiPromptDialog by remember { mutableStateOf(false) }

    val currentProj = project ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentProj.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Studio Workspace • 100% Audio Muted",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // One-Click Direct Video Export Button
                    Button(
                        onClick = { viewModel.exportFinalVideo() },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGold, contentColor = DarkCanvas),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export MP4", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCanvas)
            )
        },
        containerColor = DarkCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Workflow Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkflowStepItem("1. Audio Muted", true, StudioGold)
                    WorkflowStepItem("2. Burmese AI", segments.isNotEmpty(), StudioPurple)
                    WorkflowStepItem("3. Subtitle Sync", true, StudioCyan)
                    WorkflowStepItem("4. MP4 Export", exportState is ExportState.Success, StudioGold)
                }
            }

            // Interactive Video Player Canvas
            item {
                VideoPlayerCanvas(
                    videoUri = currentProj.videoUri,
                    isPlaying = isPlaying,
                    currentTimeMs = currentTimeMs,
                    totalDurationMs = currentProj.videoDurationMs,
                    isOriginalAudioMuted = isOriginalAudioMuted,
                    activeSegment = activeSegment,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleOriginalMute = { viewModel.toggleOriginalMute() }
                )
            }

            // Multitrack Sync Inspector
            item {
                MultitrackTimeline(
                    segments = segments,
                    totalDurationMs = currentProj.videoDurationMs,
                    currentTimeMs = currentTimeMs,
                    syncOffsetMs = syncOffsetMs,
                    activeSegmentId = activeSegment?.id,
                    onSegmentClick = { viewModel.seekTo(it.startTimeMs) },
                    onSeekTo = { viewModel.seekTo(it) },
                    onAdjustSyncOffset = { viewModel.adjustSyncOffset(it) }
                )
            }

            // AI Generator Trigger Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioPurple.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Gemini AI Script Engine",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Generates Burmese Voiceover & Timed English Subtitles",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Button(
                                onClick = { showAiPromptDialog = true },
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Regenerate AI Script")
                                }
                            }
                        }
                    }
                }
            }

            // Timeline Segment Cards Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timed Script & Subtitle Segments (${segments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Button(
                        onClick = { viewModel.addSegment() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = StudioGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Segment", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Segment Cards List
            items(segments, key = { it.id }) { seg ->
                SegmentEditorCard(
                    segment = seg,
                    isActive = seg.id == activeSegment?.id,
                    onSelectSegment = { viewModel.seekTo(seg.startTimeMs) },
                    onUpdateSegment = { viewModel.updateSegment(it) },
                    onPreviewVoiceover = { viewModel.previewVoiceoverSegment(it) },
                    onDeleteSegment = { viewModel.deleteSegment(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Custom AI Prompt Dialog
    if (showAiPromptDialog) {
        AlertDialog(
            onDismissRequest = { showAiPromptDialog = false },
            title = {
                Text("Customize Gemini AI Recap Script", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Provide specific instructions for tone, style, or focus (e.g. 'Dramatic action tone', 'Comic movie recap style').",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customAiPrompt,
                        onValueChange = { customAiPrompt = it },
                        placeholder = { Text("e.g. Fast-paced dramatic sci-fi narration...") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StudioPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiPromptDialog = false
                        viewModel.generateAiRecapScript(customAiPrompt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPurple)
                ) {
                    Text("Generate Script")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAiPromptDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }

    // Direct Video Export Progress Modal
    exportState?.let { state ->
        AlertDialog(
            onDismissRequest = {
                if (state is ExportState.Success || state is ExportState.Error) {
                    viewModel.clearExportState()
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state is ExportState.Success) Icons.Default.CheckCircle else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (state is ExportState.Success) StudioGold else StudioPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (state) {
                            is ExportState.Processing -> "Rendering & Exporting MP4"
                            is ExportState.Success -> "Export Complete!"
                            is ExportState.Error -> "Export Error"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    when (state) {
                        is ExportState.Processing -> {
                            Text(
                                text = state.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = StudioGold,
                                trackColor = DarkCanvas,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(state.progress * 100).toInt()}% • Muting Original Audio + Syncing Burmese Voiceover",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyan
                            )
                        }
                        is ExportState.Success -> {
                            Text(
                                text = "Your movie recap video has been exported successfully with muted original audio, synchronized Burmese voiceover, and burned-in English subtitles!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Output Path: ${state.exportedFilePath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        is ExportState.Error -> {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (state is ExportState.Success) {
                    Button(
                        onClick = {
                            viewModel.clearExportState()
                            onNavigateToExportDetail(currentProj.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGold, contentColor = DarkCanvas)
                    ) {
                        Text("View Final Video")
                    }
                } else if (state is ExportState.Error) {
                    Button(
                        onClick = { viewModel.clearExportState() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Text("Close")
                    }
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun WorkflowStepItem(label: String, isDone: Boolean, activeColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = if (isDone) activeColor else DarkCanvas,
            modifier = Modifier.size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isDone) activeColor else TextMuted
        )
    }
}
