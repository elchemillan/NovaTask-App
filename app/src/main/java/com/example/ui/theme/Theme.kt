package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val OriginalLightScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E0EC),
    onSecondaryContainer = Color(0xFF1D1B20),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFDF8FF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3EDF7),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFD0BCFF)
  )

private val OriginalDarkScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF2A282F),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF121115),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF25232A),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
  )

private val EmeraldLightScheme =
  lightColorScheme(
    primary = Color(0xFF006C47),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8CF8C2),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4D6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D7),
    onSecondaryContainer = Color(0xFF0B1F15),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF4FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFF8CF8C2)
  )

private val EmeraldDarkScheme =
  darkColorScheme(
    primary = Color(0xFF70DC96),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFF8CF8C2),
    secondary = Color(0xFFB3CCBC),
    onSecondary = Color(0xFF20352A),
    secondaryContainer = Color(0xFF324B3C),
    onSecondaryContainer = Color(0xFFCFE9D7),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033541),
    tertiaryContainer = Color(0xFF224C58),
    onTertiaryContainer = Color(0xFFBFEAF7),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF005234)
  )

private val RubyLightScheme =
  lightColorScheme(
    primary = Color(0xFFBA1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE0A6),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFFFF7F6),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFFFDAD6)
  )

private val RubyDarkScheme =
  darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFDEC48C),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF564419),
    onTertiaryContainer = Color(0xFFFCE0A6),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDD),
    surface = Color(0xFF191211),
    onSurface = Color(0xFFF1DFDD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF93000A)
  )

private val AmberLightScheme =
  lightColorScheme(
    primary = Color(0xFF725C0C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEE189),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF685E40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E2BC),
    onSecondaryContainer = Color(0xFF221B04),
    tertiary = Color(0xFF45664C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7ECCB),
    onTertiaryContainer = Color(0xFF02210E),
    background = Color(0xFFFFFBF1),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEDE2C8),
    onSurfaceVariant = Color(0xFF4E4734),
    outline = Color(0xFF7F7762),
    outlineVariant = Color(0xFFFEE189)
  )

private val AmberDarkScheme =
  darkColorScheme(
    primary = Color(0xFFE1C56A),
    onPrimary = Color(0xFF3C3000),
    primaryContainer = Color(0xFF564500),
    onPrimaryContainer = Color(0xFFFEE189),
    secondary = Color(0xFFD4C69D),
    onSecondary = Color(0xFF383016),
    secondaryContainer = Color(0xFF4F472B),
    onSecondaryContainer = Color(0xFFF1E2BC),
    tertiary = Color(0xFFABD0B0),
    onTertiary = Color(0xFF173720),
    tertiaryContainer = Color(0xFF2E4E35),
    onTertiaryContainer = Color(0xFFC7ECCB),
    background = Color(0xFF16130C),
    onBackground = Color(0xFFE9E2D5),
    surface = Color(0xFF15130D),
    onSurface = Color(0xFFE9E2D5),
    surfaceVariant = Color(0xFF4E4734),
    onSurfaceVariant = Color(0xFFD1C6AD),
    outline = Color(0xFF9B9079),
    outlineVariant = Color(0xFF564500)
  )

private val OceanLightScheme =
  lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF42A5F5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F8FF),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF03A9F4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1F5FE),
    onTertiaryContainer = Color(0xFF01579B),
    background = Color(0xFFFAFDFE),
    onBackground = Color(0xFF112233),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF112233),
    surfaceVariant = Color(0xFFE6F3FF),
    onSurfaceVariant = Color(0xFF2E4862),
    outline = Color(0xFF90A4AE),
    outlineVariant = Color(0xFFE3F2FD)
  )

private val OceanDarkScheme =
  darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1976D2),
    onSecondaryContainer = Color(0xFFE3F2FD),
    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color(0xFF01579B),
    tertiaryContainer = Color(0xFF0288D1),
    onTertiaryContainer = Color(0xFFE1F5FE),
    background = Color(0xFF0B1424),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF111D30),
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF1A2B44),
    onSurfaceVariant = Color(0xFFBAD0E5),
    outline = Color(0xFF64B5F6),
    outlineVariant = Color(0xFF1565C0)
  )

@Composable
fun MyApplicationTheme(
  themeStyle: String = "PURPLE",
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else -> {
        when (themeStyle.uppercase()) {
          "EMERALD" -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
          "RUBY" -> if (darkTheme) RubyDarkScheme else RubyLightScheme
          "AMBER" -> if (darkTheme) AmberDarkScheme else AmberLightScheme
          "OCEAN" -> if (darkTheme) OceanDarkScheme else OceanLightScheme
          else -> if (darkTheme) OriginalDarkScheme else OriginalLightScheme
        }
      }
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
