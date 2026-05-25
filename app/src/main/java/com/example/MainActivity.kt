package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.ElitewallsTheme
import com.example.ui.viewmodel.EcosystemViewModel

class MainActivity : FragmentActivity() {
    
    private val viewModel: EcosystemViewModel by viewModels()

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions gracefully for premium seamless workflow
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seamless dynamic permissions request on startup
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())

        setContent {
            val themeStyle by viewModel.activeTheme.collectAsState()
            val isDark by viewModel.isDarkMode.collectAsState()
            val isAmoled by viewModel.isAmoledMode.collectAsState()
            val adaptiveColor by viewModel.activeAdaptiveColor.collectAsState()

            ElitewallsTheme(
                themeStyle = themeStyle,
                darkTheme = isDark,
                isAmoled = isAmoled,
                adaptiveColor = adaptiveColor
            ) {
                MainDashboard(viewModel = viewModel)
            }
        }
    }
}
