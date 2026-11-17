package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.data.model.AppearanceMode

@Immutable
data class JesrPalette(
    val isDark: Boolean,
    val accent: Color,
    val iconColor: Color,
    val customBackgroundColor: Color?,
    val lightColor: Color?,
    val isLightEnabled: Boolean,
    val cardSurface: Color,
    val cardSurfaceWarm: Color,
    val elevatedSurface: Color,
    val pillSurface: Color,
    val pillHover: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val mutedText: Color,
    val border: Color,
    val highlight: Color,
    val cinematicTheme: CinematicThemeSpec? = null,
    val cinematicVariant: CinematicThemeVariant? = null
)

val LocalJesrPalette = staticCompositionLocalOf {
    JesrPalette(
        isDark = false,
        accent = SageOliveAccent,
        iconColor = InkBlack,
        customBackgroundColor = null,
        lightColor = null,
        isLightEnabled = true,
        cardSurface = CreamPaper,
        cardSurfaceWarm = CreamPaperWarm,
        elevatedSurface = PureWhitePill,
        pillSurface = TaupePill,
        pillHover = TaupePillHover,
        inkPrimary = InkBlack,
        inkSecondary = InkCharcoal,
        mutedText = StoneMutedText,
        border = PaperBorder,
        highlight = PaperHighlight
    )
}

