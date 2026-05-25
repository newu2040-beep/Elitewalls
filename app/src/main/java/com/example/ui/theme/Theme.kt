package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.viewmodel.AppThemeStyle

// --- Lavender ---
private val DarkLavenderScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFF6366F1),
    tertiary = Color(0xFFEC4899),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightLavenderScheme = lightColorScheme(
    primary = Color(0xFF673AB7),
    secondary = Color(0xFF9C27B0),
    tertiary = Color(0xFF512DA8),
    background = Color(0xFFFAF7FC),
    surface = Color(0xFFF3E5F5),
    onPrimary = Color.White,
    onBackground = Color(0xFF311B92),
    onSurface = Color(0xFF311B92)
)

// --- Ocean Blue ---
private val DarkOceanScheme = darkColorScheme(
    primary = Color(0xFF00B0FF),
    secondary = Color(0xFF00E5FF),
    tertiary = Color(0xFF00E676),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFF01579B),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightOceanScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),
    secondary = Color(0xFF00ACC1),
    tertiary = Color(0xFF0288D1),
    background = Color(0xFFF4F9FE),
    surface = Color(0xFFE3F2FD),
    onPrimary = Color.White,
    onBackground = Color(0xFF0D47A1),
    onSurface = Color(0xFF0D47A1)
)

// --- Mint Green ---
private val DarkMintScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    secondary = Color(0xFF1DE9B6),
    tertiary = Color(0xFF76FF03),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFF1B5E20),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightMintScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF009688),
    tertiary = Color(0xFF388E3C),
    background = Color(0xFFF5FAF6),
    surface = Color(0xFFE8F5E9),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20)
)

// --- Peach ---
private val DarkPeachScheme = darkColorScheme(
    primary = Color(0xFFFF9100),
    secondary = Color(0xFFFF3D00),
    tertiary = Color(0xFFFFC400),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFFE65100),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightPeachScheme = lightColorScheme(
    primary = Color(0xFFFB8C00),
    secondary = Color(0xFFF4511E),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFFEF9F5),
    surface = Color(0xFFFFF3E0),
    onPrimary = Color.White,
    onBackground = Color(0xFFE65100),
    onSurface = Color(0xFFE65100)
)

// --- Sakura Pink ---
private val DarkSakuraScheme = darkColorScheme(
    primary = Color(0xFFF50057),
    secondary = Color(0xFFD500F9),
    tertiary = Color(0xFFFF4081),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFF880E4F),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightSakuraScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    secondary = Color(0xFF8E24AA),
    tertiary = Color(0xFFC2185B),
    background = Color(0xFFFEF5F7),
    surface = Color(0xFFFCE4EC),
    onPrimary = Color.White,
    onBackground = Color(0xFF880E4F),
    onSurface = Color(0xFF880E4F)
)

// --- Arctic White ---
private val DarkArcticScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFE0F7FA),
    tertiary = Color(0xFF18FFFF),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFF006064),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightArcticScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    secondary = Color(0xFF006064),
    tertiary = Color(0xFF0097A7),
    background = Color(0xFFF4FCFC),
    surface = Color(0xFFE0F7FA),
    onPrimary = Color.White,
    onBackground = Color(0xFF006064),
    onSurface = Color(0xFF006064)
)

// --- Sunset Orange ---
private val DarkSunsetScheme = darkColorScheme(
    primary = Color(0xFFFF3D00),
    secondary = Color(0xFFFF9100),
    tertiary = Color(0xFFFF6E40),
    background = Color(0xFF0F1115),
    surface = Color(0xFF161920),
    onPrimary = Color(0xFFBF360C),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFCBD5E1)
)
private val LightSunsetScheme = lightColorScheme(
    primary = Color(0xFFD84315),
    secondary = Color(0xFFFFA000),
    tertiary = Color(0xFFE64A19),
    background = Color(0xFFFEF6F4),
    surface = Color(0xFFFBE9E7),
    onPrimary = Color.White,
    onBackground = Color(0xFFBF360C),
    onSurface = Color(0xFFBF360C)
)

// --- Pastel Lavender Theme ---
private val DarkPastelLavenderScheme = darkColorScheme(
    primary = Color(0xFFC7B1F7),
    secondary = Color(0xFFE4DAFC),
    tertiary = Color(0xFFF0E5FF),
    background = Color(0xFF1E1A2B),
    surface = Color(0xFF28233A),
    onPrimary = Color(0xFF381A6F),
    onBackground = Color(0xFFE4DAFC),
    onSurface = Color(0xFFE4DAFC)
)
private val LightPastelLavenderScheme = lightColorScheme(
    primary = Color(0xFF865DFF),
    secondary = Color(0xFFA58BFF),
    tertiary = Color(0xFFC7B1F7),
    background = Color(0xFFFAF7FF),
    surface = Color(0xFFF1EAFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF381A6F),
    onSurface = Color(0xFF381A6F)
)

// --- Pastel Peach Theme ---
private val DarkPastelPeachScheme = darkColorScheme(
    primary = Color(0xFFF7CBB1),
    secondary = Color(0xFFFCEADA),
    tertiary = Color(0xFFFFF0E5),
    background = Color(0xFF2B211A),
    surface = Color(0xFF3A2D23),
    onPrimary = Color(0xFF6F351A),
    onBackground = Color(0xFFFCEADA),
    onSurface = Color(0xFFFCEADA)
)
private val LightPastelPeachScheme = lightColorScheme(
    primary = Color(0xFFFF9E7D),
    secondary = Color(0xFFFFAE94),
    tertiary = Color(0xFFF7CBB1),
    background = Color(0xFFFFF7F2),
    surface = Color(0xFFFFE8D6),
    onPrimary = Color.White,
    onBackground = Color(0xFF6F351A),
    onSurface = Color(0xFF6F351A)
)

