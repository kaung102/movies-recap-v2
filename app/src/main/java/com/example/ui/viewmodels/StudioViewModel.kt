package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiRecapService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiRecapService(application, "YOUR_API_KEY")

    private val _scriptResult = MutableStateFlow<String>("")
    val scriptResult: StateFlow<String> = _scriptResult

    fun processFullVideo(videoPath: String, videoDurationSeconds: Int) {
        viewModelScope.launch {
            // ၁၆ မိနစ်စာ (Full Duration) အပြည့်အဝ Process လုပ်ရန် ခိုင်းစေခြင်း
            val prompt = "Analyze the full video clip duration ($videoDurationSeconds seconds). Generate complete Burmese narration and timed subtitles for the entire duration."
            val result = geminiService.generateRecapScript(prompt)
            _scriptResult.value = result
        }
    }

    fun generateFallbackSegments(): List<String> {
        return emptyList() // Fallback ပုံသေ စာသားများကို လုံးဝ မသုံးစေရန်
    }

    fun generateRecapTimeline(videoPath: String?): String {
        return geminiService.generateRecapTimeline(videoPath)
    }
}

