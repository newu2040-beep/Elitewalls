package com.example.ui.screens

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import android.net.Uri
import android.os.Build
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.models.CollectionItem
import com.example.data.models.SoundItem
import com.example.data.models.WallpaperItem
import com.example.data.models.SetupItem
import com.example.ui.viewmodel.AppThemeStyle
import com.example.ui.viewmodel.EcosystemViewModel
import com.example.ui.viewmodel.EditorSettings
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect

@Composable
fun LiveVideoPlayer(uri: String, modifier: Modifier = Modifier, isSilent: Boolean = true) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            repeatMode = Player.REPEAT_MODE_ALL
            if (isSilent) volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

// --- Glassmorphic Glass Modifier ---
@Composable
fun Modifier.glassmorphic(
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    cornerRadius: Dp = 24.dp
): Modifier {
    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.15f))
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
}

// --- Dynamic Color Matrix Generator for GPU Styling ---
fun compileColorFiltersMatrix(settings: EditorSettings): ColorMatrix {
    val matrix = ColorMatrix()

    // 1. Saturation
    matrix.setToSaturation(settings.saturation)

    // 2. Custom brightness/contrast and tint logic compiled manually
    val b = settings.brightness * 255f
    val c = settings.contrast

    // Apply amoled blackener
    val darkenerFactor = 1f - settings.amoledDarkener

    // Filter style layers
    var rTint = 1f
    var gTint = 1f
    var bTint = 1f

    if (settings.cyberpunkFilter) {
        rTint = 1.3f
        gTint = 0.5f
        bTint = 1.4f
    } else if (settings.vintageFilter) {
        rTint = 1.2f
        gTint = 1.0f
        bTint = 0.8f
    }

    settings.colorFilterTint?.let { color ->
        rTint *= color.red
        gTint *= color.green
        bTint *= color.blue
    }

    val customMatrix = floatArrayOf(
        c * rTint * darkenerFactor, 0f, 0f, 0f, b,
        0f, c * gTint * darkenerFactor, 0f, 0f, b,
        0f, 0f, c * bTint * darkenerFactor, 0f, b,
        0f, 0f, 0f, 1f, 0f
    )

    val finalMatrix = ColorMatrix()
    finalMatrix.set(matrix)
    finalMatrix.timesAssign(ColorMatrix(customMatrix))
    return finalMatrix
}

fun applyFiltersToBitmap(bitmap: Bitmap, settings: EditorSettings): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val filteredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(filteredBitmap)
    val paint = android.graphics.Paint()

    val matrix = android.graphics.ColorMatrix()
    matrix.setSaturation(settings.saturation)

    val b = settings.brightness * 255f
    val c = settings.contrast
    val darkenerFactor = 1f - settings.amoledDarkener

    var rTint = 1f
    var gTint = 1f
    var bTint = 1f

    if (settings.cyberpunkFilter) {
        rTint = 1.3f
        gTint = 0.5f
        bTint = 1.4f
    } else if (settings.vintageFilter) {
        rTint = 1.2f
        gTint = 1.0f
        bTint = 0.8f
    }

    settings.colorFilterTint?.let { color ->
        rTint *= color.red
        gTint *= color.green
        bTint *= color.blue
    }

    val customMatrix = floatArrayOf(
        c * rTint * darkenerFactor, 0f, 0f, 0f, b,
        0f, c * gTint * darkenerFactor, 0f, 0f, b,
        0f, 0f, c * bTint * darkenerFactor, 0f, b,
        0f, 0f, 0f, 1f, 0f
    )

    val finalMatrix = android.graphics.ColorMatrix()
    finalMatrix.set(matrix)
    finalMatrix.postConcat(android.graphics.ColorMatrix(customMatrix))

    paint.colorFilter = android.graphics.ColorMatrixColorFilter(finalMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    return filteredBitmap
}

fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "upload_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: EcosystemViewModel) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentView by viewModel.currentScreen.collectAsState()

    // Base properties
    val wallpaperList by viewModel.allWallpapers.collectAsState()
    val liveList by viewModel.liveWallpapers.collectAsState()
    val favoritesList by viewModel.favoriteWallpapers.collectAsState()
    val downloadsList by viewModel.downloadedWallpapers.collectAsState()
    val soundsList by viewModel.allSounds.collectAsState()
    val collectionsList by viewModel.allCollections.collectAsState()

    val pActiveSound by viewModel.playActiveSoundId.collectAsState()
    val activeThemeValue by viewModel.activeTheme.collectAsState()
    val darkValue by viewModel.isDarkMode.collectAsState()
    val amoledValue by viewModel.isAmoledMode.collectAsState()

    // Navigation lists mapped to sections
    val drawerItems = listOf(
        Triple("Home", Screen.HOME, Icons.Default.Home),
        Triple("Categories", Screen.CATEGORIES, Icons.Default.Category),
        Triple("Live Wallpapers", Screen.LIVE_WALLPAPERS, Icons.Default.MovieFilter),
        Triple("Setups", Screen.SETUPS, Icons.Default.Devices),
        Triple("Fonts Studio", Screen.FONTS, Icons.Default.FontDownload),
        Triple("Sounds & Ringtones", Screen.SOUNDS, Icons.Default.MusicNote),
        Triple("Favorites", Screen.FAVORITES, Icons.Default.Favorite),
        Triple("Downloads", Screen.DOWNLOADS, Icons.Default.Download),
        Triple("Upload Center", Screen.UPLOAD_CENTER, Icons.Default.CloudUpload),
        Triple("Settings", Screen.SETTINGS, Icons.Default.Settings),
        Triple("About", Screen.ABOUT, Icons.Default.Info)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                // Elite Title Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = "Elitewalls Logo icon",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "ELITEWALLS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Premium Ecosystem v2.5",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Menu items
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(drawerItems) { (name, targetScreen, icon) ->
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = icon, contentDescription = name) },
                            label = { Text(name, fontWeight = FontWeight.SemiBold) },
                            selected = currentView == targetScreen,
                            onClick = {
                                viewModel.navigateTo(targetScreen)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("menu_nav_${name.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Decorative Rahul Shah credits
                Text(
                    text = "Developer Focus: Rahul Shah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentView != Screen.EDITOR && currentView != Screen.APPLY_PREVIEW) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentView) {
                                    Screen.HOME -> "ELITEWALLS"
                                    Screen.CATEGORIES -> "Categories"
                                    Screen.LIVE_WALLPAPERS -> "Live Canvas"
                                    Screen.FONTS -> "Typography Studio"
                                    Screen.SOUNDS -> "Sound Waves"
                                    Screen.FAVORITES -> "Private Vault"
                                    Screen.DOWNLOADS -> "Local Gallery"
                                    Screen.UPLOAD_CENTER -> "Upload Studio"
                                    Screen.SETTINGS -> "Dashboard Settings"
                                    Screen.ABOUT -> "Project Elitewalls"
                                    Screen.DETAIL -> "Aesthetic Canvas"
                                    else -> "ELITEWALLS"
                                },
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("app_bar_title")
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("hamburger_icon_test")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Hamburger menu button"
                                )
                            }
                        },
                        actions = {
                            var showThemeMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Change applet accent pastel palette"
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false },
                                modifier = Modifier.glassmorphic(cornerRadius = 16.dp)
                            ) {
                                AppThemeStyle.values().forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.name.replace("_", " ")) },
                                        onClick = {
                                            viewModel.activeTheme.value = theme
                                            showThemeMenu = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (theme) {
                                                            AppThemeStyle.LAVENDER -> Color(0xFFD1C4E9)
                                                            AppThemeStyle.OCEAN_BLUE -> Color(0xFF90CAF9)
                                                            AppThemeStyle.MINT_GREEN -> Color(0xFFA5D6A7)
                                                            AppThemeStyle.PEACH -> Color(0xFFFFCC80)
                                                            AppThemeStyle.SAKURA_PINK -> Color(0xFFF48FB1)
                                                            AppThemeStyle.ARCTIC_WHITE -> Color(0xFFE0F7FA)
                                                            AppThemeStyle.SUNSET_ORANGE -> Color(0xFFFFAB91)
                                                            AppThemeStyle.PASTEL_LAVENDER -> Color(0xFFC7B1F7)
                                                            AppThemeStyle.PASTEL_PEACH -> Color(0xFFF7CBB1)
                                                            AppThemeStyle.PASTEL_MINT -> Color(0xFFB1F7D7)
                                                            AppThemeStyle.PASTEL_ROSE -> Color(0xFFF7B1C7)
                                                            AppThemeStyle.PASTEL_SKY -> Color(0xFFB1ECF7)
                                                            AppThemeStyle.DYNAMIC_ADAPTIVE -> MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                            )
                                        }
                                    )
                                }
                            }

                            // Dark / Light Toggle icon
                            IconButton(onClick = { viewModel.isDarkMode.value = !darkValue }) {
                                Icon(
                                    imageVector = if (darkValue) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Dark mode toggle switch"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                if (currentView != Screen.EDITOR && currentView != Screen.APPLY_PREVIEW) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("app_bottom_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(
                                    backgroundColor = if (darkValue) Color.Black.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    borderColor = if (darkValue) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    cornerRadius = 32.dp
                                )
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bottomRoutes = listOf(
                                Triple("Home", Screen.HOME, Icons.Default.Home),
                                Triple("Live", Screen.LIVE_WALLPAPERS, Icons.Default.MovieFilter),
                                Triple("Sounds", Screen.SOUNDS, Icons.Default.MusicNote),
                                Triple("Vault", Screen.FAVORITES, Icons.Default.Favorite),
                                Triple("Upload", Screen.UPLOAD_CENTER, Icons.Default.CloudUpload)
                            )
                            val unselectedTint = if (darkValue) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            bottomRoutes.forEach { (name, screen, icon) ->
                                val selected = currentView == screen
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.navigateTo(screen) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = name,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else unselectedTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = name,
                                            color = if (selected) MaterialTheme.colorScheme.primary else unselectedTint,
                                            fontSize = 9.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentView == Screen.HOME) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFA78BFA), // violet-400
                                        Color(0xFF4F46E5)  // indigo-600
                                    )
                                )
                            )
                            .clickable { viewModel.navigateTo(Screen.UPLOAD_CENTER) }
                            .testTag("floating_upload_action"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add uploaded background",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val basePrimary = MaterialTheme.colorScheme.primary
                val baseSecondary = MaterialTheme.colorScheme.secondary
                // Background artistic gradient aura for fluid layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        basePrimary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                radius = 600.dp.toPx(),
                                center = Offset(0f, 0f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        baseSecondary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                ),
                                radius = 400.dp.toPx(),
                                center = Offset(size.width, size.height * 0.8f)
                            )
                        }
                )

                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { 300 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        ) + fadeIn(animationSpec = tween(220)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -300 },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeOut(animationSpec = tween(120))
                    }
                ) { targetScreen ->
                    when (targetScreen) {
                        Screen.HOME -> HomeScreen(viewModel, wallpaperList)
                        Screen.CATEGORIES -> CategoriesScreen(viewModel, wallpaperList)
                        Screen.LIVE_WALLPAPERS -> LiveWallpapersScreen(viewModel, liveList)
                        Screen.SOUNDS -> SoundsScreen(viewModel, soundsList)
                        Screen.FAVORITES -> FavoritesScreen(viewModel, favoritesList, soundsList)
                        Screen.DOWNLOADS -> DownloadsScreen(viewModel, downloadsList)
                        Screen.UPLOAD_CENTER -> UploadCenterScreen(viewModel)
                        Screen.FONTS -> FontsScreen(viewModel)
                        Screen.SETTINGS -> SettingsScreen(viewModel)
                        Screen.ABOUT -> AboutScreen()
                        Screen.DETAIL -> DetailScreen(viewModel)
                        Screen.EDITOR -> EditorScreen(viewModel)
                        Screen.APPLY_PREVIEW -> ApplyPreviewScreen(viewModel)
                        Screen.SETUPS -> SetupsScreen(viewModel)
                    }
                }
            }
        }
    }
}

