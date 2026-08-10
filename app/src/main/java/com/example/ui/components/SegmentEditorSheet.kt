package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RecapSegment
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SegmentEditorCard(
    segment: RecapSegment,
    isActive: Boolean,
    onSelectSegment: () -> Unit,
    onUpdateSegment: (RecapSegment) -> Unit,
    onPreviewVoiceover: (RecapSegment) -> Unit,
    onDeleteSegment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var burmeseText by remember(segment.burmeseScript) { mutableStateOf(segment.burmeseScript) }
    var englishText by remember(segment.englishSubtitle) { mutableStateOf(segment.englishSubtitle) }
    var speakerTag by remember(segment.speakerTag) { mutableStateOf(segment.speakerTag) }

    val borderColor = if (isActive) StudioGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val cardBg = if (isActive) DarkSurface else DarkCanvas

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onSelectSegment()
                isExpanded = !isExpanded
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Segment Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = StudioPurple.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Voiceover", tint = StudioGold, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        val startSec = segment.startTimeMs / 1000
                        val endSec = segment.endTimeMs / 1000
                        Text(
                            text = String.format("[%02d:%02d - %02d:%02d]", startSec / 60, startSec % 60, endSec / 60, endSec % 60),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioGold
                        )
                        Text(
                            text = "Speaker: ${segment.speakerTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview Audio Button
                    IconButton(
                        onClick = { onPreviewVoiceover(segment) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(StudioGold.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Burmese Voiceover", tint = StudioGold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onDeleteSegment(segment.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Segment", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short Preview Summary when closed
            if (!isExpanded) {
                Text(
                    text = "🇲🇲 ${segment.burmeseScript}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "🇬🇧 ${segment.englishSubtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioCyan,
                    maxLines = 1
                )
            } else {
                // Expanded Edit Fields
                Spacer(modifier = Modifier.height(6.dp))

                // Burmese Voiceover Text Field
                OutlinedTextField(
                    value = burmeseText,
                    onValueChange = {
                        burmeseText = it
                        onUpdateSegment(segment.copy(burmeseScript = it))
                    },
                    label = { Text("Burmese Voiceover Script (မြန်မာစကားပြော)") },
                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = StudioPurple) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioPurple,
                        unfocusedBorderColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // English Subtitle Text Field
                OutlinedTextField(
                    value = englishText,
                    onValueChange = {
                        englishText = it
                        onUpdateSegment(segment.copy(englishSubtitle = it))
                    },
                    label = { Text("Timed English Subtitle") },
                    leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null, tint = StudioCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speaker Tag Field
                OutlinedTextField(
                    value = speakerTag,
                    onValueChange = {
                        speakerTag = it
                        onUpdateSegment(segment.copy(speakerTag = it))
                    },
                    label = { Text("Speaker Tag (e.g. Narrator)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioGold,
                        unfocusedBorderColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
