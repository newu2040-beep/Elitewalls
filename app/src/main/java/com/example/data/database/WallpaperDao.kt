package com.example.data.database

import androidx.room.*
import com.example.data.models.WallpaperItem
import com.example.data.models.SoundItem
import com.example.data.models.CollectionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    // --- Wallpapers API ---
    @Query("SELECT * FROM wallpapers ORDER BY timestamp DESC")
    fun getAllWallpapers(): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpapers WHERE category = :category ORDER BY timestamp DESC")
    fun getWallpapersByCategory(category: String): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpapers WHERE isLive = 1 ORDER BY timestamp DESC")
    fun getLiveWallpapers(): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpapers WHERE isBookmarked = 1 OR isLiked = 1 ORDER BY timestamp DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpapers WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    fun getDownloadedWallpapers(): Flow<List<WallpaperItem>>

    @Query("SELECT * FROM wallpapers WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchWallpapers(query: String): Flow<List<WallpaperItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperItem>)

    @Update
    suspend fun updateWallpaper(wallpaper: WallpaperItem)

    @Delete
    suspend fun deleteWallpaper(wallpaper: WallpaperItem)

    // --- Sounds & Ringtones API ---
    @Query("SELECT * FROM sounds ORDER BY timestamp DESC")
    fun getAllSounds(): Flow<List<SoundItem>>

    @Query("SELECT * FROM sounds WHERE category = :category ORDER BY timestamp DESC")
    fun getSoundsByCategory(category: String): Flow<List<SoundItem>>

    @Query("SELECT * FROM sounds WHERE isBookmarked = 1 OR isLiked = 1 ORDER BY timestamp DESC")
    fun getFavoriteSounds(): Flow<List<SoundItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSound(sound: SoundItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSounds(sounds: List<SoundItem>)

    @Update
    suspend fun updateSound(sound: SoundItem)

    // --- Collections API ---
    @Query("SELECT * FROM user_collections ORDER BY timestamp DESC")
    fun getAllCollections(): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionItem)

    @Delete
    suspend fun deleteCollection(collection: CollectionItem)
}
