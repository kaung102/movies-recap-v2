package com.example.data.gemini

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.models.RecapSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRecapService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val valStr = keyField.get(null) as? String ?: ""
            if (valStr == "MY_GEMINI_API_KEY") "" else valStr
        } catch (e: Exception) {
            ""
        }

    suspend fun generateRecapTimeline(
        videoTitle: String,
        videoDurationMs: Long,
        customPrompt: String = ""
    ): List<RecapSegment> = withContext(Dispatchers.IO) {
        val key = apiKey
        if (key.isBlank()) {
            Log.d("GeminiRecapService", "No GEMINI_API_KEY found, using local smart studio synthesizer")
            return@withContext generateFallbackSegments(videoTitle, videoDurationMs)
        }

        try {
            val systemPrompt = """
                You are a professional movie recap writer and Burmese voiceover sync director.
                Your goal: Generate a movie recap script for a video titled "$videoTitle" with total duration ${videoDurationMs / 1000} seconds.
                
                REQUIREMENTS:
                1. Split the total video duration into 4 to 8 sequential timed segments from 0s to ${videoDurationMs / 1000}s.
                2. For each segment, provide:
                   - Burmese voiceover script ('burmeseScript') written in natural Burmese movie recap style.
                   - Timed English subtitle ('englishSubtitle') translating or summarizing the action.
                   - Exact start time in milliseconds ('startTimeMs') and end time in milliseconds ('endTimeMs').
                   - Speaker tag ('speakerTag', e.g., 'Narrator', 'Protagonist').
                   - Scene visual description ('sceneDescription').
                
                Respond STRICTLY with valid JSON array of objects:
                [
                  {
                    "startTimeMs": 0,
                    "endTimeMs": 5000,
                    "burmeseScript": "မြန်မာစကားပြော ပြန်လည်သုံးသပ်ချက် မိတ်ဆက်...",
                    "englishSubtitle": "Welcome to the intense movie recap sequence...",
                    "speakerTag": "Narrator",
                    "sceneDescription": "Opening cinematic shot of the protagonist walking into the dark alley."
                  }
                ]
            """.trimIndent()

            val promptText = if (customPrompt.isNotBlank()) {
                "$systemPrompt\n\nUser specific notes: $customPrompt"
            } else {
                systemPrompt
            }

            // Using gemini-3.1-pro-preview with high thinking mode
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("responseMimeType", "application/json")
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$key"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiRecapService", "API call failed code ${response.code}: $responseBody")
                return@withContext generateFallbackSegments(videoTitle, videoDurationMs)
            }

            val jsonObject = JSONObject(responseBody)
            val candidates = jsonObject.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var resultText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Extract JSON array from Markdown code blocks if present
            if (resultText.contains("```json")) {
                resultText = resultText.substringAfter("```json").substringBefore("```").trim()
            } else if (resultText.contains("```")) {
                resultText = resultText.substringAfter("```").substringBefore("```").trim()
            }

            val jsonArray = JSONArray(resultText)
            val segments = mutableListOf<RecapSegment>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startMs = obj.optLong("startTimeMs", (i * (videoDurationMs / jsonArray.length())))
                val endMs = obj.optLong("endTimeMs", ((i + 1) * (videoDurationMs / jsonArray.length())))
                val burmese = obj.optString("burmeseScript", "ဒီအခန်းမှာတော့ ဇာတ်လိုက်ဟာ ကြီးမားတဲ့ စိန်ခေါ်မှုကို ရင်ဆိုင်နေရပါတယ်။")
                val english = obj.optString("englishSubtitle", "In this scene, the protagonist faces a major challenge.")
                val speaker = obj.optString("speakerTag", "Narrator")
                val scene = obj.optString("sceneDescription", "Cinematic Scene ${i + 1}")

                segments.add(
                    RecapSegment(
                        id = "seg_${System.currentTimeMillis()}_$i",
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        originalAudioVolume = 0.0f, // 100% muted original audio
                        burmeseScript = burmese,
                        englishSubtitle = english,
                        speakerTag = speaker,
                        sceneDescription = scene
                    )
                )
            }

            if (segments.isNotEmpty()) segments else generateFallbackSegments(videoTitle, videoDurationMs)
        } catch (e: Exception) {
            Log.e("GeminiRecapService", "Error parsing or calling Gemini API: ${e.message}", e)
            generateFallbackSegments(videoTitle, videoDurationMs)
        }
    }

    fun generateFallbackSegments(videoTitle: String, videoDurationMs: Long): List<RecapSegment> {
        val totalMs = if (videoDurationMs > 0) videoDurationMs else 30000L
        val segmentCount = 5
        val segDuration = totalMs / segmentCount

        val sampleScript = listOf(
            Pair(
                "ဇာတ်လမ်းစတင်ချိန်မှာတော့ မြို့တော်ကြီးရဲ့ မှောင်မိုက်တဲ့ ညတစ်ညမှာ လျှို့ဝှက်ဆန်းကြယ်တဲ့ အဖြစ်အပျက်တွေ စတင်ခဲ့ပါတယ်။",
                "As the story begins, mysterious events unfold on a dark city night."
            ),
            Pair(
                "ဇာတ်လိုက်ဟာ သဲလွန်စအသစ်တွေကို ရှာဖွေတွေ့ရှိခဲ့ပြီး ရန်သူတွေရဲ့ နောက်ယောင်ခံလိုက်ခြင်းကို ခံခဲ့ရပါတယ်။",
                "The protagonist uncovers fresh clues while being pursued by deadly adversaries."
            ),
            Pair(
                "ရုတ်တရက်ဆိုသလိုပဲ မထင်မှတ်ထားတဲ့ တိုက်ခိုက်မှုနဲ့အတူ အားလုံး ကမောက်ကမ ဖြစ်သွားခဲ့ရပါတယ်။",
                "Suddenly, an unexpected ambush throws everything into total chaos."
            ),
            Pair(
                "သူ့ရဲ့ ရဲရင့်တဲ့ ဆုံးဖြတ်ချက်ကြောင့် နောက်ဆုံး တိုက်ပွဲဆီသို့ တက်လှမ်းနိုင်ခဲ့ပါတယ်။",
                "Driven by brave determination, he advances towards the final showdown."
            ),
            Pair(
                "နောက်ဆုံးမှာတော့ အမှန်တရား ပေါ်ပေါက်သွားခဲ့ပြီး မြို့တော်ကြီး အေးချမ်းသွားခဲ့ပါတယ်။",
                "In the end, justice prevails and peace returns to the grand city."
            )
        )

        return (0 until segmentCount).map { index ->
            val startMs = index * segDuration
            val endMs = if (index == segmentCount - 1) totalMs else (index + 1) * segDuration
            val (burmese, english) = sampleScript[index % sampleScript.size]

            RecapSegment(
                id = "seg_preset_$index",
                startTimeMs = startMs,
                endTimeMs = endMs,
                originalAudioVolume = 0.0f,
                burmeseScript = burmese,
                englishSubtitle = english,
                speakerTag = if (index == 0) "Intro Narrator" else "Movie Recap",
                sceneDescription = "Scene Segment #${index + 1} ($videoTitle)"
            )
        }
    }
}
