package com.audioplayer.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.audioplayer.databinding.ActivityPlaybackBinding
import com.audioplayer.service.AudioService
import com.audioplayer.service.AudioManager
import com.audioplayer.utils.FileUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class PlaybackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaybackBinding
    private lateinit var viewModel: PlaybackViewModel
    private lateinit var adapter: SongAdapter
    private var audioService: AudioService? = null
    private var audioManager: AudioManager? = null
    private var isBound = false
    
    private val playlistId by lazy { intent.getLongExtra("playlistId", 0) }
    private val folderPath by lazy { intent.getStringExtra("folderPath") ?: "" }
    private val folderName by lazy { intent.getStringExtra("folderName") ?: "" }
    
    private var songList: List<File> = emptyList()
    private var currentPosition = 0L
    private var totalDuration = 0L
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.LocalBinder
            audioService = binder.getService()
            audioManager = AudioManager(applicationContext)
            audioManager?.bindService(audioService!!)
            isBound = true
            
            loadSongs()
            setupPlayback()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioManager?.unbindService()
            audioService = null
            audioManager = null
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = PlaybackViewModel(application)
        setupRecyclerView()
        setupListeners()
        setupToolbar()
        
        val intent = Intent(this, AudioService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = folderName
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = SongAdapter(
            onItemClick = { position ->
                audioManager?.skipToIndex(position)
            }
        )
        
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        binding.ivPlayPause.setOnClickListener {
            if (audioManager?.isPlaying?.value == true) {
                audioManager?.pause()
                binding.ivPlayPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
                audioManager?.resume()
                binding.ivPlayPause.setImageResource(R.drawable.ic_baseline_pause_24)
            }
        }
        
        binding.ivPrevious.setOnClickListener {
            audioManager?.skipToPrevious()
        }
        
        binding.ivNext.setOnClickListener {
            audioManager?.skipToNext()
        }
        
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPosition = (progress * totalDuration / 100).coerceAtMost(totalDuration)
                    binding.tvCurrentTime.text = formatTime(currentPosition)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始拖动
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    audioManager?.seekTo(currentPosition)
                }
            }
        })
    }
    
    private fun loadSongs() {
        songList = FileUtils.getMp3FilesInFolder(folderPath)
        adapter.submitList(songList.map { it.name })
    }
    
    private fun setupPlayback() {
        val (startIndex, startPosition) = viewModel.loadPlaybackProgress(playlistId)
        
        // 检查起始音频文件是否存在
        if (startIndex in 0 until songList.size) {
            val startFile = songList[startIndex]
            if (!startFile.exists()) {
                Toast.makeText(this, getString(R.string.file_missing), Toast.LENGTH_SHORT).show()
                // 播放下一首
                val nextIndex = (startIndex + 1) % songList.size
                audioManager?.playPlaylist(playlistId, folderPath, nextIndex, 0)
                updateUI(nextIndex)
            } else {
                audioManager?.playPlaylist(playlistId, folderPath, startIndex, startPosition)
                updateUI(startIndex)
            }
        }
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            audioManager?.currentIndex?.collect { index ->
                updateUI(index)
            }
        }
        
        lifecycleScope.launch {
            audioManager?.currentPosition?.collect { position ->
                currentPosition = position
                binding.tvCurrentTime.text = formatTime(position)
                if (totalDuration > 0) {
                    binding.seekBar.progress = (position * 100 / totalDuration).toInt()
                }
            }
        }
        
        lifecycleScope.launch {
            audioManager?.isPlaying?.collect { isPlaying ->
                binding.ivPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
                )
            }
        }
    }
    
    private fun updateUI(currentIndex: Int) {
        if (currentIndex in 0 until songList.size) {
            val currentSong = songList[currentIndex]
            binding.tvCurrentSong.text = currentSong.name
            adapter.setCurrentPlayingIndex(currentIndex)
            
            // 模拟设置总时长，实际应该从ExoPlayer获取
            totalDuration = 180000 // 3分钟
            binding.tvTotalTime.text = formatTime(totalDuration)
        }
    }
    
    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
