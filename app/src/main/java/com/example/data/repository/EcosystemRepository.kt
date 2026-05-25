package com.example.data.repository

import com.example.data.database.WallpaperDao
import com.example.data.database.DatabaseSeeder
import com.example.data.models.WallpaperItem
import com.example.data.models.SoundItem
import com.example.data.models.CollectionItem
import com.example.data.models.SetupItem
import com.example.data.models.CreatorStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EcosystemRepository(private val dao: WallpaperDao) {

    init {
        // Run database seeding on in-app initialization
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedDatabase()
        }
    }

    private suspend fun checkAndSeedDatabase() {
        val wallpapers = dao.getAllWallpapers().first()
        if (wallpapers.isEmpty()) {
            dao.insertWallpapers(DatabaseSeeder.getSeedWallpapers())
            dao.insertSounds(DatabaseSeeder.getSeedSounds())
            for (col in DatabaseSeeder.getSeedCollections()) {
                dao.insertCollection(col)
            }
        }
    }

    // --- Wallpapers ---
    val allWallpapers: Flow<List<WallpaperItem>> = dao.getAllWallpapers()
    
    val liveWallpapers: Flow<List<WallpaperItem>> = dao.getLiveWallpapers()
    
    val favoriteWallpapers: Flow<List<WallpaperItem>> = dao.getFavoriteWallpapers()
    
    val downloadedWallpapers: Flow<List<WallpaperItem>> = dao.getDownloadedWallpapers()

    fun getWallpapersByCategory(category: String): Flow<List<WallpaperItem>> =
        dao.getWallpapersByCategory(category)

    fun searchWallpapers(query: String): Flow<List<WallpaperItem>> =
        dao.searchWallpapers(query)

    suspend fun toggleLikeWallpaper(wallpaperId: String, isLiked: Boolean) {
        val wallpaper = dao.searchWallpapers(wallpaperId).first().firstOrNull { it.id == wallpaperId }
        wallpaper?.let {
            val updated = it.copy(
                isLiked = isLiked,
                likesCount = if (isLiked) it.likesCount + 1 else maxOf(0, it.likesCount - 1)
            )
            dao.updateWallpaper(updated)
        }
    }

    suspend fun toggleBookmarkWallpaper(wallpaperId: String, isBookmarked: Boolean) {
        val wallpaper = dao.searchWallpapers(wallpaperId).first().firstOrNull { it.id == wallpaperId }
        wallpaper?.let {
            val updated = it.copy(isBookmarked = isBookmarked)
            dao.updateWallpaper(updated)
        }
    }

    suspend fun markAsDownloaded(wallpaperId: String) {
        val wallpaper = dao.searchWallpapers(wallpaperId).first().firstOrNull { it.id == wallpaperId }
        wallpaper?.let {
            val updated = it.copy(
                isDownloaded = true,
                downloadsCount = it.downloadsCount + 1
            )
            dao.updateWallpaper(updated)
        }
    }

    suspend fun uploadWallpaper(wallpaper: WallpaperItem) {
        dao.insertWallpaper(wallpaper)
    }

    // --- Sounds ---
    val allSounds: Flow<List<SoundItem>> = dao.getAllSounds()

    val favoriteSounds: Flow<List<SoundItem>> = dao.getFavoriteSounds()

    fun getSoundsByCategory(category: String): Flow<List<SoundItem>> =
        dao.getSoundsByCategory(category)

    suspend fun toggleLikeSound(soundId: String, isLiked: Boolean) {
        val soundList = dao.getAllSounds().first()
        val sound = soundList.firstOrNull { it.id == soundId }
        sound?.let {
            val updated = it.copy(isLiked = isLiked)
            dao.updateSound(updated)
        }
    }

    suspend fun toggleBookmarkSound(soundId: String, isBookmarked: Boolean) {
        val soundList = dao.getAllSounds().first()
        val sound = soundList.firstOrNull { it.id == soundId }
        sound?.let {
            val updated = it.copy(isBookmarked = isBookmarked)
            dao.updateSound(updated)
        }
    }

    suspend fun markSoundDownloaded(soundId: String) {
        val soundList = dao.getAllSounds().first()
        val sound = soundList.firstOrNull { it.id == soundId }
        sound?.let {
            val updated = it.copy(
                isDownloaded = true,
                downloadsCount = it.downloadsCount + 1
            )
            dao.updateSound(updated)
        }
    }

    suspend fun uploadSound(sound: SoundItem) {
        dao.insertSound(sound)
    }

    // --- Collections ---
    val allCollections: Flow<List<CollectionItem>> = dao.getAllCollections()

    suspend fun insertCollection(collection: CollectionItem) {
        dao.insertCollection(collection)
    }

    suspend fun deleteCollection(collection: CollectionItem) {
        dao.deleteCollection(collection)
    }

    // --- Setups ---
    val allSetups: Flow<List<SetupItem>> = dao.getAllSetups()

    suspend fun uploadSetup(setup: SetupItem) {
        dao.insertSetup(setup)
    }

    suspend fun deleteSetup(setup: SetupItem) {
        dao.deleteSetup(setup)
    }

    // --- Creator Analytics Flow ---
    fun getCreatorStats(): Flow<CreatorStats> = flow {
        val wps = dao.getAllWallpapers().first().filter { it.author == "Rahul Shah" }
        val snds = dao.getAllSounds().first().filter { it.artist == "Rahul Shah" }
        val totalUploads = wps.size + snds.size
        val totalDl = wps.sumOf { it.downloadsCount } + snds.sumOf { it.downloadsCount }
        val totalLikes = wps.sumOf { it.likesCount }
        
        emit(
            CreatorStats(
                totalUploads = totalUploads + 5, // Offset to show base creator values
                totalDownloads = totalDl + 10500,
                totalLikes = totalLikes + 4800,
                followersCount = 1290,
                averageQualityScore = 9.7
            )
        )
    }
}
