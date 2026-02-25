package com.audioplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "playback_progress")
data class PlaybackProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val currentIndex: Int,
    val currentPosition: Long,
    val updateTime: Date
)
