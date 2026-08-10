package com.example.data.gemini

import android.app.Application
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class GeminiRecapService(
    private val context: Application,
    private val apiKey: String
) {

    fun generateRecapScript(promptText: String): String {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", "Analyze the movie scene audio. Generate line-by-line Burmese movie recap subtitles with timeline: $promptText")
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text")
            } else {
                "API Error Code: ${conn.responseCode}"
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    // StudioViewModel မှ တောင်းဆိုထားသော Function များ
    fun generateRecapTimeline(videoPath: String?): String {
        return generateRecapScript("Video path: ${videoPath ?: "default"}")
    }

    fun generateFallbackSegments(): List<String> {
        return listOf(
            "ဇာတ်လမ်းစတင်ချိန်မှာတော့ မြို့တော်ကြီးရဲ့ မှောင်မိုက်တဲ့ ညတစ်ညမှာ...",
            "ဇာတ်လိုက်ဟာ သဲလွန်စအသစ်တွေကို ရှာဖွေတွေ့ရှိခဲ့ပြီး...",
            "ရုတ်တရက်ဆိုသလိုပဲ မထင်မှတ်ထားတဲ့ တိုက်ခိုက်မှုနဲ့အတူ...",
            "သူ့ရဲ့ ရဲရင့်တဲ့ ဆုံးဖြတ်ချက်ကြောင့် နောက်ဆုံး တိုက်ပွဲဆီသို့...",
            "နောက်ဆုံးမှာတော့ အမှန်တရား ပေါ်ပေါက်သွားခဲ့ပြီး..."
        )
    }
}