// ==================== HOME SCREEN ====================
@Composable
fun HomeScreen(viewModel: EcosystemViewModel, list: List<WallpaperItem>) {
    val context = LocalContext.current
    val searchTxt by viewModel.searchQuery.collectAsState()
    val randomCarouselWps = remember(list) { list.shuffled().take(6) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Aesthetic Search bar
        OutlinedTextField(
            value = searchTxt,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search 4K, Dark AMOLED, Cyberpunk backgrounds...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search bar icon") },
            trailingIcon = {
                if (searchTxt.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search query")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .glassmorphic(
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    cornerRadius = 24.dp
                )
                .testTag("home_search_bar"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        val filteredItems = if (searchTxt.isBlank()) list else {
            list.filter {
                it.title.contains(searchTxt, ignoreCase = true) ||
                        it.category.contains(searchTxt, ignoreCase = true) ||
                        it.tags.contains(searchTxt, ignoreCase = true)
            }
        }

        if (searchTxt.isNotBlank()) {
            Text(
                text = "Search Results (${filteredItems.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No backgrounds found matching \"$searchTxt\"")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        WallpaperCard(item = item, onClick = { viewModel.showWallpaperDetail(item) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Featured Hero Card (Editor's Choice) from design theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable {
                        val nebulaWp = list.find { it.title.contains("Nebula", ignoreCase = true) } ?: list.firstOrNull()
                        nebulaWp?.let { viewModel.showWallpaperDetail(it) }
                    }
                    .testTag("featured_hero_card")
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=800",
                    contentDescription = "Deep Nebula",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.9f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Editor's Choice",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deep Nebula",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "By Rahul Shah • 4K Abstract",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 20.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            val targetWp = list.find { it.title.contains("Nebula", ignoreCase = true) } ?: list.firstOrNull()
                            targetWp?.let { viewModel.toggleLikeWallpaper(it) }
                            Toast.makeText(context, "Added Deep Nebula to Private Vault!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite Nebula",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Glass Carousel
            Text(
                text = "Trending Spotlights",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(randomCarouselWps) { wp ->
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.showWallpaperDetail(wp) }
                            .testTag("spotlight_card_${wp.id}")
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(wp.url)
                                .crossfade(true)
                                .build(),
                            contentDescription = wp.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Translucent card tags
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = wp.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = wp.category,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        // Ultra High Def bubble
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(wp.quality, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }



            Spacer(modifier = Modifier.height(12.dp))

            // Recently Uploaded Grid Title
            Text(
                text = "Explore Masterworks",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic grid layout
            val displayedWallpapers = list.take(10)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val chunks = displayedWallpapers.chunked(2)
                chunks.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        chunk.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                WallpaperCard(item = item, onClick = { viewModel.showWallpaperDetail(item) })
                            }
                        }
                        if (chunk.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ==================== WALLPAPER CARD ====================
@Composable
fun WallpaperCard(item: WallpaperItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("wallpaper_item_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.url)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "by ${item.author}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.quality,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==================== CATEGORIES SCREEN ====================
@Composable
fun CategoriesScreen(viewModel: EcosystemViewModel, allList: List<WallpaperItem>) {
    val categories = listOf(
        Pair("Minimal", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=400"),
        Pair("Nature", "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&q=80&w=400"),
        Pair("Anime", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=400"),
        Pair("Gaming", "https://images.unsplash.com/photo-1612287230202-1bf1d85d1bdf?auto=format&fit=crop&q=80&w=400"),
        Pair("Cars", "https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&q=80&w=400"),
        Pair("Abstract", "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&q=80&w=400"),
        Pair("Cyberpunk", "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=400"),
        Pair("Aesthetic", "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&q=80&w=400"),
        Pair("Dark AMOLED", "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&q=80&w=400"),
        Pair("Space", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=400"),
        Pair("Technology", "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&q=80&w=400"),
        Pair("Photography", "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?auto=format&fit=crop&q=80&w=400")
    )

    val activeCat by viewModel.selectedCategory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tag Row Filter
        LazyRow(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val selected = activeCat == "All"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (selected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.selectedCategory.value = "All" }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "All Masterworks",
                        color = if (selected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            items(categories) { (name, _) ->
                val selected = activeCat == name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (selected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.selectedCategory.value = name }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = name,
                        color = if (selected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (activeCat == "All") {
            // Display all category poster cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { (name, cover) ->
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectedCategory.value = name }
                            .testTag("category_cell_$name")
                    ) {
                        AsyncImage(
                            model = cover,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        } else {
            // Filter list items
            val filteredList = allList.filter { it.category.equals(activeCat, ignoreCase = true) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category: $activeCat", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.selectedCategory.value = "All" }) {
                    Text("Show Categories")
                }
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No backgrounds seeded for \"$activeCat\" group.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { item ->
                        WallpaperCard(item = item, onClick = { viewModel.showWallpaperDetail(item) })
                    }
                }
            }
        }
    }
}

// ==================== LIVE WALLPAPERS SCREEN ====================
@Composable
fun LiveWallpapersScreen(viewModel: EcosystemViewModel, liveWps: List<WallpaperItem>) {
    var playingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MovieFilter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Synchronous Live Wallpapers", fontWeight = FontWeight.Bold)
                    Text("Liquid loops play instantly matching high frame-rate rendering", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(liveWps) { wp ->
                val isPlaying = playingId == wp.id
                Box(
                    modifier = Modifier
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { viewModel.showWallpaperDetail(wp) }
                        .testTag("live_canvas_wp_${wp.id}")
                ) {
                    if (isPlaying && wp.isLive && wp.videoUrl != null) {
                        LiveVideoPlayer(uri = wp.videoUrl, modifier = Modifier.fillMaxSize())
                    } else {
                        AsyncImage(
                            model = wp.url,
                            contentDescription = wp.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Wave Play indicator
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (playingId == wp.id) Color.Black.copy(alpha = 0.1f) else Color.Transparent)
                    )

                    IconButton(
                        onClick = {
                            playingId = if (playingId == wp.id) null else wp.id
                            if (playingId != null) {
                                Toast.makeText(viewModel.getApplication(), "Playing synced video loop & ambient track!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playingId == wp.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Simulate active video playback",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(wp.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("🎥 Interactive Wave Loop", color = MaterialTheme.colorScheme.primaryContainer, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== SOUNDS SCREEN ====================
@Composable
fun SoundsScreen(viewModel: EcosystemViewModel, sounds: List<SoundItem>) {
    var selectedTab by remember { mutableStateOf("All") }
    val playSoundId by viewModel.playActiveSoundId.collectAsState()

    var showTrimmer by remember { mutableStateOf<SoundItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("All", "Ringtone", "Meme", "Sound Effect", "Ambient")
            tabs.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab, fontSize = 12.sp) }
                )
            }
        }

        val filteredSounds = if (selectedTab == "All") sounds else {
            sounds.filter { it.category == selectedTab }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredSounds) { item ->
                val isPlaying = playSoundId == item.id
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_effect_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.togglePlaySound(item) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Trigger play/pause icon for sound clip",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${item.artist} • ${item.category}", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.toggleLikeSound(item) }) {
                                Icon(
                                    imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like item icon",
                                    tint = if (item.isLiked) Color.Red else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { viewModel.toggleBookmarkSound(item) }) {
                                Icon(
                                    imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark item icon"
                                )
                            }
                        }

                        // Play Waves simulation
                        if (isPlaying) {
                            Spacer(modifier = Modifier.height(10.dp))
                            SoundWaveformsIndicator()
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.durationText, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showTrimmer = item },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Trim Clip", fontSize = 11.sp)
                                }
                                val coroutineScope = rememberCoroutineScope()
                                val ctx = LocalContext.current
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                saveFileToPublicGallery(ctx, item.soundUrl, item.title, isVideo = false, isAudio = true)
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(ctx, "Download finished! Saved Elitewalls_${item.title}.mp3 to Music folder.", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(ctx, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.toggleBookmarkSound(item)
                                        Toast.makeText(viewModel.getApplication(), "Sound clip \"${item.title}\" saved locally!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Bottom Sheet Audio Trimmer Drawer
        showTrimmer?.let { activeItem ->
            AlertDialog(
                onDismissRequest = { showTrimmer = null },
                confirmButton = {
                    Button(onClick = {
                        viewModel.trimActiveAudioClip()
                        showTrimmer = null
                    }) {
                        Text("Save and Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTrimmer = null }) {
                        Text("Discard")
                    }
                },
                title = { Text("Audio Trimmer: ${activeItem.title}") },
                text = {
                    val trimS by viewModel.trimProgressStart.collectAsState()
                    val trimE by viewModel.trimProgressEnd.collectAsState()

                    Column {
                        Text("Drag edges to choose ringtone range", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            // Trim overlay highlight
                            val highlightStart = trimS
                            val highlightEnd = trimE
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(highlightEnd - highlightStart)
                                    .offset(x = (trimS * 200).dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                            )

                            // Synthetic waveform
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(20) { index ->
                                    val heightFactor = remember { (20..50).random() }
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(heightFactor.dp)
                                            .background(
                                                if (index / 20f >= trimS && index / 20f <= trimE)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Start Offset", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = trimS,
                                    onValueChange = { if (it < trimE - 0.1f) viewModel.trimProgressStart.value = it },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                            Column {
                                Text("End Limit", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = trimE,
                                    onValueChange = { if (it > trimS + 0.1f) viewModel.trimProgressEnd.value = it },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun SoundWaveformsIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
        repeat(34) { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = (300..900).random(), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_scale_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(scale)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        }
    }
}

// ==================== SECURE PRIVATE CRYPT VAULT ====================
fun triggerNativeBiometricAuth(context: android.content.Context, onResult: (Boolean) -> Unit) {
    if (context is androidx.fragment.app.FragmentActivity) {
        val executor = context.mainExecutor
        val biometricPrompt = androidx.biometric.BiometricPrompt(context, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(false)
                }
            })
        
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Vault Authorization")
            .setSubtitle("Confirm biological credentials to access private wallpapers")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    } else {
        onResult(false)
    }
}

@Composable
fun FavoritesScreen(viewModel: EcosystemViewModel, wallFav: List<WallpaperItem>, soundFav: List<SoundItem>) {
    val context = LocalContext.current
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var savedPin by remember { mutableStateOf("1234") } // Default Master Passcode
    var isSettingNewPin by remember { mutableStateOf(false) }
    var tempNewPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(0) } // 0: Enter Old, 1: Enter New, 2: Confirm New
    
    var screenState by remember { mutableStateOf(0) } // 0: Wallpapers, 1: Audios

    val privateImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorageWithExt(context, it, "jpg")
            if (path != null) {
                val isVideo = context.contentResolver.getType(it)?.startsWith("video") == true
                viewModel.uploadPrivateVaultItem(path, isVideo)
            }
        }
    }

    if (!isVaultUnlocked) {
        // --- POLISHED PIN / BIOMETRIC LOCK SCREEN OVERLAY ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Outer Glow ring containing Lock icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Cosmic Private Sandbox Vault Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Aesthetic Private Crypt",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Decrypt offline wallpaper vault & hidden ringtones.\nDefault security code is 1234",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(30.dp))

                // Passcode round dots indicators of typed input
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 36.dp)
                ) {
                    for (i in 0 until 4) {
                        val active = inputPin.length > i
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                        )
                    }
                }

                // Dial Touch pad layout in elegant pastel circle keys
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(280.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("fingerprint", "0", "backspace")
                    )
                    
                    keys.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowKeys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (key == "fingerprint" || key == "backspace") Color.Transparent
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .clickable {
                                            when (key) {
                                                "fingerprint" -> {
                                                    triggerNativeBiometricAuth(context) { success ->
                                                        if (success) {
                                                            isVaultUnlocked = true
                                                            Toast.makeText(context, "System Biometrics verified! Decrypting Crypt...", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Biometric authentication failed or unsupported", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                "backspace" -> {
                                                    if (inputPin.isNotEmpty()) {
                                                        inputPin = inputPin.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (inputPin.length < 4) {
                                                        inputPin += key
                                                        if (inputPin.length == 4) {
                                                            if (inputPin == savedPin) {
                                                                isVaultUnlocked = true
                                                                inputPin = ""
                                                                Toast.makeText(context, "Private Vault Decrypted Succesfully!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Incorrect Security PIN. Please retry.", Toast.LENGTH_SHORT).show()
                                                                inputPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "fingerprint" -> {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Scan Fingerprint",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        "backspace" -> {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- UNLOCKED PRIVATE VAULT CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Elegant header row with setup options and lock action icon
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cosmic Private Sandbox",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Secured Offline Bookmark Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Change PIN option
                    IconButton(
                        onClick = { isSettingNewPin = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Password, contentDescription = "Change Vault PIN code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    // Re-lock option
                    IconButton(
                        onClick = { isVaultUnlocked = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // --- CHANGE PIN FLOW DIALOG ---
            if (isSettingNewPin) {
                AlertDialog(
                    onDismissRequest = { isSettingNewPin = false; tempNewPin = ""; setupStep = 0 },
                    title = { Text("Update Private Crypt PIN") },
                    text = {
                        Column {
                            val promptText = when (setupStep) {
                                0 -> "Enter old 4-digit PIN to proceed"
                                1 -> "Enter your new 4-digit security PIN"
                                else -> "Re-enter new 4-digit security PIN to confirm"
                            }
                            Text(promptText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                            
                            OutlinedTextField(
                                value = tempNewPin,
                                onValueChange = { 
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        tempNewPin = it 
                                    }
                                },
                                label = { Text("PIN Code [4 digits]") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (tempNewPin.length != 4) {
                                    Toast.makeText(context, "PIN must consist of exactly 4 digits", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                when (setupStep) {
                                    0 -> {
                                        if (tempNewPin == savedPin) {
                                            setupStep = 1
                                            tempNewPin = ""
                                        } else {
                                            Toast.makeText(context, "Incorrect old PIN. Please retry.", Toast.LENGTH_SHORT).show()
                                            tempNewPin = ""
                                        }
                                    }
                                    1 -> {
                                        savedPin = tempNewPin // save draft
                                        setupStep = 2
                                        tempNewPin = ""
                                    }
                                    2 -> {
                                        if (tempNewPin == savedPin) {
                                            Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                                            isSettingNewPin = false
                                            tempNewPin = ""
                                            setupStep = 0
                                        } else {
                                            Toast.makeText(context, "Mismatch in confirmation PIN. Restarting process.", Toast.LENGTH_SHORT).show()
                                            tempNewPin = ""
                                            setupStep = 0
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Validate")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isSettingNewPin = false; tempNewPin = ""; setupStep = 0 }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = screenState, modifier = Modifier.padding(bottom = 16.dp)) {
                        Tab(selected = screenState == 0, onClick = { screenState = 0 }) {
                            Text("Wallpapers (${wallFav.size})", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = screenState == 1, onClick = { screenState = 1 }) {
                            Text("Soundboards (${soundFav.size})", modifier = Modifier.padding(12.dp))
                        }
                    }

                    if (screenState == 0) {
                        if (wallFav.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Your Private Vault is empty.\nLike elements on the dashboard to store them here securely.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(wallFav) { item ->
                                    WallpaperCard(item = item, onClick = { viewModel.showWallpaperDetail(item) })
                                }
                            }
                        }
                    } else {
                        val favoritedSounds = soundFav.filter { it.isLiked || it.isBookmarked }
                        if (favoritedSounds.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No saved soundboards or custom ringtones in secure storage.", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(favoritedSounds) { item ->
                                    SoundsScreen(viewModel, listOf(item))
                                }
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { privateImageLauncher.launch("*/*") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add private file to Vault")
                }
            }
        }
    }
}

// ==================== DOWNLOADS SCREEN ====================
@Composable
fun DownloadsScreen(viewModel: EcosystemViewModel, localList: List<WallpaperItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Offline Canvas Crypt",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "These backgrounds are completely stored in your local sandbox cache and can be applied during network blackouts",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (localList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No downloaded themes in sandbox gallery yet!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(localList) { item ->
                    WallpaperCard(item = item, onClick = { viewModel.showWallpaperDetail(item) })
                }
            }
        }
    }
}

// ==================== UPLOAD CENTER SCREEN ====================
// --- METADATA RETRIEVAL HELPERS FOR REAL-TIME DISPLAY ---
fun queryRealFileSize(context: Context, uri: Uri): Long {
    var size: Long = 0
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return size
}

fun decodeImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        Pair(options.outWidth.coerceAtLeast(1), options.outHeight.coerceAtLeast(1))
    } catch (e: Exception) {
        Pair(1920, 1080)
    }
}

fun retrieveMediaDurationAndResolution(context: Context, uri: Uri): Triple<String, String, String> {
    val retriever = android.media.MediaMetadataRetriever()
    var dims = "1920x1080"
    var duration = "0:15"
    var mime = "video/mp4"
    try {
        retriever.setDataSource(context, uri)
        val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (w != null && h != null) {
            dims = "${w}x${h}"
        }
        val durMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        if (durMs > 0) {
            val totalSec = durMs / 1000
            val min = totalSec / 60
            val remainingSec = totalSec % 60
            duration = String.format("%02d:%02d", min, remainingSec)
        }
        val m = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        if (m != null) mime = m
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return Triple(dims, duration, mime)
}

fun copyUriToInternalStorageWithExt(context: Context, uri: Uri, ext: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "upload_${System.currentTimeMillis()}.$ext"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun UploadCenterScreen(viewModel: EcosystemViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Theme Wallpaper, 1: Live Video, 2: Sounds/Ringtones

    // Inputs States
    var uploadTitle by remember { mutableStateOf("") }
    var uploadCategory by remember { mutableStateOf("Minimal") }
    var uploadWidth by remember { mutableStateOf("3840") }
    var uploadHeight by remember { mutableStateOf("2160") }
    var uploadTags by remember { mutableStateOf("creative, 4k, amoled") }
    var uploadArtist by remember { mutableStateOf("Rahul Shah") }

    // Uri selection States
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }

    // Real-time metadata computed states
    var imageSizeText by remember { mutableStateOf("N/A") }
    var imageDimsText by remember { mutableStateOf("N/A") }

    var videoSizeText by remember { mutableStateOf("N/A") }
    var videoDimsText by remember { mutableStateOf("N/A") }
    var videoDurationText by remember { mutableStateOf("0:00") }

    var audioSizeText by remember { mutableStateOf("N/A") }
    var audioDurationText by remember { mutableStateOf("0:00") }

    // Media Player Preview States
    var audioMediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    // File pickers launchers
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            val size = queryRealFileSize(context, uri)
            val mb = String.format("%.2f MB", size.toDouble() / (1024 * 1024))
            imageSizeText = mb
            val dims = decodeImageDimensions(context, uri)
            uploadWidth = dims.first.toString()
            uploadHeight = dims.second.toString()
            imageDimsText = "${dims.first} x ${dims.second}"
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedVideoUri = uri
        if (uri != null) {
            val size = queryRealFileSize(context, uri)
            val mb = String.format("%.2f MB", size.toDouble() / (1024 * 1024))
            videoSizeText = mb
            val meta = retrieveMediaDurationAndResolution(context, uri)
            videoDimsText = meta.first
            videoDurationText = meta.second
            val parts = meta.first.split("x")
            if (parts.size == 2) {
                uploadWidth = parts[0]
                uploadHeight = parts[1]
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAudioUri = uri
        if (uri != null) {
            val size = queryRealFileSize(context, uri)
            val kb = String.format("%.1f KB", size.toDouble() / 1024)
            audioSizeText = kb
            val meta = retrieveMediaDurationAndResolution(context, uri)
            audioDurationText = meta.second
        }
    }

    // Stop audio player if tab or screen changes
    DisposableEffect(activeTab) {
        onDispose {
            audioMediaPlayer?.stop()
            audioMediaPlayer?.release()
            audioMediaPlayer = null
            isAudioPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- LUXURIOUS TAB SELECTOR ---
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            containerColor = Color.Transparent
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wallpaper", style = MaterialTheme.typography.labelMedium)
                }
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Video", style = MaterialTheme.typography.labelMedium)
                }
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ringtone/SFX", style = MaterialTheme.typography.labelMedium)
                }
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Setup", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // --- SUBMISSION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Intro text
                Text(
                    text = when (activeTab) {
                        0 -> "Publish Static Artworks"
                        1 -> "Publish Live Video Wallpapers"
                        2 -> "Publish Ringtones & Sound Effects"
                        else -> "Publish Home Screen Setup"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "All manual uploads are instantly processed, checked for display resolution compatibility and indexed in local SQLite tables.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- TAB CONTROLLER PICKER ACTIONS ---
                when (activeTab) {
                    0, 3 -> { // IMAGE WALLPAPER OR SETUP
                        Button(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri == null) "Choose Image from Gallery" else "Replace Chosen Image")
                        }

                        if (selectedImageUri != null) {
                            // REAL TIME IMAGE METADATA CARD
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Real-Time File Information", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text("File size: $imageSizeText", fontSize = 11.sp)
                                        Text("Resolution: $imageDimsText", fontSize = 11.sp)
                                        Text("Mime Type: image/jpeg", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // VIDEO WALLPAPER
                        Button(
                            onClick = { videoLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedVideoUri == null) "Choose Video from Gallery" else "Replace Chosen Video")
                        }

                        if (selectedVideoUri != null) {
                            // REAL-TIME VIDEO PREVIEW & METADATA CARD
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Dynamic Preview & Metadata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    // INBUILT VIDEO GALLERY PREVIEW PLAYER
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LiveVideoPlayer(uri = selectedVideoUri.toString(), modifier = Modifier.fillMaxSize(), isSilent = false)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Raw File size: $videoSizeText", fontSize = 11.sp)
                                    Text("Video resolution: $videoDimsText", fontSize = 11.sp)
                                    Text("Video duration: $videoDurationText", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    2 -> { // SOUND RINGTONES
                        Button(
                            onClick = { audioLauncher.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedAudioUri == null) "Choose Sound from Gallery" else "Replace Chosen Sound")
                        }

                        if (selectedAudioUri != null) {
                            // REAL-TIME AUDIO METADATA & PREVIEW PLAYER CARD
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Ringtone Preview & Metadata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    // AUDIO PLAYER CONTROLS
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    if (isAudioPlaying) {
                                                        audioMediaPlayer?.pause()
                                                        isAudioPlaying = false
                                                    } else {
                                                        if (audioMediaPlayer == null) {
                                                            audioMediaPlayer = android.media.MediaPlayer().apply {
                                                                setDataSource(context, selectedAudioUri!!)
                                                                prepare()
                                                                isLooping = true
                                                                start()
                                                            }
                                                        } else {
                                                            audioMediaPlayer?.start()
                                                        }
                                                        isAudioPlaying = true
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Could not preview local clip: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play preview",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Selected Ringtone Pitch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Listen to verify rhythm template before uploading", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Audio size: $audioSizeText", fontSize = 11.sp)
                                    Text("Duration: $audioDurationText", fontSize = 11.sp)
                                    Text("Mime Type: audio/mpeg", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // --- SHARED FORM CONTROLS ---
                OutlinedTextField(
                    value = uploadTitle,
                    onValueChange = { uploadTitle = it },
                    label = { Text("Creation/Ringtone Title") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                if (activeTab != 2) {
                    Text("Select Art Category Theme", style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(uploadCategory)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            val categories = listOf("Minimal", "Nature", "Anime", "Gaming", "Cars", "Abstract", "Cyberpunk", "Aesthetic", "Dark AMOLED", "Space")
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    uploadCategory = cat
                                    expanded = false
                                })
                            }
                        }
                    }
                } else {
                    Text("Select Audio Grouping", style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(uploadCategory)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            val categories = listOf("Ringtone", "Meme", "Sound Effect", "Ambient")
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    uploadCategory = cat
                                    expanded = false
                                })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uploadArtist,
                    onValueChange = { uploadArtist = it },
                    label = { Text("Artist Branding Label") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                if (activeTab != 2) {
                    OutlinedTextField(
                        value = uploadTags,
                        onValueChange = { uploadTags = it },
                        label = { Text(if (activeTab == 3) "Device Model (e.g. Pixel 8 Pro)" else "Tags (delimited by commas)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // --- UPLOAD INITIATE BUTTON ---
                Button(
                    onClick = {
                        if (uploadTitle.isBlank()) {
                            Toast.makeText(context, "Failure: Please write title heading.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        when (activeTab) {
                            0 -> { // STATIC WALLPAPER
                                if (selectedImageUri == null) {
                                    Toast.makeText(context, "Please select an image file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val path = copyUriToInternalStorageWithExt(context, selectedImageUri!!, "jpg")
                                if (path != null) {
                                    viewModel.analyzeAndValidateUpload(
                                        title = uploadTitle,
                                        category = uploadCategory,
                                        width = uploadWidth.toIntOrNull() ?: 3840,
                                        height = uploadHeight.toIntOrNull() ?: 2160,
                                        tags = uploadTags,
                                        url = path,
                                        isLive = false,
                                        videoUrl = null
                                    )
                                    // Complete
                                    selectedImageUri = null
                                    uploadTitle = ""
                                    viewModel.navigateTo(Screen.HOME)
                                }
                            }
                            1 -> { // VIDEO WALLPAPER
                                if (selectedVideoUri == null) {
                                    Toast.makeText(context, "Please select a video file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val path = copyUriToInternalStorageWithExt(context, selectedVideoUri!!, "mp4")
                                if (path != null) {
                                    viewModel.analyzeAndValidateUpload(
                                        title = uploadTitle,
                                        category = uploadCategory,
                                        width = uploadWidth.toIntOrNull() ?: 1920,
                                        height = uploadHeight.toIntOrNull() ?: 1080,
                                        tags = uploadTags,
                                        url = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&q=80&w=600", // poster frame thumbnail
                                        isLive = true,
                                        videoUrl = path
                                    )
                                    selectedVideoUri = null
                                    uploadTitle = ""
                                    viewModel.navigateTo(Screen.LIVE_WALLPAPERS)
                                }
                            }
                            2 -> { // SOUND/RINGTONE
                                if (selectedAudioUri == null) {
                                    Toast.makeText(context, "Please select an audio file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val path = copyUriToInternalStorageWithExt(context, selectedAudioUri!!, "mp3")
                                if (path != null) {
                                    viewModel.uploadCustomSound(
                                        title = uploadTitle,
                                        category = uploadCategory,
                                        durationText = audioDurationText,
                                        soundUrl = path,
                                        artist = uploadArtist
                                    )
                                    selectedAudioUri = null
                                    uploadTitle = ""
                                    viewModel.navigateTo(Screen.SOUNDS)
                                }
                            }
                            3 -> { // SETUP
                                if (selectedImageUri == null) {
                                    Toast.makeText(context, "Please select a homescreen image first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val path = copyUriToInternalStorageWithExt(context, selectedImageUri!!, "jpg")
                                if (path != null) {
                                    viewModel.uploadCustomSetup(
                                        title = uploadTitle,
                                        description = "Created by $uploadArtist",
                                        imageUrl = path,
                                        deviceModel = uploadTags.ifBlank { "Unknown Device" }
                                    )
                                    selectedImageUri = null
                                    uploadTitle = ""
                                    // Setups doesn't have a dedicated nav rail yet, we can navigate home
                                    viewModel.navigateTo(Screen.HOME)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Quality Scan & Publish Instantly")
                }
            }
        }
    }
}

// ==================== SHARE AND MANAGEMENT HELPERS ====================
fun shareLocalFile(context: android.content.Context, filePath: String, title: String, isVideo: Boolean = false, isAudio: Boolean = false) {
    if (filePath.isEmpty() || !filePath.startsWith("/")) {
        // Shared URL text fallback
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, "Discover this premium Elitewalls artwork: \"$title\"\nLink: $filePath")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Masterpiece Using"))
        return
    }
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Underlying file not yet fully buffered to disk", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = when {
                isVideo -> "video/mp4"
                isAudio -> "audio/mp3"
                else -> "image/jpeg"
            }
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, "Look at my custom creation \"$title\" uploaded on Elitewalls Sandbox!")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Masterpiece Using"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing exception: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun saveFileToPublicGallery(context: android.content.Context, sourcePath: String, title: String, isVideo: Boolean = false, isAudio: Boolean = false) {
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Elitewalls_$title")
            if (isVideo) {
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/Elitewalls")
                }
            } else if (isAudio) {
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MUSIC + "/Elitewalls")
                }
            } else {
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Elitewalls")
                }
            }
        }

        val baseUri = when {
            isVideo -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            isAudio -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val insertUri = resolver.insert(baseUri, contentValues)
        if (insertUri == null) {
            return
        }

        val outputStream = resolver.openOutputStream(insertUri)
        if (outputStream == null) {
            return
        }

        val isLocalFile = sourcePath.startsWith("/")
        val inputStream = if (isLocalFile) {
            java.io.FileInputStream(java.io.File(sourcePath))
        } else {
            java.net.URL(sourcePath).openStream()
        }

        if (inputStream == null) {
            return
        }

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ==================== SETTINGS SCREEN ====================
@Composable
fun SettingsScreen(viewModel: EcosystemViewModel) {
    val amoledValue by viewModel.isAmoledMode.collectAsState()
    val isSlideshow by viewModel.isSlideshowActive.collectAsState()
    val cacheVal by viewModel.cacheSizeMb.collectAsState()

    var autoChangeFreq by remember { mutableStateOf(2) } // Hours

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("System Preferences & Optimisation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Text("Fine tune battery, caching, and auto scheduler options", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 20.dp))

        // Card list
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("True AMOLED Pitch-Black Mode", fontWeight = FontWeight.Bold)
                        Text("Overwrites canvas backgrounds with solid #000000 to maximize contrast on OLED panels.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = amoledValue, onCheckedChange = { viewModel.isAmoledMode.value = it })
                }
                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interactive Slideshow Loop", fontWeight = FontWeight.Bold)
                        Text("Auto rotates chosen spotlights every 4 seconds dynamically.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = isSlideshow, onCheckedChange = { viewModel.toggleSlideshow() })
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(18.dp))

                Column {
                    Text("Auto Wallpaper Cycle Frequency", fontWeight = FontWeight.Bold)
                    Text("Trigger automatic rotation behind scenes in the system wallpaper client", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = autoChangeFreq.toFloat(),
                        onValueChange = { autoChangeFreq = it.toInt() },
                        valueRange = 1f..24f,
                        steps = 22
                    )
                    Text("Triggers background refresh every $autoChangeFreq hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Purge Image Sandbox Cache", fontWeight = FontWeight.Bold)
                        Text("Active usage: ${String.format("%.1f", cacheVal)} MB storage consumed", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.clearCache() },
                        enabled = cacheVal > 0f,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Dry-Purge")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fresh Start: Purge Demo Data", fontWeight = FontWeight.Bold)
                        Text("Deletes all preloaded mock/seeded elements so you have a clean slate to manually add custom wallpapers.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.purgeDemoData() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Purge Demo")
                    }
                }
            }
        }
    }
}

// ==================== ABOUT SCREEN ====================
@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Project Elitewalls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = "A luxury-level Material 3 high-definition wallpaper platform and audio synthesis customizer designed specifically for Android 12+ devices.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Platform Terms of Distribution:", fontWeight = FontWeight.Bold)
                Text("All vectors, loop videos, and ambient ringtones seeded are royalty-free in respect of creators globally.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animated Credits Section "Made with love by Rahul Shah"
        AnimatedDeveloperCredits()
    }
}

@Composable
fun AnimatedDeveloperCredits() {
    val infiniteTransition = rememberInfiniteTransition(label = "credits_glow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
            .blur(if (alphaAnim < 0.6f) 0.5.dp else 0.0.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = alphaAnim),
                        MaterialTheme.colorScheme.secondary.copy(alpha = alphaAnim)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(22.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Pulse red vector heart detailing love",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Made with love",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "Rahul Shah",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Premium Digital Platform Architect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==================== DETAIL SCREEN ====================
@Composable
fun DetailScreen(viewModel: EcosystemViewModel) {
    val wp by viewModel.selectedWallpaper.collectAsState()
    val isLiked = wp?.isLiked ?: false
    val isBookmarked = wp?.isBookmarked ?: false

    val analyzing by viewModel.aiAnalyzing.collectAsState()
    val analysisResult by viewModel.aiAnalysisResult.collectAsState()

    wp?.let { item ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Interactive Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("detail_hero_canvas")
            ) {
                if (item.isLive && item.videoUrl != null) {
                    LiveVideoPlayer(uri = item.videoUrl, modifier = Modifier.fillMaxSize(), isSilent = false)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Translucent Actions Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("by ${item.author} • ${item.category}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { viewModel.toggleLikeWallpaper(item) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like background trigger toggle",
                                    tint = if (isLiked) Color.Red else Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.toggleBookmarkWallpaper(item) }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark background trigger toggle",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.openApplyPreview() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_apply_action"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Wallpaper")
                }

                OutlinedButton(
                    onClick = { viewModel.openEditorForActive() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("open_editor_action"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tweak Editor")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val coroutineScope = rememberCoroutineScope()
            val currentContext = LocalContext.current
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val path = if (item.isLive) (item.videoUrl ?: item.url) else item.url
                            saveFileToPublicGallery(currentContext, path, item.title, isVideo = item.isLive, isAudio = false)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(currentContext, "Download finished! Saved Elitewalls_${item.title} to storage.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(currentContext, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_artwork_action"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Masterpiece to Device")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.deleteWallpaperItem(item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("delete_artwork_action"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Artwork from Database")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Quality Analyzer Box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Aesthetic Quality Analyzer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Trigger real-time neural diagnostics mapping color distribution, sharpness vector scores, and recommended adjustments.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (analyzing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Querying Gemini API for visual diagnostic report...", fontSize = 12.sp)
                        }
                    } else if (analysisResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(analysisResult!!, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.runAiQualityAnalyzer() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Launch QA Analysis")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // More properties
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Technical Assets", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Resolution Class: ${item.quality} (${item.width} x ${item.height} pixels)")
                    Text("Author Profile: ${item.author}")
                    Text("Storage Stamp: ${(item.timestamp.rem(100).coerceAtLeast(10)) / 10f} MB compressed")
                    Text("Telemetry: ${item.downloadsCount} Downloads • ${item.likesCount} Liked")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tags: ${item.tags}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
    }
}

// ==================== EDITOR SCREEN ====================
@Composable
fun EditorScreen(viewModel: EcosystemViewModel) {
    val wp by viewModel.selectedWallpaper.collectAsState()
    val editor by viewModel.editorState.collectAsState()

    wp?.let { item ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Undo / Redo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return back")
                }
                Text("PRO ADJUSTMENT ENGINE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                Row {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo adjustment layer")
                    }
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo adjustment layer")
                    }
                }
            }

            // Real-time Canvas Renderer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("editor_render_canvas")
            ) {
                // Compile combined Matrix
                val compiledMatrix = remember(editor) { compileColorFiltersMatrix(editor) }

                AsyncImage(
                    model = item.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(compiledMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur((if (editor.blurRadius > 0f) editor.blurRadius else 0f).dp)
                        .graphicsLayer {
                            alpha = 1f
                            clip = true
                        }
                )

                // Neon Glow overlay simulator
                if (editor.neonGlow) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x3F00E5FF),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Grain / Noise simulation overlay
                if (editor.grain > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                // Draw pixel dot spray
                                for (p in 0..120) {
                                    val rX = (0..size.width.toInt()).random().toFloat()
                                    val rY = (0..size.height.toInt()).random().toFloat()
                                    drawCircle(
                                        color = Color.White.copy(alpha = editor.grain * 0.4f),
                                        radius = 1.5.dp.toPx(),
                                        center = Offset(rX, rY)
                                    )
                                }
                            }
                    )
                }

                // Vignette simulation overlay
                if (editor.vignette > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = editor.vignette)
                                    )
                                )
                            )
                    )
                }

                // Glass Blur overlay on dynamic rendering title
                if (editor.glassBlur) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .glassmorphic(cornerRadius = 16.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("GLASS OVERLAY ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Adjustments Panel
            Card(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Aesthetic Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Blur slider
                    SliderControl(
                        label = "Gaussian Soft Blur",
                        value = editor.blurRadius,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(blurRadius = scale) }
                        },
                        valueRange = 0f..25f
                    )

                    // Brightness slider
                    SliderControl(
                        label = "OLED Brightness Offset",
                        value = editor.brightness,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(brightness = scale) }
                        },
                        valueRange = -0.5f..0.5f
                    )

                    // Contrast Slider
                    SliderControl(
                        label = "Deep Contrast",
                        value = editor.contrast,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(contrast = scale) }
                        },
                        valueRange = 0.5f..1.8f
                    )

                    // Saturation
                    SliderControl(
                        label = "Chromatic Saturation",
                        value = editor.saturation,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(saturation = scale) }
                        },
                        valueRange = 0f..2f
                    )

                    // AMOLED Darkener
                    SliderControl(
                        label = "AMOLED Pixel Dimmer",
                        value = editor.amoledDarkener,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(amoledDarkener = scale) }
                        },
                        valueRange = 0f..0.9f
                    )

                    // Vignette
                    SliderControl(
                        label = "Cinematic Vignette Focus",
                        value = editor.vignette,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(vignette = scale) }
                        },
                        valueRange = 0f..1f
                    )

                    // Grain Noise
                    SliderControl(
                        label = "Vintage Grain Noise",
                        value = editor.grain,
                        onValueChange = { scale ->
                            viewModel.updateEditorState { it.copy(grain = scale) }
                        },
                        valueRange = 0f..1f
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Precompiled Style Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterButton(
                            text = "Vintage Hues",
                            active = editor.vintageFilter,
                            onClick = {
                                viewModel.updateEditorState { it.copy(vintageFilter = !editor.vintageFilter, cyberpunkFilter = false) }
                            }
                        )
                        FilterButton(
                            text = "Acid Cyberpunk",
                            active = editor.cyberpunkFilter,
                            onClick = {
                                viewModel.updateEditorState { it.copy(cyberpunkFilter = !editor.cyberpunkFilter, vintageFilter = false) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aesthetic Overlays", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterButton(
                            text = "Glass Frosting",
                            active = editor.glassBlur,
                            onClick = { viewModel.updateEditorState { it.copy(glassBlur = !editor.glassBlur) } }
                        )
                        FilterButton(
                            text = "Neon Glow Flare",
                            active = editor.neonGlow,
                            onClick = { viewModel.updateEditorState { it.copy(neonGlow = !editor.neonGlow) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.navigateTo(Screen.APPLY_PREVIEW)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("apply_custom_canvas_action"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Export to Applying Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
fun FilterButton(text: String, active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
        ),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}

// ==================== APPLY PREVIEW SCREEN ====================
@Composable
fun ApplyPreviewScreen(viewModel: EcosystemViewModel) {
    val wp by viewModel.selectedWallpaper.collectAsState()
    val editor by viewModel.editorState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var applyingInProgress by remember { mutableStateOf(false) }

    wp?.let { item ->
        Box(modifier = Modifier.fillMaxSize()) {
            val compiledMatrix = remember(editor) { compileColorFiltersMatrix(editor) }

            // Fullscreen Dynamic Preview (Video player for Live Video or Static Image)
            if (item.isLive && item.videoUrl != null) {
                LiveVideoPlayer(uri = item.videoUrl, modifier = Modifier.fillMaxSize(), isSilent = false)
            } else {
                AsyncImage(
                    model = item.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(compiledMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur((if (editor.blurRadius > 0f) editor.blurRadius else 0f).dp)
                )
            }

            // Neon glows or overlays
            if (editor.neonGlow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x3F00E5FF), Color.Transparent)
                            )
                        )
                )
            }

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        viewModel.stopAudio()
                        viewModel.navigateBack() 
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Exit preview", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("ACTIVE CANVAS PREVIEW", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(48.dp)) // Equalizer space
            }

            // Centralized Apply Trigger helper
            val setWallpaperRealTime: (String) -> Unit = { target ->
                scope.launch {
                    applyingInProgress = true
                    viewModel.stopAudio() // Stop audio preview upon set wallpaper
                    try {
                        if (item.isLive && item.videoUrl != null) {
                            Toast.makeText(context, "Live video set successfully (Mock: Requires System LiveWallpaperService to persist)", Toast.LENGTH_LONG).show()
                            viewModel.downloadActiveWallpaper()
                            viewModel.navigateTo(Screen.HOME)
                            return@launch
                        }

                        val imageLoader = coil.Coil.imageLoader(context)
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(item.url)
                            .allowHardware(false) // software bitmap for filters representation
                            .build()
                        val result = imageLoader.execute(request)
                        if (result is coil.request.SuccessResult) {
                            val originalBitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                            val filteredBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                applyFiltersToBitmap(originalBitmap, editor)
                            }
                            val wallpaperManager = WallpaperManager.getInstance(context)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    val flags = when (target) {
                                        "home" -> WallpaperManager.FLAG_SYSTEM
                                        "lock" -> WallpaperManager.FLAG_LOCK
                                        else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                    }
                                    wallpaperManager.setBitmap(filteredBitmap, null, true, flags)
                                } else {
                                    wallpaperManager.setBitmap(filteredBitmap)
                                }
                            }
                            Toast.makeText(context, "Successfully set wallpaper in real-time!", Toast.LENGTH_LONG).show()
                            viewModel.downloadActiveWallpaper()
                            viewModel.navigateTo(Screen.HOME)
                        } else {
                            Toast.makeText(context, "Could not digest source background canvas", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Apply error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                        applyingInProgress = false
                    }
                }
            }

            // Bottom application drawer
            Card(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Soundtrack Preview Row
                    val syncAudio = item.syncSoundUrl ?: "https://actions.google.com/sounds/v1/ambient/ocean_waves.ogg"
                    val soundItemForWp = SoundItem(
                        id = "sync_wp_sound_${item.id}",
                        title = "${item.title} Soundtrack",
                        category = "Ambient",
                        artist = item.author,
                        soundUrl = syncAudio
                    )
                    
                    val activePlaySoundId by viewModel.playActiveSoundId.collectAsState()
                    val isPlayingSound = activePlaySoundId == soundItemForWp.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPlayingSound) Icons.Default.MusicVideo else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Acoustic Sound Preview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (item.syncSoundUrl != null) "Synchronized ambient loop" else "Atmospheric sound overlay", color = Color.LightGray, fontSize = 9.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.togglePlaySound(soundItemForWp) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlayingSound) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play sound preview",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Apply Layout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { setWallpaperRealTime("home") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Home Screen", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { setWallpaperRealTime("lock") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lock Screen", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { setWallpaperRealTime("both") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Both Screens")
                    }
                }
            }

            // Processing Loader Overlay
            if (applyingInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Applying High-Fidelity Canvas...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Matching dynamic colors in real-time on device",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ==================== FONTS SCREEN ====================
@Composable
fun FontsScreen(viewModel: EcosystemViewModel) {
    val fontsList by viewModel.allFonts.collectAsState()
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var fontTitle by remember { mutableStateOf("") }
    
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorageWithExt(context, it, "ttf")
            if (path != null) {
                viewModel.uploadFontItem(path, if (fontTitle.isNotBlank()) fontTitle else "Custom Font")
                fontTitle = ""
                isUploading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Typography Studio",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Discover or upload highly aesthetic system fonts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isUploading) {
                OutlinedTextField(
                    value = fontTitle,
                    onValueChange = { fontTitle = it },
                    label = { Text("Font Family Name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { fontPickerLauncher.launch("*/*") }) {
                    Text("Select TTF/OTF")
                }
                IconButton(onClick = { isUploading = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            } else {
                Button(
                    onClick = { isUploading = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload New Font")
                }
            }
        }

        if (fontsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No fonts shared yet.", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(fontsList, key = { it.id }) { fontItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fontItem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Shared by ${fontItem.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { 
                                saveFileToPublicGallery(context, fontItem.fontUrl, fontItem.title)
                                Toast.makeText(context, "Font saved to Downloads", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download Font", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteFontItem(fontItem) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Font", tint = Color.Red.copy(alpha=0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupsScreen(viewModel: EcosystemViewModel) {
    val setupsList by viewModel.allSetups.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Community Setups",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Discover inspiration for your home and lock screen layouts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (setupsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No setups published yet.", style = MaterialTheme.typography.titleMedium)
                    Text("Be the first to share your device setup!", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(setupsList, key = { it.id }) { setup ->
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.45f).clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = setup.imageUrl,
                                contentDescription = setup.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            )
                            Column(
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                            ) {
                                Text(setup.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(setup.deviceModel, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(setup.author, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            
                            // Delete
                            IconButton(
                                onClick = { viewModel.deleteSetupItem(setup) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Setup", tint = Color.Red.copy(alpha=0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
