package com.audioplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.audioplayer.databinding.ActivityMainBinding
import com.audioplayer.models.Playlist
import com.audioplayer.utils.FileUtils
import com.audioplayer.utils.PermissionUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PlaylistViewModel
    private lateinit var adapter: PlaylistAdapter
    
    private var isEditMode = false
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val folderPath = getFolderPathFromUri(uri)
                folderPath?.let {
                    viewModel.importFolder(it)
                }
            }
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFolderPicker()
        } else {
            Toast.makeText(this, "需要存储权限才能导入音频文件", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "需要通知权限才能显示播放控制", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = PlaylistViewModel(application)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkNotificationPermission()
    }
    
    private fun checkNotificationPermission() {
        if (!PermissionUtils.hasNotificationPermission(this)) {
            notificationPermissionLauncher.launch(PermissionUtils.getNotificationPermission())
        }
    }
    
    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onItemClick = { playlist ->
                if (!isEditMode) {
                    if (viewModel.checkFolderExists(playlist)) {
                        navigateToPlayback(playlist)
                    } else {
                        showFolderNotFoundDialog(playlist)
                    }
                }
            },
            onDeleteClick = { playlist ->
                viewModel.deletePlaylist(playlist)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun showFolderNotFoundDialog(playlist: Playlist) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.folder_not_found))
            .setMessage("该文件夹已不存在，是否移除该播放列表？")
            .setPositiveButton(getString(R.string.confirm)) {
                _, _ ->
                viewModel.deletePlaylist(playlist)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun setupListeners() {
        binding.btnImport.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        binding.btnEdit.setOnClickListener {
            toggleEditMode()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playlists.collect {playlists ->
                adapter.submitList(playlists)
                updateEmptyState(playlists.isEmpty())
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect {error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun toggleEditMode() {
        isEditMode = !isEditMode
        adapter.setEditMode(isEditMode)
        binding.btnEdit.text = if (isEditMode) getString(R.string.done) else getString(R.string.edit)
    }
    
    private fun checkPermissionsAndOpenPicker() {
        if (PermissionUtils.hasStoragePermission(this)) {
            openFolderPicker()
        } else {
            permissionLauncher.launch(PermissionUtils.getStoragePermission())
        }
    }
    
    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        folderPickerLauncher.launch(intent)
    }
    
    private fun getFolderPathFromUri(uri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        
        if (type == "primary") {
            return "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
        }
        return null
    }
    
    private fun navigateToPlayback(playlist: Playlist) {
        val intent = Intent(this, PlaybackActivity::class.java)
        intent.putExtra("playlistId", playlist.id)
        intent.putExtra("folderPath", playlist.folderPath)
        intent.putExtra("folderName", playlist.folderName)
        startActivity(intent)
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
