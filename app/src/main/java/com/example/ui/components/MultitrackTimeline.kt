package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RecapSegment
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MultitrackTimeline(
    segments: List<RecapSegment>,
    totalDurationMs: Long,
    currentTimeMs: Long,
    syncOffsetMs: Long,
    activeSegmentId: String?,
    onSegmentClick: (RecapSegment) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAdjustSyncOffset: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Timeline Header & Sync Tweak Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Multitrack Sync Inspection",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Original Video (Muted) • Burmese Voiceover • Timed Subtitles",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Sync Tweak Fine Control
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioGold.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sync Tweak:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { onAdjustSyncOffset(-50L) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-50ms", tint = StudioGold)
                    }
                    Text(
                        text = "${if (syncOffsetMs >= 0) "+" else ""}${syncOffsetMs}ms",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudioGold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onAdjustSyncOffset(50L) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+50ms", tint = StudioGold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas Timeline Multitrack
        val dur = totalDurationMs.coerceAtLeast(1000L)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCanvas)
                .pointerInput(dur) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekTo((ratio * dur).toLong())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val trackHeight = 36f
                val gap = 6f

                // Track 1: Original Video Track (Muted Status)
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(0f, 4f),
                    size = Size(canvasWidth, trackHeight)
                )

                // Track 2: Burmese Voiceover Track
                drawRect(
                    color = Color(0xFF1E1035),
                    topLeft = Offset(0f, 4f + trackHeight + gap),
                    size = Size(canvasWidth, trackHeight)
                )

                // Track 3: English Subtitles Track
                drawRect(
                    color = Color(0xFF0C2438),
                    topLeft = Offset(0f, 4f + (trackHeight + gap) * 2),
                    size = Size(canvasWidth, trackHeight)
                )

                // Draw Segment Blocks on Voiceover and Subtitle tracks
                segments.forEach { seg ->
                    val startRatio = seg.startTimeMs.toFloat() / dur
                    val endRatio = seg.endTimeMs.toFloat() / dur
                    val segLeft = startRatio * canvasWidth
                    val segWidth = ((endRatio - startRatio) * canvasWidth).coerceAtLeast(4f)

                    // Voiceover segment block
                    val isVoiceActive = seg.id == activeSegmentId
                    val voColor = if (isVoiceActive) StudioPurple else Color(0xFF9D4EDD)
                    drawRoundRect(
                        color = voColor,
                        topLeft = Offset(segLeft, 4f + trackHeight + gap),
                        size = Size(segWidth - 2f, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    // Subtitle segment block
                    val subColor = if (isVoiceActive) StudioGold else StudioCyan
                    drawRoundRect(
                        color = subColor,
                        topLeft = Offset(segLeft, 4f + (trackHeight + gap) * 2),
                        size = Size(segWidth - 2f, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }

                // Draw Playhead Line
                val playheadRatio = (currentTimeMs.toFloat() / dur).coerceIn(0f, 1f)
                val playheadX = playheadRatio * canvasWidth
                drawLine(
                    color = StudioGold,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, canvasHeight),
                    strokeWidth = 4f
                )
                drawCircle(
                    color = StudioGold,
                    radius = 8f,
                    center = Offset(playheadX, 6f)
                )
            }

            // Track Labels Sidebar Overlay (Left)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 4.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeOff, contentDescription = "Muted Video", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Original Video (Muted)", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Burmese Voiceover", tint = StudioPurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Burmese Voiceover", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Subtitles, contentDescription = "English Subtitles", tint = StudioCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("English Subtitles", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
