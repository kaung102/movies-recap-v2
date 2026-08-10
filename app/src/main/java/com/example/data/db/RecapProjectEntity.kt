package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recap_projects")
data class RecapProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUri: String,
    val videoDurationMs: Long,
    val isOriginalAudioMuted: Boolean = true,
    val segmentsJson: String,
    val exportedVideoPath: String? = null,
    val exportStatus: String = "DRAFT",
    val syncOffsetMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
