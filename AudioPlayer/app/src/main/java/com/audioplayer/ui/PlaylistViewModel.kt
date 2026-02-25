package com.audioplayer.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audioplayer.db.AudioDatabase
import com.audioplayer.models.Playlist
import com.audioplayer.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AudioDatabase.getDatabase(application)
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadPlaylists()
    }
    
    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val playlists = db.playlistDao().getAll()
                _playlists.value = playlists
            } catch (e: Exception) {
                _errorMessage.value = "加载播放列表失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun checkFolderExists(playlist: Playlist): Boolean {
        return File(playlist.folderPath).exists()
    }
    
    fun importFolder(folderPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val folder = File(folderPath)
                if (!folder.exists() || !folder.isDirectory) {
                    _errorMessage.value = "文件夹不存在"
                    return@launch
                }
                
                val mp3Files = FileUtils.getMp3FilesInFolder(folderPath)
                if (mp3Files.isEmpty()) {
                    _errorMessage.value = "该文件夹未找到音频文件"
                    return@launch
                }
                
                // 检查是否已导入
                val existingPlaylist = db.playlistDao().getByFolderPath(folderPath)
                if (existingPlaylist != null) {
                    _errorMessage.value = "该文件夹已导入"
                    return@launch
                }
                
                val playlist = Playlist(
                    folderPath = folderPath,
                    folderName = FileUtils.getFolderName(folderPath),
                    importTime = Date()
                )
                
                db.playlistDao().insert(playlist)
                loadPlaylists()
            } catch (e: Exception) {
                _errorMessage.value = "导入失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                db.playlistDao().delete(playlist)
                db.playbackProgressDao().deleteByPlaylistId(playlist.id)
                _playlists.value = _playlists.value.filter { it.id != playlist.id }
            } catch (e: Exception) {
                _errorMessage.value = "删除失败"
            }
        }
    }
    
    fun getFolderPathFromUri(context: Context, uri: Uri): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                if ("primary" == type) {
                    "${context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)?.parent?.parent?.parent}/${split[1]}"
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }
}
