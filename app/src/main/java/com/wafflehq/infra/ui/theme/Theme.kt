package com.wafflehq.infra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppRole {
    Primary,
    Secondary,
    Tertiary,
    Success,
    Warning,
    Error,
    Neutral,
}

@Immutable
data class RoleColors(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
)

@Immutable
data class AppColors(
    val primary: RoleColors,
    val secondary: RoleColors,
    val tertiary: RoleColors,
    val success: RoleColors,
    val warning: RoleColors,
    val error: RoleColors,
    val neutral: RoleColors,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surface3: Color,
    val outline: Color,
) {
    fun forRole(role: AppRole): RoleColors = when (role) {
        AppRole.Primary   -> primary
        AppRole.Secondary -> secondary
        AppRole.Tertiary  -> tertiary
        AppRole.Success   -> success
        AppRole.Warning   -> warning
        AppRole.Error     -> error
        AppRole.Neutral   -> neutral
    }
}

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val neutral: Color,
    val onNeutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
)

private val LightAppColors = AppColors(
    primary   = RoleColors(Sapphire40,   Color.White, Sapphire90,   Sapphire10),
    secondary = RoleColors(Aquamarine40, Color.White, Aquamarine90, Aquamarine10),
    tertiary  = RoleColors(Amethyst40,   Color.White, Amethyst90,   Amethyst10),
    success   = RoleColors(Emerald40,    Color.White, Emerald90,    Emerald10),
    warning   = RoleColors(Citrine40,    Color.White, Citrine90,    Citrine10),
    error     = RoleColors(Garnet40,     Color.White, Garnet90,     Garnet10),
    neutral   = RoleColors(Graphite40,   Color.White, Graphite90,   Graphite10),
    background       = LightBackground,
    onBackground     = OnSurfaceLight,
    surface          = LightSurface,
    onSurface        = OnSurfaceLight,
    surfaceVariant   = LightSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight,
    surface3         = LightSurface3,
    outline          = OutlineLight,
)

private val LightExtendedColors = ExtendedColors(
    success            = Emerald40,
    onSuccess          = Color.White,
    successContainer   = Emerald90,
    onSuccessContainer = Emerald10,
    warning            = Citrine40,
    onWarning          = Color.White,
    warningContainer   = Citrine90,
    onWarningContainer = Citrine10,
    neutral            = Graphite40,
    onNeutral          = Color.White,
    neutralContainer   = Graphite90,
    onNeutralContainer = Graphite10,
)

private val LightColorScheme = lightColorScheme(
    primary              = Sapphire40,
    onPrimary            = Color.White,
    primaryContainer     = Sapphire90,
    onPrimaryContainer   = Sapphire10,
    secondary            = Aquamarine40,
    onSecondary          = Color.White,
    secondaryContainer   = Aquamarine90,
    onSecondaryContainer = Aquamarine10,
    tertiary             = Amethyst40,
    onTertiary           = Color.White,
    tertiaryContainer    = Amethyst90,
    onTertiaryContainer  = Amethyst10,
    error                = Garnet40,
    onError              = Color.White,
    errorContainer       = Garnet90,
    onErrorContainer     = Garnet10,
    background           = LightBackground,
    onBackground         = OnSurfaceLight,
    surface              = LightSurface,
    onSurface            = OnSurfaceLight,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = OnSurfaceVariantLight,
    outline              = OutlineLight,
    inverseSurface       = DarkSurfaceVariant,
    inverseOnSurface     = LightBackground,
)

private val DarkAppColors = AppColors(
    primary   = RoleColors(Sapphire80,   Sapphire20,   Sapphire30,   Sapphire90),
    secondary = RoleColors(Aquamarine80, Aquamarine20, Aquamarine30, Aquamarine90),
    tertiary  = RoleColors(Amethyst80,   Amethyst20,   Amethyst30,   Amethyst90),
    success   = RoleColors(Emerald80,    Emerald20,    Emerald30,    Emerald90),
    warning   = RoleColors(Citrine80,    Citrine20,    Citrine30,    Citrine90),
    error     = RoleColors(Garnet80,     Garnet20,     Garnet30,     Garnet90),
    neutral   = RoleColors(Graphite80,   Graphite20,   Graphite30,   Graphite90),
    background       = DarkBackground,
    onBackground     = OnSurfaceDark,
    surface          = DarkSurface,
    onSurface        = OnSurfaceDark,
    surfaceVariant   = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantDark,
    surface3         = DarkSurface3,
    outline          = OutlineDark,
)

private val DarkExtendedColors = ExtendedColors(
    success            = Emerald80,
    onSuccess          = Emerald20,
    successContainer   = Emerald30,
    onSuccessContainer = Emerald90,
    warning            = Citrine80,
    onWarning          = Citrine20,
    warningContainer   = Citrine30,
    onWarningContainer = Citrine90,
    neutral            = Graphite80,
    onNeutral          = Graphite20,
    neutralContainer   = Graphite30,
    onNeutralContainer = Graphite90,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Sapphire80,
    onPrimary            = Sapphire20,
    primaryContainer     = Sapphire30,
    onPrimaryContainer   = Sapphire90,
    secondary            = Aquamarine80,
    onSecondary          = Aquamarine20,
    secondaryContainer   = Aquamarine30,
    onSecondaryContainer = Aquamarine90,
    tertiary             = Amethyst80,
    onTertiary           = Amethyst20,
    tertiaryContainer    = Amethyst30,
    onTertiaryContainer  = Amethyst90,
    error                = Garnet80,
    onError              = Garnet20,
    errorContainer       = Garnet30,
    onErrorContainer     = Garnet90,
    background           = DarkBackground,
    onBackground         = OnSurfaceDark,
    surface              = DarkSurface,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = OnSurfaceVariantDark,
    outline              = OutlineDark,
    inverseSurface       = LightBackground,
    inverseOnSurface     = DarkBackground,
)

val LocalAppColors      = staticCompositionLocalOf { LightAppColors }
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightAppTokens = buildAppTokens(LightAppColors)
private val DarkAppTokens  = buildAppTokens(DarkAppColors)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme    = if (darkTheme) DarkColorScheme    else LightColorScheme
    val appColors      = if (darkTheme) DarkAppColors      else LightAppColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val appTokens      = if (darkTheme) DarkAppTokens      else LightAppTokens

    CompositionLocalProvider(
        LocalAppColors      provides appColors,
        LocalExtendedColors provides extendedColors,
        LocalAppTokens      provides appTokens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content,
        )
    }
}

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val tokens: AppTokens
        @Composable @ReadOnlyComposable
        get() = LocalAppTokens.current

    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
