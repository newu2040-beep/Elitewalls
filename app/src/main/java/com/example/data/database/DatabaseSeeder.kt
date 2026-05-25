package com.example.data.database

import com.example.data.models.WallpaperItem
import com.example.data.models.SoundItem
import com.example.data.models.CollectionItem

object DatabaseSeeder {

    fun getSeedWallpapers(): List<WallpaperItem> {
        return listOf(
            // Minimal
            WallpaperItem(
                id = "wp_minimal_1",
                title = "Futuristic Prism Curves",
                url = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=1440",
                author = "Lucas Void",
                quality = "4K",
                width = 3840,
                height = 2160,
                category = "Minimal",
                tags = "minimal, curves, glass, elegant, white, pastel"
            ),
            // Nature
            WallpaperItem(
                id = "wp_nature_1",
                title = "Misty Forest Peak",
                url = "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&q=80&w=1440",
                author = "Elena Green",
                quality = "4K",
                width = 3840,
                height = 2560,
                category = "Nature",
                tags = "nature, forest, mystic, dawn, sunbeam, green"
            ),
            // Anime
            WallpaperItem(
                id = "wp_anime_1",
                title = "Neo Tokyo Neon Train",
                url = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=1440",
                author = "Kaito Sato",
                quality = "1440p",
                width = 2560,
                height = 1440,
                category = "Anime",
                tags = "anime, neon, cyberpunk, aesthetic, train, station"
            ),
            // Gaming
            WallpaperItem(
                id = "wp_gaming_1",
                title = "Next-Gen Controller Glow",
                url = "https://images.unsplash.com/photo-1612287230202-1bf1d85d1bdf?auto=format&fit=crop&q=80&w=1440",
                author = "Marcus Player",
                quality = "4K",
                width = 3840,
                height = 2160,
                category = "Gaming",
                tags = "gaming, controller, console, setup, neon, dark"
            ),
            // Cars
            WallpaperItem(
                id = "wp_cars_1",
                title = "Supercar Wet Asphalt Spec",
                url = "https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&q=80&w=1440",
                author = "Rahul Shah",
                quality = "4K",
                width = 4000,
                height = 2250,
                category = "Cars",
                tags = "cars, supercar, porsche, asphalt, reflection, premium"
            ),
            // Abstract
            WallpaperItem(
                id = "wp_abstract_1",
                title = "Liquid Pastel Aurora Flow",
                url = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&q=80&w=1440",
                author = "Sophie Silk",
                quality = "4K",
                width = 3840,
                height = 2160,
                category = "Abstract",
                tags = "abstract, liquid, fluid, gradient, aurora, colorful"
            ),
            // Cyberpunk
            WallpaperItem(
                id = "wp_cyber_1",
                title = "Megacity Rain Grid",
                url = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=1440",
                author = "Nox Net",
                quality = "AMOLED",
                width = 3840,
                height = 2160,
                category = "Cyberpunk",
                tags = "cyberpunk, technology, code, matrix, dark, terminal"
            ),
            // Aesthetic
            WallpaperItem(
                id = "wp_aesthetic_1",
                title = "Dreamy Retro Bloom Spark",
                url = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&q=80&w=1440",
                author = "Lila Moss",
                quality = "1080p",
                width = 1920,
                height = 1080,
                category = "Aesthetic",
                tags = "aesthetic, retro, sparkle, flowers, plant, warm"
            ),
            // Dark AMOLED
            WallpaperItem(
                id = "wp_amoled_1",
                title = "Infinite Deep Dark Spheres",
                url = "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&q=80&w=1440",
                author = "Dark Pixel",
                quality = "AMOLED",
                width = 3840,
                height = 2160,
                category = "Dark AMOLED",
                tags = "amoled, dark, black, spheres, space, minimal"
            ),
            // Space
            WallpaperItem(
                id = "wp_space_1",
                title = "Cosmic Stardust Gateway",
                url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=1440",
                author = "Astroglow",
                quality = "4K",
                width = 3840,
                height = 2400,
                category = "Space",
                tags = "space, nebula, galaxy, stars, earth, glow"
            ),
            // Technology
            WallpaperItem(
                id = "wp_tech_1",
                title = "Cyber Circuit Overlord",
                url = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&q=80&w=1440",
                author = "Silicon Valley",
                quality = "4K",
                width = 3840,
                height = 2160,
                category = "Technology",
                tags = "technology, chip, cpu, board, circuit, glowing"
            ),
            // Photography
            WallpaperItem(
                id = "wp_photo_1",
                title = "Symmetrical Frame Gaze",
                url = "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?auto=format&fit=crop&q=80&w=1440",
                author = "Shutter Snap",
                quality = "1440p",
                width = 2560,
                height = 1700,
                category = "Photography",
                tags = "photography, symmetry, architecture, minimalist, perspective"
            ),

            // Live / Video Wallpapers
            WallpaperItem(
                id = "wp_live_1",
                title = "Dreamy Sea Waves Loop",
                url = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&q=80&w=1080",
                author = "Marina Splash",
                quality = "4K",
                width = 3840,
                height = 2160,
                category = "Nature",
                isLive = true,
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-wave-running-over-the-smooth-sand-of-a-beach-43105-large.mp4",
                syncSoundUrl = "sn_ambient_ocean",
                tags = "live, video, ocean, waves, serene, sound"
            ),
            WallpaperItem(
                id = "wp_live_2",
                title = "Cyberpunk City Drive Loop",
                url = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&q=80&w=1080",
                author = "Neo Driver",
                quality = "AMOLED",
                width = 3840,
                height = 2160,
                category = "Cyberpunk",
                isLive = true,
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-driving-in-a-futuristic-cyberpunk-city-at-night-42200-large.mp4",
                syncSoundUrl = "sn_effect_glitch",
                tags = "live, video, cyberpunk, car, driving, neon"
            ),
            WallpaperItem(
                id = "wp_live_3",
                title = "Calm Starry Night Sky",
                url = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&q=80&w=1080",
                author = "Zen Astro",
                quality = "1440p",
                width = 2560,
                height = 1440,
                category = "Space",
                isLive = true,
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-star-trails-glowing-in-the-night-sky-40292-large.mp4",
                syncSoundUrl = "sn_ambient_stars",
                tags = "live, video, stars, night, galaxy, space"
            )
        )
    }

