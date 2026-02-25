package com.audioplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audioplayer.databinding.PlaylistItemBinding
import com.audioplayer.models.Playlist
import com.audioplayer.utils.FileUtils

class PlaylistAdapter(
    private val onItemClick: (Playlist) -> Unit,
    private val onDeleteClick: (Playlist) -> Unit
) : ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(DiffCallback) {
    
    private var isEditMode = false
    
    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist)
    }
    
    inner class ViewHolder(private val binding: PlaylistItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist) {
            binding.tvFolderName.text = playlist.folderName
            
            val songCount = FileUtils.getMp3FilesInFolder(playlist.folderPath).size
            binding.tvSongCount.text = "${songCount} 首歌曲"
            
            binding.ivDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener {
                onItemClick(playlist)
            }
            
            binding.ivDelete.setOnClickListener {
                onDeleteClick(playlist)
            }
        }
    }
    
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
                return oldItem == newItem
            }
        }
    }
}