@Composable
fun JesrTheme(
    appearanceMode: AppearanceMode = AppearanceMode.AUTO,
    customBackgroundHex: Long? = null,
    customIconHex: Long? = null,
    customLightHex: Long? = null,
    customLightEnabled: Boolean = true,
    cinematicThemeId: String? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val cinematicSpec = CinematicThemesCatalog.findById(cinematicThemeId)

    val isDark = when {
        appearanceMode == AppearanceMode.DARK -> true
        appearanceMode == AppearanceMode.LIGHT -> false
        appearanceMode == AppearanceMode.AUTO -> systemDark
        cinematicSpec != null -> customBackgroundHex?.let { Color(it).luminance() < 0.35f } ?: systemDark
        else -> customBackgroundHex?.let { Color(it).luminance() < 0.35f } ?: systemDark
    }

    if (cinematicSpec != null) {
        val variant = if (isDark) cinematicSpec.darkVariant else cinematicSpec.lightVariant
        val colorScheme = if (isDark) {
            darkColorScheme(
                primary = variant.inkPrimary,
                onPrimary = InkBlack,
                primaryContainer = variant.pillSurface,
                onPrimaryContainer = variant.inkPrimary,
                secondary = variant.frameSecondary,
                onSecondary = InkBlack,
                secondaryContainer = variant.pillSurface,
                onSecondaryContainer = variant.inkPrimary,
                tertiary = variant.accent,
                onTertiary = Color.White,
                background = variant.bgMid,
                onBackground = variant.inkPrimary,
                surface = variant.cardSurface,
                onSurface = variant.inkPrimary,
                surfaceVariant = variant.cardSurfaceWarm,
                onSurfaceVariant = variant.mutedText,
                outline = variant.mutedText,
                outlineVariant = variant.border,
                error = TerracottaAccent,
                onError = Color.White,
                errorContainer = TerracottaContainer,
                onErrorContainer = Color(0xFFFFB49A)
            )
        } else {
            lightColorScheme(
                primary = variant.inkPrimary,
                onPrimary = Color.White,
                primaryContainer = variant.pillSurface,
                onPrimaryContainer = variant.inkPrimary,
                secondary = variant.frameSecondary,
                onSecondary = InkBlack,
                secondaryContainer = variant.cardSurfaceWarm,
                onSecondaryContainer = variant.inkSecondary,
                tertiary = variant.accent,
                onTertiary = Color.White,
                background = variant.bgMid,
                onBackground = variant.inkPrimary,
                surface = variant.cardSurface,
                onSurface = variant.inkPrimary,
                surfaceVariant = variant.cardSurfaceWarm,
                onSurfaceVariant = variant.mutedText,
                outline = variant.mutedText,
                outlineVariant = variant.border,
                error = TerracottaAccent,
                onError = Color.White,
                errorContainer = TerracottaContainer,
                onErrorContainer = TerracottaAccent
            )
        }

        val jesrPalette = JesrPalette(
            isDark = isDark,
            accent = variant.accent,
            iconColor = variant.iconColor,
            customBackgroundColor = variant.bgMid,
            lightColor = variant.lightColor,
            isLightEnabled = customLightEnabled,
            cardSurface = variant.cardSurface,
            cardSurfaceWarm = variant.cardSurfaceWarm,
            elevatedSurface = variant.elevatedSurface,
            pillSurface = variant.pillSurface,
            pillHover = variant.pillHover,
            inkPrimary = variant.inkPrimary,
            inkSecondary = variant.inkSecondary,
            mutedText = variant.mutedText,
            border = variant.border,
            highlight = variant.highlight,
            cinematicTheme = cinematicSpec,
            cinematicVariant = variant
        )

        CompositionLocalProvider(LocalJesrPalette provides jesrPalette) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
        return
    }

    val isCustomMode = appearanceMode == AppearanceMode.CUSTOM
    val customBgColor = if (isCustomMode) customBackgroundHex?.let { Color(it) } else null
    val customIconColor = if (isCustomMode) customIconHex?.let { Color(it) } else null
    val customLightColor = if (isCustomMode) customLightHex?.let { Color(it) } else null
    val effectiveLightEnabled = if (isCustomMode) customLightEnabled else true

    val defaultAccent = if (isDark) Color(0xFF7FA8DC) else SageOliveAccent
    val accentColor = customIconColor ?: defaultAccent
    val resolvedIconColor = customIconColor ?: if (isDark) DarkInkPrimary else InkBlack

    val baseCard = if (isDark) Color(0xFF1A2029) else CreamPaper
    val baseCardWarm = if (isDark) Color(0xFF232B36) else CreamPaperWarm
    val baseElevated = if (isDark) Color(0xFF28313E) else PureWhitePill
    val basePill = if (isDark) Color(0xFF2D3746) else TaupePill
    val basePillHover = if (isDark) Color(0xFF394557) else TaupePillHover

    val tintedCard = if (customBgColor != null) lerp(baseCard, customBgColor, 0.24f).copy(alpha = 0.78f) else baseCard
    val tintedCardWarm = if (customBgColor != null) lerp(baseCardWarm, customBgColor, 0.26f).copy(alpha = 0.84f) else baseCardWarm
    val tintedElevated = if (customBgColor != null) lerp(baseElevated, customBgColor, 0.16f).copy(alpha = 0.88f) else baseElevated
    val tintedPill = if (customBgColor != null) lerp(basePill, customBgColor, 0.20f).copy(alpha = 0.86f) else basePill

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = DarkInkPrimary,
            onPrimary = InkBlack,
            primaryContainer = tintedPill,
            onPrimaryContainer = DarkInkPrimary,
            secondary = Color(0xFFB8D0F0),
            onSecondary = InkBlack,
            secondaryContainer = Color(0x337FA8DC),
            onSecondaryContainer = DarkInkPrimary,
            tertiary = accentColor,
            onTertiary = Color.White,
            background = customBgColor ?: Color(0xFF0E131B),
            onBackground = DarkInkPrimary,
            surface = tintedCard,
            onSurface = DarkInkPrimary,
            surfaceVariant = tintedCardWarm,
            onSurfaceVariant = DarkMutedText,
            outline = DarkMutedText,
            outlineVariant = DarkBorderSubtle,
            error = TerracottaAccent,
            onError = Color.White,
            errorContainer = TerracottaContainer,
            onErrorContainer = Color(0xFFFFB49A)
        )
    } else {
        lightColorScheme(
            primary = InkBlack,
            onPrimary = Color.White,
            primaryContainer = tintedPill,
            onPrimaryContainer = InkBlack,
            secondary = AmberEmblem,
            onSecondary = InkBlack,
            secondaryContainer = AmberContainer,
            onSecondaryContainer = InkCharcoal,
            tertiary = accentColor,
            onTertiary = Color.White,
            background = customBgColor ?: StoneBackdropMid,
            onBackground = InkBlack,
            surface = tintedCard,
            onSurface = InkBlack,
            surfaceVariant = tintedCardWarm,
            onSurfaceVariant = StoneMutedText,
            outline = SwatchStoneDark,
            outlineVariant = PaperBorder,
            error = TerracottaAccent,
            onError = Color.White,
            errorContainer = TerracottaContainer,
            onErrorContainer = TerracottaAccent
        )
    }

    val jesrPalette = if (isDark) {
        JesrPalette(
            isDark = true,
            accent = accentColor,
            iconColor = resolvedIconColor,
            customBackgroundColor = customBgColor,
            lightColor = customLightColor,
            isLightEnabled = effectiveLightEnabled,
            cardSurface = tintedCard,
            cardSurfaceWarm = tintedCardWarm,
            elevatedSurface = tintedElevated,
            pillSurface = tintedPill,
            pillHover = basePillHover,
            inkPrimary = Color(0xFFF0F4FA),
            inkSecondary = Color(0xFFC8D2E0),
            mutedText = Color(0xFF93A0B2),
            border = Color(0x28DCE8FA),
            highlight = Color(0x38DCE8FA)
        )
    } else {
        JesrPalette(
            isDark = false,
            accent = accentColor,
            iconColor = resolvedIconColor,
            customBackgroundColor = customBgColor,
            lightColor = customLightColor,
            isLightEnabled = effectiveLightEnabled,
            cardSurface = tintedCard,
            cardSurfaceWarm = tintedCardWarm,
            elevatedSurface = tintedElevated,
            pillSurface = tintedPill,
            pillHover = basePillHover,
            inkPrimary = InkBlack,
            inkSecondary = InkCharcoal,
            mutedText = StoneMutedText,
            border = PaperBorder,
            highlight = PaperHighlight
        )
    }

    CompositionLocalProvider(LocalJesrPalette provides jesrPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
