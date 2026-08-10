package com.example.data.models

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class RecapSegment(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val originalAudioVolume: Float = 0.0f, // 0.0 = completely muted
    val burmeseScript: String,
    val englishSubtitle: String,
    val speakerTag: String = "Narrator",
    val sceneDescription: String = "",
    val voiceoverOffsetMs: Long = 0L,
    val playbackPitch: Float = 1.0f
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(100L)
}

object RecapJsonConverter {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, RecapSegment::class.java)
    private val listAdapter = moshi.adapter<List<RecapSegment>>(listType)

    fun toJson(segments: List<RecapSegment>): String {
        return listAdapter.toJson(segments)
    }

    fun fromJson(json: String): List<RecapSegment> {
        return try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
