package com.audioplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import com.audioplayer.db.AudioDatabase
import com.audioplayer.models.PlaybackProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class AudioManager(private val context: Context) {
    private var audioService: AudioService? = null
    private val db = AudioDatabase.getDatabase(context)
    
    private val _currentPlaylistId = MutableStateFlow<Long?>(null)
    val currentPlaylistId: StateFlow<Long?> = _currentPlaylistId.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private var updateProgressJob: Job? = null
    private var mediaItems: List<MediaItem> = emptyList()
    
    fun bindService(service: AudioService) {
        audioService = service
    }
    
    fun unbindService() {
        updateProgressJob?.cancel()
        audioService = null
    }
    
    fun playPlaylist(playlistId: Long, folderPath: String, startIndex: Int = 0, startPosition: Long = 0) {
        _currentPlaylistId.value = playlistId
        
        val mp3Files = File(folderPath).listFiles()?.filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }?.sortedBy { it.name } ?: emptyList()
        mediaItems = mp3Files.map { MediaItem.fromUri(Uri.fromFile(it)) }
        
        if (mediaItems.isNotEmpty()) {
            val actualStartIndex = minOf(startIndex, mediaItems.size - 1)
            audioService?.play(mediaItems, actualStartIndex, startPosition)
            _currentIndex.value = actualStartIndex
            _currentPosition.value = startPosition
            _isPlaying.value = true
            
            startProgressUpdate()
        }
    }
    
    fun pause() {
        audioService?.pause()
        _isPlaying.value = false
    }
    
    fun resume() {
        audioService?.resume()
        _isPlaying.value = true
    }
    
    fun skipToPrevious() {
        val newIndex = (_currentIndex.value - 1 + mediaItems.size) % mediaItems.size
        skipToIndex(newIndex)
    }
    
    fun skipToNext() {
        val newIndex = (_currentIndex.value + 1) % mediaItems.size
        skipToIndex(newIndex)
    }
    
    fun skipToIndex(index: Int) {
        if (index in 0 until mediaItems.size) {
            audioService?.seekTo(index, 0)
            _currentIndex.value = index
            _currentPosition.value = 0
            savePlaybackProgress()
        }
    }
    
    fun seekTo(position: Long) {
        audioService?.seekTo(position)
        _currentPosition.value = position
    }
    
    private fun startProgressUpdate() {
        updateProgressJob?.cancel()
        updateProgressJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(5000)
                audioService?.let {
                    val position = it.getCurrentPosition()
                    _currentPosition.value = position
                    savePlaybackProgress()
                }
            }
        }
    }
    
    private suspend fun savePlaybackProgress() {
        val playlistId = _currentPlaylistId.value ?: return
        val progress = PlaybackProgress(
            playlistId = playlistId,
            currentIndex = _currentIndex.value,
            currentPosition = _currentPosition.value,
            updateTime = Date()
        )
        db.playbackProgressDao().insert(progress)
    }
    
    fun savePlaybackProgressSync() {
        CoroutineScope(Dispatchers.IO).launch {
            savePlaybackProgress()
        }
    }
    
    suspend fun getPlaybackProgress(playlistId: Long): PlaybackProgress? {
        return db.playbackProgressDao().getByPlaylistId(playlistId)
    }
}
