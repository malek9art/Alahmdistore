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

private val DarkColorScheme = darkColorScheme(
  primary = GoldAccent,
  secondary = TealLight,
  tertiary = GoldAccentLight,
  background = TealDark,
  surface = TealMedium,
  onPrimary = TealDark,
  onSecondary = TextWhite,
  onTertiary = TealDark,
  onBackground = TextWhite,
  onSurface = TextWhite,
  error = AlertError
)

private val LightColorScheme = lightColorScheme(
  primary = TealLight,
  secondary = TealMedium,
  tertiary = GoldAccent,
  background = NeutralLight,
  surface = TextWhite,
  onPrimary = TextWhite,
  onSecondary = TextWhite,
  onTertiary = TealDark,
  onBackground = TealDark,
  onSurface = TealDark,
  error = AlertError
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Choose theme dynamically
  dynamicColor: Boolean = false, // Disable dynamic color to maintain strict Alahmadi visual identity
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
