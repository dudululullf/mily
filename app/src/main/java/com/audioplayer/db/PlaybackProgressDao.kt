package com.audioplayer.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.audioplayer.models.PlaybackProgress

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: PlaybackProgress): Long

    @Update
    suspend fun update(progress: PlaybackProgress)

    @Delete
    suspend fun delete(progress: PlaybackProgress)

    @Query("SELECT * FROM playback_progress WHERE playlistId = :playlistId")
    suspend fun getByPlaylistId(playlistId: Long): PlaybackProgress?

    @Query("DELETE FROM playback_progress WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)
}