    fun getSeedSounds(): List<SoundItem> {
        return listOf(
            SoundItem(
                id = "sn_ringtone_futuristic",
                title = "Neo-Synth Cyber Wave",
                category = "Ringtone",
                durationText = "0:30",
                durationMs = 30000L,
                artist = "DJ Rahul",
                soundUrl = "ringtone_synth"
            ),
            SoundItem(
                id = "sn_ringtone_minimal",
                title = "Pastel Zen Harp",
                category = "Ringtone",
                durationText = "0:25",
                durationMs = 25000L,
                artist = "Sato Breeze",
                soundUrl = "ringtone_harp"
            ),
            SoundItem(
                id = "sn_meme_bruh",
                title = "Bruh Ultimate Reverb",
                category = "Meme",
                durationText = "0:02",
                durationMs = 2000L,
                artist = "Internet Legend",
                soundUrl = "meme_bruh"
            ),
            SoundItem(
                id = "sn_meme_emotional",
                title = "Violin Sad Cinematic",
                category = "Meme",
                durationText = "0:12",
                durationMs = 12000L,
                artist = "Meme Orchestra",
                soundUrl = "meme_violin"
            ),
            SoundItem(
                id = "sn_effect_glitch",
                title = "Digital Reality Glitch",
                category = "Sound Effect",
                durationText = "0:05",
                durationMs = 5000L,
                artist = "Nox Lab",
                soundUrl = "effect_glitch"
            ),
            SoundItem(
                id = "sn_ambient_ocean",
                title = "Tidal Whispers Ambient",
                category = "Ambient",
                durationText = "5:00",
                durationMs = 300000L,
                artist = "Marina Zen",
                soundUrl = "ambient_ocean"
            ),
            SoundItem(
                id = "sn_ambient_stars",
                title = "Infinite Celestial Breath",
                category = "Ambient",
                durationText = "4:30",
                durationMs = 270000L,
                artist = "Space Breath",
                soundUrl = "ambient_stars"
            )
        )
    }

    fun getSeedCollections(): List<CollectionItem> {
        return listOf(
            CollectionItem(
                id = "col_amoled",
                name = "Deep AMOLED Space",
                description = "Pure black canvases embedded with brilliant lighting vectors.",
                coverUrl = "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&q=80&w=400",
                wallpaperCount = 5
            ),
            CollectionItem(
                id = "col_cyberpunk",
                name = "Cyberpunk Neo Tokyo",
                description = "Streets drenched in neon magenta, dark rain, and code prisms.",
                coverUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=400",
                wallpaperCount = 4
            ),
            CollectionItem(
                id = "col_minimalist",
                name = "Premium Minimalist",
                description = "Gorgeously soft shadows, clean geometry, and expansive negative space.",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=400",
                wallpaperCount = 6
            )
        )
    }
}
