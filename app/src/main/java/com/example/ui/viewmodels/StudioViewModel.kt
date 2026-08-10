package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiRecapService
import com.example.data.models.RecapSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiRecapService(application, "")

    private val _scriptResult = MutableStateFlow<String>("")
    val scriptResult: StateFlow<String> = _scriptResult

    fun processFullVideo(videoPath: String, videoDurationSeconds: Int) {
        viewModelScope.launch {
            // ဗီဒီယို အစမှ အဆုံးအထိ အချိန်အပြည့်အဝ စာတန်းထုတ်ပေးရန် ခိုင်းစေခြင်း
            val prompt = """
                Analyze the ENTIRE video duration ($videoDurationSeconds seconds total).
                Do not stop at 40 seconds. Generate line-by-line Burmese recap script covering from 00:00 to the end of the video.
                Include detailed narration for all major scenes throughout the full video length.
            """.trimIndent()
            val result = geminiService.generateRecapScript(prompt)
            _scriptResult.value = result
        }
    }

    fun generateFallbackSegments(): List<RecapSegment> {
        return emptyList()
    }

    fun generateRecapTimeline(videoPath: String?): String {
        return geminiService.generateRecapTimeline(videoPath)
    }
}
