package com.audioplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audioplayer.db.AudioDatabase
import com.audioplayer.models.PlaybackProgress
import com.audioplayer.service.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AudioDatabase.getDatabase(application)
    private val audioManager = AudioManager(application)
    
    private val _currentPlaylistId = MutableStateFlow<Long?>(null)
    val currentPlaylistId: StateFlow<Long?> = _currentPlaylistId.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _audioFiles = MutableStateFlow<List<String>>(emptyList())
    val audioFiles: StateFlow<List<String>> = _audioFiles.asStateFlow()
    
    fun bindAudioManager(manager: AudioManager) {
        // 绑定音频管理器
    }
    
    fun loadPlaybackProgress(playlistId: Long): Pair<Int, Long> {
        var progress = Pair(0, 0L)
        viewModelScope.launch {
            val playbackProgress = db.playbackProgressDao().getByPlaylistId(playlistId)
            progress = playbackProgress?.let { Pair(it.currentIndex, it.currentPosition) } ?: Pair(0, 0L)
        }
        return progress
    }
    
    fun savePlaybackProgress(playlistId: Long, index: Int, position: Long) {
        viewModelScope.launch {
            val progress = PlaybackProgress(
                playlistId = playlistId,
                currentIndex = index,
                currentPosition = position,
                updateTime = Date()
            )
            db.playbackProgressDao().insert(progress)
        }
    }
    
    fun playPlaylist(playlistId: Long, folderPath: String) {
        _currentPlaylistId.value = playlistId
        
        viewModelScope.launch {
            val progress = db.playbackProgressDao().getByPlaylistId(playlistId)
            val (startIndex, startPosition) = progress?.let { Pair(it.currentIndex, it.currentPosition) } ?: Pair(0, 0L)
            
            audioManager.playPlaylist(playlistId, folderPath, startIndex, startPosition)
            _currentIndex.value = startIndex
            _currentPosition.value = startPosition
            _isPlaying.value = true
        }
    }
    
    fun pause() {
        audioManager.pause()
        _isPlaying.value = false
    }
    
    fun resume() {
        audioManager.resume()
        _isPlaying.value = true
    }
    
    fun skipToPrevious() {
        audioManager.skipToPrevious()
    }
    
    fun skipToNext() {
        audioManager.skipToNext()
    }
    
    fun skipToIndex(index: Int) {
        audioManager.skipToIndex(index)
    }
    
    fun seekTo(position: Long) {
        audioManager.seekTo(position)
    }
}
