package com.audioplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audioplayer.databinding.SongItemBinding

class SongAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<String, SongAdapter.ViewHolder>(DiffCallback) {
    
    private var currentPlayingIndex = -1
    
    fun setCurrentPlayingIndex(index: Int) {
        currentPlayingIndex = index
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songName = getItem(position)
        holder.bind(songName, position)
    }
    
    inner class ViewHolder(private val binding: SongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(songName: String, position: Int) {
            binding.tvSongName.text = songName
            binding.ivPlaying.visibility = if (position == currentPlayingIndex) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
    }
    
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
            
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}
