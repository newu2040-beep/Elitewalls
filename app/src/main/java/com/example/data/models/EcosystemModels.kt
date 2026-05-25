package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallpapers")
data class WallpaperItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String, // Can be local resource or external high-quality URL
    val author: String = "Rahul Shah",
    val quality: String = "4K", // 1080p, 1440p, 4K, AMOLED
    val width: Int = 3840,
    val height: Int = 2160,
    val category: String, // Minimal, Nature, Anime, Gaming, Cars, Abstract, Cyberpunk, Aesthetic, Dark AMOLED, Space, Technology, Photography
    val isLive: Boolean = false,
    val videoUrl: String? = null,
    val isPremium: Boolean = false,
    val authorAvatar: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120",
    val tags: String = "", // comma separated
    val likesCount: Int = 312,
    val downloadsCount: Int = 189,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
    val syncSoundUrl: String? = null, // Path for wallpapers with synchronised sound
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sounds")
data class SoundItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // Meme, Ringtone, Sound Effect, Ambient
    val durationText: String = "0:15",
    val durationMs: Long = 15000L,
    val artist: String = "Elitewalls Media",
    val soundUrl: String, // Local resource or external audio stream representation
    val downloadsCount: Int = 450,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_collections")
data class CollectionItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val coverUrl: String,
    val wallpaperCount: Int = 0,
    val isPrivate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "setup_items")
data class SetupItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val imageUrl: String,
    val author: String = "Rahul Shah",
    val authorAvatar: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120",
    val deviceModel: String = "Unknown Device",
    val likesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CreatorStats(
    val totalUploads: Int = 8,
    val totalDownloads: Int = 12400,
    val totalLikes: Int = 8900,
    val followersCount: Int = 1840,
    val averageQualityScore: Double = 9.8 // out of 10
)
