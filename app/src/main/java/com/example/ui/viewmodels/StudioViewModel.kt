package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.BurmeseTtsEngine
import com.example.data.db.AppDatabase
import com.example.data.db.RecapProjectEntity
import com.example.data.db.RecapProjectRepository
import com.example.data.gemini.GeminiRecapService
import com.example.data.models.RecapJsonConverter
import com.example.data.models.RecapSegment
import com.example.data.video.ExportState
import com.example.data.video.SampleVideos
import com.example.data.video.VideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecapProjectRepository
    private val geminiService: GeminiRecapService
    val ttsEngine: BurmeseTtsEngine
    private val videoExporter: VideoExporter

    val allProjects: StateFlow<List<RecapProjectEntity>>

    private val _currentProject = MutableStateFlow<RecapProjectEntity?>(null)
    val currentProject: StateFlow<RecapProjectEntity?> = _currentProject.asStateFlow()

    private val _segments = MutableStateFlow<List<RecapSegment>>(emptyList())
    val segments: StateFlow<List<RecapSegment>> = _segments.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isOriginalAudioMuted = MutableStateFlow(true)
    val isOriginalAudioMuted: StateFlow<Boolean> = _isOriginalAudioMuted.asStateFlow()

    private val _syncOffsetMs = MutableStateFlow(0L)
    val syncOffsetMs: StateFlow<Long> = _syncOffsetMs.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState?>(null)
    val exportState: StateFlow<ExportState?> = _exportState.asStateFlow()

    private val _activeSegment = MutableStateFlow<RecapSegment?>(null)
    val activeSegment: StateFlow<RecapSegment?> = _activeSegment.asStateFlow()

    private var playbackJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RecapProjectRepository(db.recapProjectDao())
        geminiService = GeminiRecapService(application)
        ttsEngine = BurmeseTtsEngine(application)
        videoExporter = VideoExporter(application)

        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun loadOrCreateProject(
        videoUri: String,
        title: String,
        durationMs: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val projectId = "proj_${videoUri.hashCode()}"
            val existing = repository.getProjectById(projectId)

            if (existing != null) {
                _currentProject.value = existing
                _isOriginalAudioMuted.value = existing.isOriginalAudioMuted
                _syncOffsetMs.value = existing.syncOffsetMs
                val loadedSegs = RecapJsonConverter.fromJson(existing.segmentsJson)
                _segments.value = if (loadedSegs.isNotEmpty()) loadedSegs else geminiService.generateFallbackSegments(title, durationMs)
            } else {
                val initialSegs = geminiService.generateFallbackSegments(title, durationMs)
                val newEntity = RecapProjectEntity(
                    id = projectId,
                    title = title,
                    videoUri = videoUri,
                    videoDurationMs = durationMs,
                    isOriginalAudioMuted = true,
                    segmentsJson = RecapJsonConverter.toJson(initialSegs)
                )
                repository.saveProject(newEntity)
                _currentProject.value = newEntity
                _segments.value = initialSegs
            }
        }
    }

    fun generateAiRecapScript(customPrompt: String = "") {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            _isGenerating.value = true
            val newSegments = geminiService.generateRecapTimeline(
                videoTitle = proj.title,
                videoDurationMs = proj.videoDurationMs,
                customPrompt = customPrompt
            )
            _segments.value = newSegments
            _isGenerating.value = false
            saveCurrentProjectState()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val dur = _currentProject.value?.videoDurationMs ?: 30000L
            while (_isPlaying.value) {
                delay(100)
                var nextTime = _currentTimeMs.value + 100
                if (nextTime >= dur) {
                    nextTime = 0L // Loop
                }
                _currentTimeMs.value = nextTime
                updateActiveSegmentAndTts(nextTime)
            }
        }
    }

    private fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        ttsEngine.stop()
    }

    fun seekTo(positionMs: Long) {
        val dur = _currentProject.value?.videoDurationMs ?: 30000L
        val target = positionMs.coerceIn(0L, dur)
        _currentTimeMs.value = target
        updateActiveSegmentAndTts(target)
    }

    private fun updateActiveSegmentAndTts(currentTime: Long) {
        val effectiveTime = currentTime + _syncOffsetMs.value
        val currentSeg = _segments.value.find { seg ->
            effectiveTime >= seg.startTimeMs && effectiveTime <= seg.endTimeMs
        }

        if (currentSeg?.id != _activeSegment.value?.id) {
            _activeSegment.value = currentSeg
            if (currentSeg != null && _isPlaying.value) {
                // Trigger synchronized Burmese voiceover for active segment
                ttsEngine.speakSegment(currentSeg, _syncOffsetMs.value)
            }
        }
    }

    fun updateSegment(updatedSegment: RecapSegment) {
        val list = _segments.value.toMutableList()
        val index = list.indexOfFirst { it.id == updatedSegment.id }
        if (index >= 0) {
            list[index] = updatedSegment
            _segments.value = list
            saveCurrentProjectState()
        }
    }

    fun addSegment() {
        val list = _segments.value.toMutableList()
        val totalMs = _currentProject.value?.videoDurationMs ?: 30000L
        val lastEnd = list.lastOrNull()?.endTimeMs ?: 0L
        val startMs = lastEnd.coerceAtMost(totalMs - 1000L)
        val endMs = (startMs + 5000L).coerceAtMost(totalMs)

        val newSeg = RecapSegment(
            id = "seg_${System.currentTimeMillis()}",
            startTimeMs = startMs,
            endTimeMs = endMs,
            burmeseScript = "မြန်မာစကားပြော ပြန်လည်သုံးသပ်ချက် အသစ်...",
            englishSubtitle = "New movie recap timeline segment..."
        )
        list.add(newSeg)
        _segments.value = list
        saveCurrentProjectState()
    }

    fun deleteSegment(segmentId: String) {
        val list = _segments.value.filter { it.id != segmentId }
        _segments.value = list
        saveCurrentProjectState()
    }

    fun adjustSyncOffset(offsetChangeMs: Long) {
        _syncOffsetMs.value += offsetChangeMs
        saveCurrentProjectState()
    }

    fun toggleOriginalMute() {
        _isOriginalAudioMuted.value = !_isOriginalAudioMuted.value
        saveCurrentProjectState()
    }

    fun previewVoiceoverSegment(segment: RecapSegment) {
        ttsEngine.speakSegment(segment, _syncOffsetMs.value)
    }

    fun exportFinalVideo() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            _exportState.value = ExportState.Processing(0.01f, "Preparing video export...")
            videoExporter.exportRecapVideo(
                videoUri = proj.videoUri,
                segments = _segments.value,
                ttsEngine = ttsEngine,
                totalDurationMs = proj.videoDurationMs,
                syncOffsetMs = _syncOffsetMs.value
            ).collect { state ->
                _exportState.value = state
                if (state is ExportState.Success) {
                    // Update project entity with exported path
                    val updatedEntity = proj.copy(
                        exportedVideoPath = state.exportedFilePath,
                        exportStatus = "COMPLETED",
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateProject(updatedEntity)
                    _currentProject.value = updatedEntity
                }
            }
        }
    }

    fun clearExportState() {
        _exportState.value = null
    }

    private fun saveCurrentProjectState() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = proj.copy(
                isOriginalAudioMuted = _isOriginalAudioMuted.value,
                syncOffsetMs = _syncOffsetMs.value,
                segmentsJson = RecapJsonConverter.toJson(_segments.value),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        ttsEngine.shutdown()
    }
}
