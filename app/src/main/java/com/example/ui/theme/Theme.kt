package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldLight,
    onPrimary = NavyDark,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color.White,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = EmeraldContainer,
    background = NavyDark,
    surface = NavyPrimary,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = NavySecondary,
    outline = NavyContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavySecondary,
    onPrimaryContainer = Color.White,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldDark,
    background = SlateLight,
    surface = SlateCard,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    outline = SlateBorder
  )

@Composable
fun ExecutivoGoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

