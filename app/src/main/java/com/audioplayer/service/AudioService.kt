package com.audioplayer.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioService : MediaLibraryService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var mediaSession: MediaLibraryService.MediaLibrarySession
    private var exoPlayer: ExoPlayer? = null
    
    private val _playbackState = MutableStateFlow(0) // 0 = None, 1 = Playing, 2 = Paused, 3 = Buffering, 4 = Stopped
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeSessionAndPlayer()
    }
    
    private fun initializeSessionAndPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setAudioFocusPolicy(ExoPlayer.AUDIO_FOCUS_POLICY_PAUSES_WHEN_NO_FOCUS)
            .build()
        
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> _playbackState.value = 0 // None
                    Player.STATE_BUFFERING -> _playbackState.value = 3 // Buffering
                    Player.STATE_READY -> _playbackState.value = if (exoPlayer?.isPlaying == true) 1 else 2 // Playing or Paused
                    Player.STATE_ENDED -> _playbackState.value = 4 // Stopped
                }
            }
            
            override fun onPositionDiscontinuity(reason: Player.PositionDiscontinuityReason) {
                _currentPosition.value = exoPlayer?.currentPosition ?: 0L
            }
        })
        
        val callback = object : MediaLibraryService.MediaLibrarySession.Callback {
            override fun onAddMediaItems(
                mediaSession: MediaLibraryService.MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ) {
                val preparedMediaItems = mediaItems.map {
                    MediaItem.fromUri(it.requestMetadata.mediaUri!!)
                }
                mediaSession.addMediaItems(preparedMediaItems)
            }
            
            override fun onPlay(mediaSession: MediaLibraryService.MediaLibrarySession, controller: MediaSession.ControllerInfo) {
                exoPlayer?.play()
            }
            
            override fun onPause(mediaSession: MediaLibraryService.MediaLibrarySession, controller: MediaSession.ControllerInfo) {
                exoPlayer?.pause()
            }
            
            override fun onSkipToPrevious(mediaSession: MediaLibraryService.MediaLibrarySession, controller: MediaSession.ControllerInfo) {
                exoPlayer?.previous()
            }
            
            override fun onSkipToNext(mediaSession: MediaLibraryService.MediaLibrarySession, controller: MediaSession.ControllerInfo) {
                exoPlayer?.next()
            }
        }
        
        mediaSession = MediaLibraryService.MediaLibrarySession.Builder(this, exoPlayer!!, callback)
            .setSessionActivity { packageManager?.getLaunchIntentForPackage(packageName) }
            .build()
        
        // 启动前台服务，确保后台播放
        serviceScope.launch {
            val session = mediaSession
            session.setMediaNotificationProvider {session, state, builder ->
                builder.setContentTitle("Audio Player")
                    .setContentText("正在播放")
                    .build()
            }
        }
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? {
        return mediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        exoPlayer?.release()
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        exoPlayer?.release()
        serviceJob.cancel()
    }
    
    fun play(mediaItems: List<MediaItem>, startIndex: Int, startPosition: Long) {
        exoPlayer?.apply {
            setMediaItems(mediaItems)
            seekTo(startIndex, startPosition)
            play()
        }
    }
    
    fun pause() {
        exoPlayer?.pause()
    }
    
    fun resume() {
        exoPlayer?.play()
    }
    
    fun stop() {
        exoPlayer?.stop()
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    fun seekTo(index: Int, position: Long) {
        exoPlayer?.seekTo(index, position)
    }
    
    fun skipToPrevious() {
        exoPlayer?.previous()
    }
    
    fun skipToNext() {
        exoPlayer?.next()
    }
    
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    fun getCurrentIndex(): Int {
        return exoPlayer?.currentMediaItemIndex ?: 0
    }
    
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
}