// --- Pastel Mint Theme ---
private val DarkPastelMintScheme = darkColorScheme(
    primary = Color(0xFFB1F7D7),
    secondary = Color(0xFFDAFCEB),
    tertiary = Color(0xFFE5FFF2),
    background = Color(0xFF1A2B23),
    surface = Color(0xFF233A30),
    onPrimary = Color(0xFF1A6F44),
    onBackground = Color(0xFFDAFCEB),
    onSurface = Color(0xFFDAFCEB)
)
private val LightPastelMintScheme = lightColorScheme(
    primary = Color(0xFF5DFF9C),
    secondary = Color(0xFF8BFFAA),
    tertiary = Color(0xFFB1F7D7),
    background = Color(0xFFF2FFF8),
    surface = Color(0xFFD6FFE9),
    onPrimary = Color(0xFF1A6F44),
    onBackground = Color(0xFF1A6F44),
    onSurface = Color(0xFF1A6F44)
)

// --- Pastel Rose Theme ---
private val DarkPastelRoseScheme = darkColorScheme(
    primary = Color(0xFFF7B1C7),
    secondary = Color(0xFFFCDAE4),
    tertiary = Color(0xFFFFE5EE),
    background = Color(0xFF2B1A20),
    surface = Color(0xFF3A232B),
    onPrimary = Color(0xFF6F1A33),
    onBackground = Color(0xFFFCDAE4),
    onSurface = Color(0xFFFCDAE4)
)
private val LightPastelRoseScheme = lightColorScheme(
    primary = Color(0xFFFFA5B8),
    secondary = Color(0xFFFFC0CD),
    tertiary = Color(0xFFF7B1C7),
    background = Color(0xFFFFF2F5),
    surface = Color(0xFFFFD6E0),
    onPrimary = Color.White,
    onBackground = Color(0xFF6F1A33),
    onSurface = Color(0xFF6F1A33)
)

// --- Pastel Sky Theme ---
private val DarkPastelSkyScheme = darkColorScheme(
    primary = Color(0xFFB1ECF7),
    secondary = Color(0xFFDAF8FC),
    tertiary = Color(0xFFE5FAFF),
    background = Color(0xFF1A262B),
    surface = Color(0xFF23333A),
    onPrimary = Color(0xFF1A586F),
    onBackground = Color(0xFFDAF8FC),
    onSurface = Color(0xFFDAF8FC)
)
private val LightPastelSkyScheme = lightColorScheme(
    primary = Color(0xFF5DCFFF),
    secondary = Color(0xFF8BE4FF),
    tertiary = Color(0xFFB1ECF7),
    background = Color(0xFFF2FBFF),
    surface = Color(0xFFD6F3FF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A586F),
    onSurface = Color(0xFF1A586F)
)

@Composable
fun ElitewallsTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.LAVENDER,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    adaptiveColor: Color = Color(0xFF03A9F4),
    content: @Composable () -> Unit
) {
    val baseScheme = when (themeStyle) {
        AppThemeStyle.LAVENDER -> if (darkTheme) DarkLavenderScheme else LightLavenderScheme
        AppThemeStyle.OCEAN_BLUE -> if (darkTheme) DarkOceanScheme else LightOceanScheme
        AppThemeStyle.MINT_GREEN -> if (darkTheme) DarkMintScheme else LightMintScheme
        AppThemeStyle.PEACH -> if (darkTheme) DarkPeachScheme else LightPeachScheme
        AppThemeStyle.SAKURA_PINK -> if (darkTheme) DarkSakuraScheme else LightSakuraScheme
        AppThemeStyle.ARCTIC_WHITE -> if (darkTheme) DarkArcticScheme else LightArcticScheme
        AppThemeStyle.SUNSET_ORANGE -> if (darkTheme) DarkSunsetScheme else LightSunsetScheme
        AppThemeStyle.PASTEL_LAVENDER -> if (darkTheme) DarkPastelLavenderScheme else LightPastelLavenderScheme
        AppThemeStyle.PASTEL_PEACH -> if (darkTheme) DarkPastelPeachScheme else LightPastelPeachScheme
        AppThemeStyle.PASTEL_MINT -> if (darkTheme) DarkPastelMintScheme else LightPastelMintScheme
        AppThemeStyle.PASTEL_ROSE -> if (darkTheme) DarkPastelRoseScheme else LightPastelRoseScheme
        AppThemeStyle.PASTEL_SKY -> if (darkTheme) DarkPastelSkyScheme else LightPastelSkyScheme
        AppThemeStyle.DYNAMIC_ADAPTIVE -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = adaptiveColor,
                    secondary = adaptiveColor.copy(alpha = 0.8f),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            } else {
                lightColorScheme(
                    primary = adaptiveColor,
                    secondary = adaptiveColor.copy(alpha = 0.8f),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White
                )
            }
        }
    }

    // Apply pitch-black AMOLED override if darkTheme and isAmoled are active
    val finalScheme = if (darkTheme && isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0A0A)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
