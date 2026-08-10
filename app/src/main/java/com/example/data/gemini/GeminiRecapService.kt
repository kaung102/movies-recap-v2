package com.example.data.gemini

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class GeminiRecapService(private val apiKey: String) {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun generateRecapScript(videoData: ByteArray): String {
        val prompt = """
            You are a movie recap expert.
            1. Listen to the audio and analyze the scenes in this video.
            2. Write a natural, engaging Burmese narration script describing the actual story happening in the clip.
            3. Do not return hardcoded sample text. Generate exact script based on the video context.
            4. Provide line-by-line timed subtitles in Burmese and English translation.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(
                content {
                    blob("video/mp4", videoData)
                    text(prompt)
                }
            )
            response.text ?: "Error generating recap script."
        } catch (e: Exception) {
            "Failed to analyze video: ${e.localizedMessage}"
        }
    }
}
