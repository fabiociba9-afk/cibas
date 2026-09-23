package com.aistudio.executivogo.trnsp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * High contrast button colors for dark backgrounds (Navy / Dark Slate).
 * Guarantees crisp white text and icons.
 */
@Composable
fun executiveButtonDarkColors(
    containerColor: Color = NavyPrimary,
    contentColor: Color = Color.White
): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = containerColor.copy(alpha = 0.45f),
    disabledContentColor = contentColor.copy(alpha = 0.5f)
)

/**
 * High contrast button colors for primary brand green (EmeraldAccent).
 * Guarantees crisp white text and icons.
 */
@Composable
fun executiveButtonPrimaryColors(
    containerColor: Color = EmeraldAccent,
    contentColor: Color = Color.White
): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = containerColor.copy(alpha = 0.45f),
    disabledContentColor = contentColor.copy(alpha = 0.5f)
)

/**
 * High contrast button colors for danger / delete actions (RedDanger).
 * Guarantees crisp white text and icons.
 */
@Composable
fun executiveButtonDangerColors(
    containerColor: Color = RedDanger,
    contentColor: Color = Color.White
): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = containerColor.copy(alpha = 0.45f),
    disabledContentColor = contentColor.copy(alpha = 0.5f)
)

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldLight,
    onPrimary = NavyDark,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color.White,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    background = NavyDark,
    surface = NavyPrimary,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = NavySecondary,
    onSurfaceVariant = Color(0xFFE2E8F0),
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
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF8FAFC),
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
    outline = SlateBorder
  )

@Composable
fun ExecutivoGoTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

