package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    onPrimary = OnPurple80,
    secondary = PurpleContainer,
    onSecondary = Purple80,
    background = DarkBackground,
    onBackground = TextLight,
    surface = DarkBackground,
    onSurface = TextLight,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextMuted,
    outline = SurfaceVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    onPrimary = OnPurple40,
    secondary = PurpleContainerLight,
    onSecondary = OnPurpleContainerLight,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightBackground,
    onSurface = TextDark,
    surfaceVariant = LightSurface,
    onSurfaceVariant = TextDarkMuted,
    outline = OutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false, // Set to false to enforce theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
