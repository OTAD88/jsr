package com.example.ui.screens

import android.graphics.BlurMaskFilter
import android.graphics.Color as AndroidColor
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RadialGradient as NativeRadialGradient
import android.graphics.RuntimeShader
import android.graphics.Shader as NativeShader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppearanceMode
import com.example.data.repository.JesrRepository
import com.example.data.repository.StoreThemeItem
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.AmberEmblem
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.SageOliveAccent
import com.example.ui.theme.StoneBackdropDeep
import com.example.ui.theme.CinematicThemesCatalog
import com.example.ui.theme.localizedTitle
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class CustomColorTarget {
    BACKGROUND,
    ICONS,
    LIGHT
}

// Procedural AGSL shader for photorealistic turbulent solar plasma flames rotating with the 3D Sun (API 33+)
private const val REALISTIC_SOLAR_FLAME_AGSL = """
    uniform float2 uCenter;
    uniform float uRadius;
    uniform float uSpinRad;
    uniform float uTime;
    uniform float uAlpha;

    float hash21(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    float noise2(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash21(i);
        float b = hash21(i + float2(1.0, 0.0));
        float c = hash21(i + float2(0.0, 1.0));
        float d = hash21(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float amp = 0.52;
        for (int i = 0; i < 4; i++) {
            v += amp * noise2(p);
            p = p * 2.06 + float2(13.7, 7.3);
            amp *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 d = (fragCoord - uCenter) / max(uRadius, 1.0);
        float r = length(d);
        if (r > 2.35) {
            return half4(0.0);
        }

        // Rotate solar corona, turbulent flames, and photosphere smoothly with uSpinRad
        float theta = atan(d.y, d.x) - uSpinRad;
        float2 dir = float2(cos(theta), sin(theta));

        // Seamless turbulent solar plasma field
        float n1 = fbm(dir * 2.4 + float2(r * 1.3 - uTime * 0.75, uTime * 0.20));
        float n2 = fbm(dir * 5.1 - float2(r * 2.4 - uTime * 1.15, -uTime * 0.35));
        float wisps = fbm(float2(dir.x * 6.5 + n1 * 1.8, dir.y * 6.5 + r * 2.8 - uTime * 1.45));

        float radialOut = max(r - 0.90, 0.0);
        float turbulence = n1 * 0.56 + n2 * 0.28 + wisps * 0.26;

        // Real asymmetric solar flame tongues & prominences licking outward from the photosphere
        float reach = 0.24 + turbulence * 0.88;
        float flameMask = smoothstep(reach, 0.03, radialOut) * smoothstep(0.24, 0.78, turbulence);
        float coronaGlow = exp(-radialOut * 3.4) * 0.62;
        float plasma = clamp(flameMask * 1.15 + coronaGlow * (0.65 + 0.35 * n1), 0.0, 1.0);

        // Blackbody thermal flame color ramp: crimson edge -> fiery orange -> solar gold -> incandescent core
        float3 colCrimson = float3(0.88, 0.19, 0.02);
        float3 colOrange = float3(1.00, 0.50, 0.05);
        float3 colGold = float3(1.00, 0.82, 0.24);
        float3 colWhiteHot = float3(1.00, 0.98, 0.86);

        float3 flameColor = mix(colCrimson, colOrange, smoothstep(0.08, 0.42, plasma));
        flameColor = mix(flameColor, colGold, smoothstep(0.42, 0.78, plasma));
        flameColor = mix(flameColor, colWhiteHot, smoothstep(0.76, 1.0, plasma));

        // 3D Solar Photosphere inside r <= 1.0
        float sphereMask = 1.0 - smoothstep(0.95, 1.03, r);
        float nz = sqrt(max(1.0 - clamp(r, 0.0, 1.0) * clamp(r, 0.0, 1.0), 0.0));
        float surfaceGran = fbm(dir * (0.8 + r * 3.6) + float2(uTime * 0.14, 0.0));
        float3 normal3d = normalize(float3(d.x, d.y, max(nz, 0.05)));
        float3 keyLight = normalize(float3(-0.42, -0.42, 0.80));
        float diffuse = clamp(dot(normal3d, keyLight), 0.0, 1.0);
        float limb = pow(nz, 0.36);

        float3 sphereColor = mix(float3(0.90, 0.28, 0.04), colGold, clamp(limb * 0.85 + diffuse * 0.35, 0.0, 1.0));
        sphereColor = mix(sphereColor, colWhiteHot, smoothstep(0.55, 0.98, diffuse * 0.7 + limb * 0.45));
        sphereColor += (surfaceGran - 0.5) * float3(0.24, 0.14, 0.04) * limb;

        float3 outRgb = mix(flameColor, sphereColor, sphereMask);
        float outAlpha = clamp(max(plasma * (1.0 - sphereMask * 0.35), sphereMask), 0.0, 1.0) * uAlpha;
        return half4(outRgb * outAlpha, outAlpha);
    }
"""

/**
 * Appearance Settings Screen:
 * - AUTO, LIGHT, DARK are strictly fixed & non-editable presets (locked).
 * - Sun & Moon 3D capsule toggle features realistic swirling plasma flame tongues rotating with the 3D Sun.
 * - "واجهاتي" section lists the user's available themes, always including the protected Original Theme (الواجهة الأساسية).
 * - Quick "العودة للثيم الأصلي" button lets the user restore the Original Theme anytime in one tap.
 * - CUSTOM (التخصيص) offers separate, direct color options + Undo.
 */
@Composable
fun AppearanceScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val layoutDirection = LocalLayoutDirection.current
    val slideOffsetX = if (layoutDirection == LayoutDirection.Rtl) 18.dp else (-18).dp
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val appearanceMode by viewModel.appearanceMode.collectAsStateWithLifecycle()
    val customBackgroundHex by viewModel.customBackgroundHex.collectAsStateWithLifecycle()
    val customIconHex by viewModel.customIconHex.collectAsStateWithLifecycle()
    val customLightHex by viewModel.customLightHex.collectAsStateWithLifecycle()
    val customLightEnabled by viewModel.customLightEnabled.collectAsStateWithLifecycle()
    val activeCinematicThemeId by viewModel.activeCinematicThemeId.collectAsStateWithLifecycle()
    val canUndoCustomAppearance by viewModel.canUndoCustomAppearance.collectAsStateWithLifecycle()
    val ownedUserThemes by viewModel.ownedUserThemes.collectAsStateWithLifecycle()
    val justAcquiredThemeId by viewModel.justAcquiredThemeId.collectAsStateWithLifecycle()
    val isReducedMotion = rememberIsReducedMotionEnabled()

    var expandedColorTarget by remember { mutableStateOf<CustomColorTarget?>(null) }

    LaunchedEffect(justAcquiredThemeId) {
        if (justAcquiredThemeId != null) {
            delay(3500)
            viewModel.clearJustAcquiredHighlight()
        }
    }

    val resolvedBgHex = customBackgroundHex ?: JesrRepository.BASE_CUSTOM_BG_HEX
    val resolvedIconHex = customIconHex ?: JesrRepository.BASE_CUSTOM_ICON_HEX
    val resolvedLightHex = customLightHex ?: JesrRepository.BASE_CUSTOM_LIGHT_HEX
    val isOriginalThemeActive = appearanceMode == AppearanceMode.AUTO &&
        activeCinematicThemeId == null &&
        customBackgroundHex == null && customIconHex == null && customLightHex == null && customLightEnabled

    Box(modifier = modifier.fillMaxSize()) {
        EditorialSunlightBackdrop(
            modifier = Modifier.fillMaxSize(),
            isMeshActive = isMeshActive
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditorialFloatingCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = palette.cardSurface,
                contentPadding = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSubScreenHeader(
                        title = strings.appearanceTitle,
                        subtitle = strings.appearanceSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "appearance_back_btn"
                    )

                    // Direct One-Tap Return to Original UI Banner whenever customized or changed
                    AnimatedVisibility(visible = !isOriginalThemeActive) {
                        Button(
                            onClick = {
                                expandedColorTarget = null
                                viewModel.restoreOriginalTheme()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.inkPrimary,
                                contentColor = palette.elevatedSurface
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_restore_original_theme_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = strings.restoreBtn,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.quickRestoreOriginalBtn,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = strings.fixedPresetsHeader,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.inkPrimary
                    )

                    ThemeModeCard(
                        title = strings.autoOriginalTitle,
                        subtitle = strings.autoOriginalSubtitle,
                        icon = Icons.Default.BrightnessAuto,
                        selected = appearanceMode == AppearanceMode.AUTO && activeCinematicThemeId == null,
                        isLocked = true,
                        testTag = "theme_mode_auto",
                        onClick = {
                            expandedColorTarget = null
                            viewModel.restoreOriginalTheme()
                        }
                    )

                    val isDarkActive = when (appearanceMode) {
                        AppearanceMode.DARK -> true
                        AppearanceMode.LIGHT -> false
                        else -> palette.isDark
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThreeDSunMoonToggle(
                            isDark = isDarkActive,
                            isPresetActive = appearanceMode == AppearanceMode.LIGHT || appearanceMode == AppearanceMode.DARK,
                            onToggle = {
                                expandedColorTarget = null
                                viewModel.setAppearanceMode(
                                    if (isDarkActive) AppearanceMode.LIGHT else AppearanceMode.DARK
                                )
                            }
                        )
                    }

                    val activeCinematicSpec = remember(activeCinematicThemeId) {
                        activeCinematicThemeId?.let { CinematicThemesCatalog.findById(it) }
                    }
                    if (activeCinematicSpec != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.accent.copy(alpha = 0.14f))
                                .border(1.1.dp, palette.accent, RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (strings.isEnglish) activeCinematicSpec.titleEn else activeCinematicSpec.titleAr,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = strings.cinematicBadge,
                                        fontSize = 10.5.sp,
                                        color = palette.mutedText
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isDarkActive) palette.inkPrimary else palette.pillSurface)
                                            .clickable { viewModel.applyCinematicTheme(activeCinematicSpec.id, isDark = true) }
                                            .testTag("appearance_cinematic_dark_btn")
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = strings.darkVariantBtn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkActive) palette.elevatedSurface else palette.inkPrimary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (!isDarkActive) palette.inkPrimary else palette.pillSurface)
                                            .clickable { viewModel.applyCinematicTheme(activeCinematicSpec.id, isDark = false) }
                                            .testTag("appearance_cinematic_light_btn")
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = strings.lightVariantBtn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isDarkActive) palette.elevatedSurface else palette.inkPrimary
                                        )
                                    }
                                }
                            }

                            if (activeCinematicSpec.usesFrostLight) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(palette.elevatedSurface.copy(alpha = 0.78f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AcUnit,
                                            contentDescription = strings.snowFrostLightTitle,
                                            tint = palette.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = strings.snowFrostLightTitle,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.inkPrimary
                                            )
                                            Text(
                                                text = if (customLightEnabled) strings.snowFrostEnabledSub else strings.snowFrostDisabledSub,
                                                fontSize = 10.5.sp,
                                                color = palette.mutedText
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = customLightEnabled,
                                        onCheckedChange = { viewModel.setCustomLightEnabled(it) },
                                        modifier = Modifier.testTag("appearance_frost_light_toggle"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PureWhitePill,
                                            checkedTrackColor = palette.accent,
                                            uncheckedThumbColor = palette.inkPrimary,
                                            uncheckedTrackColor = palette.pillSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dedicated List in Appearance for User's Owned/Saved Themes (including Protected Base UI)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.myThemesHeader,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.inkPrimary
                        )
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.THEME_STORE) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .staggeredIconEntrance(
                                    index = 1,
                                    stepDelayMs = 60,
                                    startOffsetX = -slideOffsetX,
                                    startOffsetY = 8.dp,
                                    startScale = 0.8f,
                                    isReducedMotion = isReducedMotion
                                )
                                .testTag("appearance_open_store_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = palette.iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.storeBtn,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appearance_my_themes_section"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
                        ownedUserThemes.forEachIndexed { idx, item ->
                            val isOriginalBase = item.id == JesrRepository.BASE_THEME_ID
                            val rowCinematicSpec = remember(item.id) { CinematicThemesCatalog.findById(item.id) }
                            val isApplied = when {
                                isOriginalBase -> isOriginalThemeActive
                                rowCinematicSpec != null -> activeCinematicThemeId == item.id
                                else -> activeCinematicThemeId == null &&
                                    appearanceMode == AppearanceMode.CUSTOM &&
                                    resolvedBgHex == item.backgroundHex &&
                                    resolvedIconHex == item.iconHex &&
                                    resolvedLightHex == item.lightHex &&
                                    customLightEnabled == item.lightEnabled
                            }
                            val isMine = (currentUid != null && item.creatorId == currentUid) ||
                                item.creatorId == viewModel.myJesrId

                            OwnedThemeRow(
                                item = item,
                                isOriginalBase = isOriginalBase,
                                isMine = isMine,
                                isApplied = isApplied,
                                isJustAcquired = justAcquiredThemeId == item.id,
                                isCurrentModeDark = isDarkActive,
                                entranceIndex = idx + 2,
                                isReducedMotion = isReducedMotion,
                                onActivate = {
                                    expandedColorTarget = null
                                    viewModel.acquireAndApplyStoreTheme(item, isDarkForCinematic = isDarkActive)
                                },
                                onApplyCinematicVariant = if (rowCinematicSpec != null) {
                                    { useDark ->
                                        expandedColorTarget = null
                                        viewModel.applyCinematicTheme(item.id, isDark = useDark)
                                    }
                                } else null,
                                onRemoveDownloaded = if (!isOriginalBase) {
                                    { viewModel.removeDownloadedTheme(item) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = strings.customHeader,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.inkPrimary
                    )

                    ThemeModeCard(
                        title = strings.customTitle,
                        subtitle = strings.customSubtitle,
                        icon = Icons.Default.Palette,
                        selected = appearanceMode == AppearanceMode.CUSTOM,
                        isLocked = false,
                        testTag = "theme_mode_custom",
                        onClick = {
                            viewModel.setAppearanceMode(AppearanceMode.CUSTOM)
                        }
                    )

                    // Editable controls appear strictly in CUSTOM mode only
                    AnimatedVisibility(visible = appearanceMode == AppearanceMode.CUSTOM) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Light Visibility Toggle (Show or Disable Light)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.elevatedSurface)
                                    .border(0.8.dp, palette.border, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(palette.pillSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = strings.lightStatusCd,
                                            tint = palette.iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = strings.showLightTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary
                                        )
                                        Text(
                                            text = if (customLightEnabled) strings.lightEnabledSub else strings.lightDisabledSub,
                                            fontSize = 11.sp,
                                            color = palette.mutedText
                                        )
                                    }
                                }
                                Switch(
                                    checked = customLightEnabled,
                                    onCheckedChange = { viewModel.setCustomLightEnabled(it) },
                                    modifier = Modifier.testTag("custom_light_enabled_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureWhitePill,
                                        checkedTrackColor = palette.accent,
                                        uncheckedThumbColor = palette.inkPrimary,
                                        uncheckedTrackColor = palette.pillSurface
                                    )
                                )
                            }

                            // 2. Three completely separate, independent color options (NO sequential wizard)
                            SeparateColorOptionCard(
                                title = strings.bgColorTitle,
                                subtitle = strings.bgColorSub,
                                currentColorHex = resolvedBgHex,
                                presets = listOf(
                                    0xFFD5C3B0L, 0xFFE8DED1L, 0xFFD6DECAL,
                                    0xFF252E3CL, 0xFF152544L, 0xFF132A24L, 0xFF9E4B2BL
                                ),
                                expanded = expandedColorTarget == CustomColorTarget.BACKGROUND,
                                testTag = "custom_option_bg",
                                applyTestTag = "apply_custom_bg_btn",
                                onToggleExpand = {
                                    expandedColorTarget = if (expandedColorTarget == CustomColorTarget.BACKGROUND) null else CustomColorTarget.BACKGROUND
                                },
                                onApplyColor = { newHex ->
                                    viewModel.setCustomBackgroundColor(newHex)
                                    expandedColorTarget = null
                                }
                            )

                            SeparateColorOptionCard(
                                title = strings.iconColorTitle,
                                subtitle = strings.iconColorSub,
                                currentColorHex = resolvedIconHex,
                                presets = listOf(
                                    0xFF131312L, 0xFF2B221BL, 0xFF1F2B1CL,
                                    0xFFD8E7FFL, 0xFFFFF5E8L, 0xFFF2B632L, 0xFF748654L
                                ),
                                expanded = expandedColorTarget == CustomColorTarget.ICONS,
                                testTag = "custom_option_icons",
                                applyTestTag = "apply_custom_icons_btn",
                                onToggleExpand = {
                                    expandedColorTarget = if (expandedColorTarget == CustomColorTarget.ICONS) null else CustomColorTarget.ICONS
                                },
                                onApplyColor = { newHex ->
                                    viewModel.setCustomIconColor(newHex)
                                    expandedColorTarget = null
                                }
                            )

                            SeparateColorOptionCard(
                                title = strings.lightColorTitle,
                                subtitle = if (customLightEnabled) {
                                    strings.lightColorSubEnabled
                                } else {
                                    strings.lightColorSubDisabled
                                },
                                currentColorHex = resolvedLightHex,
                                presets = listOf(
                                    0xFFFFF5E4L, 0xFFFFEAC4L, 0xFFFFD8A8L,
                                    0xFFD8E7FFL, 0xFF8AB4FFL, 0xFF9DF0CBL, 0xFFF7FCEBL
                                ),
                                expanded = expandedColorTarget == CustomColorTarget.LIGHT,
                                testTag = "custom_option_light",
                                applyTestTag = "apply_custom_light_btn",
                                onToggleExpand = {
                                    expandedColorTarget = if (expandedColorTarget == CustomColorTarget.LIGHT) null else CustomColorTarget.LIGHT
                                },
                                onApplyColor = { newHex ->
                                    viewModel.setCustomLightColor(newHex)
                                    expandedColorTarget = null
                                }
                            )

                            // 3. Protected Non-Deletable Base Color Card (اللون الأساسي لا يحذف)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.cardSurfaceWarm)
                                    .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                                    .testTag("protected_base_color_card")
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(JesrRepository.BASE_CUSTOM_BG_HEX))
                                                .border(1.2.dp, palette.cardSurface, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(JesrRepository.BASE_CUSTOM_ICON_HEX))
                                                .border(1.2.dp, palette.cardSurface, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(JesrRepository.BASE_CUSTOM_LIGHT_HEX))
                                                .border(1.2.dp, palette.cardSurface, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = strings.unDeletableCd,
                                                tint = palette.accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = strings.baseColorTitle,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = palette.inkPrimary
                                            )
                                        }
                                        Text(
                                            text = strings.baseColorSub,
                                            fontSize = 11.sp,
                                            color = palette.mutedText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        expandedColorTarget = null
                                        viewModel.restoreOriginalTheme()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.inkPrimary,
                                        contentColor = palette.elevatedSurface
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("restore_base_color_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.restoreBtn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 4. Undo Action Button (التراجع عن التعديل)
                            Button(
                                onClick = {
                                    expandedColorTarget = null
                                    viewModel.undoCustomAppearance()
                                },
                                enabled = canUndoCustomAppearance,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.pillSurface,
                                    contentColor = palette.inkPrimary,
                                    disabledContainerColor = palette.elevatedSurface,
                                    disabledContentColor = palette.mutedText.copy(alpha = 0.55f)
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("undo_custom_color_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = strings.undoLastChange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (canUndoCustomAppearance) {
                                        strings.undoLastChange
                                    } else {
                                        strings.noUndoChanges
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnedThemeRow(
    item: StoreThemeItem,
    isOriginalBase: Boolean,
    isMine: Boolean,
    isApplied: Boolean,
    isJustAcquired: Boolean = false,
    isCurrentModeDark: Boolean = false,
    entranceIndex: Int = 0,
    isReducedMotion: Boolean = false,
    onActivate: () -> Unit,
    onApplyCinematicVariant: ((Boolean) -> Unit)? = null,
    onRemoveDownloaded: (() -> Unit)? = null
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val layoutDirection = LocalLayoutDirection.current
    val slideOffsetX = if (layoutDirection == LayoutDirection.Rtl) 18.dp else (-18).dp
    val cinematicSpec = remember(item.id) { CinematicThemesCatalog.findById(item.id) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                index = entranceIndex,
                stepDelayMs = 60,
                startOffsetX = slideOffsetX,
                startOffsetY = 12.dp,
                startScale = 0.90f,
                isReducedMotion = isReducedMotion
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    isJustAcquired -> palette.accent.copy(alpha = 0.22f)
                    isApplied -> palette.accent.copy(alpha = 0.14f)
                    else -> palette.elevatedSurface
                }
            )
            .border(
                width = if (isJustAcquired) 2.dp else if (isApplied || isOriginalBase) 1.3.dp else 0.7.dp,
                color = if (isApplied || isJustAcquired) palette.accent else palette.border,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onActivate)
            .testTag("my_theme_card_${item.id}")
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (cinematicSpec != null) {
                MiniCinematicPreviewThumbnail(
                    spec = cinematicSpec,
                    isReducedMotion = isReducedMotion
                )
            } else {
                MiniThemePreviewThumbnail(
                    backgroundHex = item.backgroundHex,
                    iconHex = item.iconHex,
                    lightHex = item.lightHex,
                    lightEnabled = item.lightEnabled
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isOriginalBase) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = strings.unDeletableCd,
                            tint = SageOliveAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = item.localizedTitle(strings.isEnglish),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isJustAcquired) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.downloadedAppliedBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.accent,
                            maxLines = 1
                        )
                    }
                } else {
                    val tagText = when {
                        isOriginalBase -> strings.tagFreeProtected
                        cinematicSpec != null -> "${strings.tagFree} • ${strings.cinematicBadge}"
                        isMine -> strings.tagMyFree
                        else -> strings.tagFree
                    }
                    Text(
                        text = tagText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOriginalBase) SageOliveAccent else palette.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (cinematicSpec == null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isApplied) palette.accent else palette.pillSurface)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isApplied) strings.activeLabel else strings.applyLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isApplied) PureWhitePill else palette.inkPrimary
                    )
                }
            }

            if (!isOriginalBase && onRemoveDownloaded != null) {
                IconButton(
                    onClick = onRemoveDownloaded,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.pillSurface)
                        .testTag("appearance_remove_theme_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = strings.removeDownloadedCd,
                        tint = palette.mutedText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        if (cinematicSpec != null && onApplyCinematicVariant != null) {
            val isDarkApplied = isApplied && isCurrentModeDark
            val isLightApplied = isApplied && !isCurrentModeDark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDarkApplied) palette.accent else palette.pillSurface)
                        .clickable { onApplyCinematicVariant(true) }
                        .testTag("appearance_apply_dark_${item.id}")
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkApplied) Icons.Default.Check else Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = if (isDarkApplied) PureWhitePill else palette.inkPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.darkVariantBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkApplied) PureWhitePill else palette.inkPrimary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isLightApplied) palette.accent else palette.pillSurface)
                        .clickable { onApplyCinematicVariant(false) }
                        .testTag("appearance_apply_light_${item.id}")
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLightApplied) Icons.Default.Check else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = if (isLightApplied) PureWhitePill else palette.inkPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.lightVariantBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLightApplied) PureWhitePill else palette.inkPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeparateColorOptionCard(
    title: String,
    subtitle: String,
    currentColorHex: Long,
    presets: List<Long>,
    expanded: Boolean,
    testTag: String,
    applyTestTag: String,
    onToggleExpand: () -> Unit,
    onApplyColor: (Long) -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current

    val initialHsv = remember(currentColorHex, expanded) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(currentColorHex.toInt(), hsv)
        hsv
    }
    var hue by remember(currentColorHex, expanded) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(currentColorHex, expanded) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(currentColorHex, expanded) { mutableFloatStateOf(initialHsv[2]) }

    val draftColor = remember(hue, sat, value) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.elevatedSurface)
            .border(
                width = if (expanded) 1.3.dp else 0.8.dp,
                color = if (expanded) palette.accent else palette.border,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .testTag(testTag)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(currentColorHex))
                        .border(1.5.dp, palette.inkPrimary.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.inkPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = palette.mutedText
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) strings.hideLabel else strings.pickColorLabel,
                tint = palette.iconColor
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardSurfaceWarm)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { presetHex ->
                        val isSelected = presetHex == currentColorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(presetHex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) palette.accent else palette.border,
                                    shape = CircleShape
                                )
                                .clickable { onApplyColor(presetHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = strings.selectedLabel,
                                    tint = if (Color(presetHex).luminance() > 0.55f) InkBlack else PureWhitePill,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                sat = (offset.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    val pureHue = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(sat * size.width, (1f - value) * size.height),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                        activeTrackColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    )
                )

                val btnContentColor = if (draftColor.luminance() > 0.55f) InkBlack else PureWhitePill
                Button(
                    onClick = {
                        val hex = draftColor.toArgb().toLong() and 0xFFFFFFFFL
                        onApplyColor(hex)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = draftColor,
                        contentColor = btnContentColor
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(applyTestTag)
                ) {
                    Text(
                        text = strings.applyColorPrefix(title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    isLocked: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.14f) else palette.elevatedSurface)
            .border(
                width = if (selected) 1.5.dp else 0.7.dp,
                color = if (selected) palette.accent else palette.border,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) palette.accent else palette.iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = palette.mutedText
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = strings.lockedModeCd,
                    tint = palette.mutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = strings.selectedLabel,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Single capsule toggle with a rotating 3D celestial sphere:
 * - Light Mode (RIGHT): Realistic incandescent 3D Sun rendered with AGSL turbulent swirling solar
 *   plasma & organic flame tongues rotating smoothly with the Sun (or volumetric blurred fallback < API 33).
 * - Dark Mode (LEFT): Slowly rotating 3D Moon with foreshortened craters on a quiet nocturnal backdrop.
 */
@Composable
private fun ThreeDSunMoonToggle(
    isDark: Boolean,
    isPresetActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val darkProgress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
        label = "sun_moon_slide_progress"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 500f),
        label = "sun_moon_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "celestial_3d_rotation")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "celestial_spin_angle"
    )
    val flameTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "solar_flame_plasma_time"
    )
    val effectiveAngle = if (isReducedMotion) 0f else spinAngle
    val effectiveFlameTime = if (isReducedMotion) 1.5f else flameTime

    val solarRuntimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(REALISTIC_SOLAR_FLAME_AGSL)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val trackShape = RoundedCornerShape(50)
    val borderColor = if (isPresetActive) palette.accent else palette.border

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .size(width = 166.dp, height = 74.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(
                    elevation = if (isPressed) 4.dp else 10.dp,
                    shape = trackShape,
                    ambientColor = StoneBackdropDeep.copy(alpha = 0.28f),
                    spotColor = StoneBackdropDeep.copy(alpha = 0.34f)
                )
                .clip(trackShape)
                .border(
                    width = if (isPresetActive) 1.5.dp else 0.9.dp,
                    color = borderColor,
                    shape = trackShape
                )
                .semantics {
                    contentDescription = if (isDark) {
                        strings.sunMoonToggleDarkCd
                    } else {
                        strings.sunMoonToggleLightCd
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = palette.accent),
                    role = Role.Switch,
                    onClick = onToggle
                )
                .testTag("theme_mode_sun_moon_toggle")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cy = h / 2f

                // 1. Matte Studio Backdrop (NO sky/clouds)
                val topTone = lerp(Color(0xFF281F19), Color(0xFF0C121C), darkProgress)
                val bottomTone = lerp(Color(0xFF47362A), Color(0xFF1E2738), darkProgress)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(topTone, bottomTone),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                val dayAlpha = (1f - darkProgress).coerceIn(0f, 1f)
                val nightAlpha = darkProgress.coerceIn(0f, 1f)

                if (nightAlpha > 0.02f) {
                    val nightSpecks = listOf(
                        Triple(0.58f, 0.34f, 1.2f),
                        Triple(0.72f, 0.28f, 1.6f),
                        Triple(0.83f, 0.44f, 1.1f),
                        Triple(0.66f, 0.65f, 1.2f),
                        Triple(0.79f, 0.72f, 1.4f)
                    )
                    nightSpecks.forEach { (sx, sy, sr) ->
                        drawCircle(
                            color = Color(0xFFEAF3FF).copy(alpha = 0.60f * nightAlpha),
                            radius = sr.dp.toPx(),
                            center = Offset(w * sx, h * sy)
                        )
                    }
                }

                // 2. Sliding Sphere Position: Light -> RIGHT, Dark -> LEFT
                val sphereRadius = h * 0.255f
                val edgePadding = h * 0.5f
                val leftX = edgePadding
                val rightX = w - edgePadding
                val cx = leftX + (rightX - leftX) * (1f - darkProgress)
                val sphereCenter = Offset(cx, cy)

                val phaseRad = (effectiveAngle * (PI / 180.0)).toFloat()

                // 3. Realistic Swirling Plasma Sun + Flame Tongues (Right / Light)
                if (dayAlpha > 0.01f) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && solarRuntimeShader != null) {
                        solarRuntimeShader.setFloatUniform("uCenter", cx, cy)
                        solarRuntimeShader.setFloatUniform("uRadius", sphereRadius)
                        solarRuntimeShader.setFloatUniform("uSpinRad", phaseRad)
                        solarRuntimeShader.setFloatUniform("uTime", effectiveFlameTime)
                        solarRuntimeShader.setFloatUniform("uAlpha", dayAlpha)
                        drawCircle(
                            brush = ShaderBrush(solarRuntimeShader),
                            radius = sphereRadius * 2.35f,
                            center = sphereCenter
                        )
                    } else {
                        drawVolumetricPlasmaSunFallback(
                            center = sphereCenter,
                            radius = sphereRadius,
                            spinRad = phaseRad,
                            time = effectiveFlameTime,
                            alpha = dayAlpha
                        )
                    }
                }

                // 4. 3D Rotating MOON (Left / Dark)
                if (nightAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB5D2FF).copy(alpha = 0.42f * nightAlpha),
                                Color(0xFFB5D2FF).copy(alpha = 0.14f * nightAlpha),
                                Color.Transparent
                            ),
                            center = sphereCenter,
                            radius = sphereRadius * 1.85f
                        ),
                        radius = sphereRadius * 1.85f,
                        center = sphereCenter
                    )

                    val sphereClip = Path().apply {
                        addOval(
                            Rect(
                                left = cx - sphereRadius,
                                top = cy - sphereRadius,
                                right = cx + sphereRadius,
                                bottom = cy + sphereRadius
                            )
                        )
                    }
                    clipPath(sphereClip) {
                        val highlightCenter = Offset(
                            cx - sphereRadius * 0.30f,
                            cy - sphereRadius * 0.30f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = nightAlpha),
                                    Color(0xFFE3ECFA).copy(alpha = nightAlpha),
                                    Color(0xFF9EB2D1).copy(alpha = nightAlpha),
                                    Color(0xFF3E4E6B).copy(alpha = nightAlpha)
                                ),
                                center = highlightCenter,
                                radius = sphereRadius * 1.38f
                            ),
                            radius = sphereRadius,
                            center = sphereCenter
                        )

                        val craters = listOf(
                            Triple(0.3, -0.32f, 0.19f),
                            Triple(1.4, 0.18f, 0.25f),
                            Triple(2.6, -0.10f, 0.15f),
                            Triple(3.8, 0.38f, 0.17f),
                            Triple(5.1, -0.22f, 0.21f)
                        )
                        craters.forEach { (lon, latFactor, radiusFactor) ->
                            val theta = lon + phaseRad
                            val facing = cos(theta).toFloat()
                            if (facing > 0.05f) {
                                val fx = cx + sphereRadius * 0.74f * sin(theta).toFloat()
                                val fy = cy + sphereRadius * latFactor
                                val rx = (sphereRadius * radiusFactor * facing).coerceAtLeast(1f)
                                val ry = sphereRadius * radiusFactor

                                drawOval(
                                    color = Color(0xFF566886).copy(alpha = 0.42f * facing * nightAlpha),
                                    topLeft = Offset(fx - rx, fy - ry),
                                    size = Size(rx * 2f, ry * 2f)
                                )
                                drawOval(
                                    color = Color(0xFFF5F9FF).copy(alpha = 0.34f * facing * nightAlpha),
                                    topLeft = Offset(fx - rx, fy - ry),
                                    size = Size(rx * 2f, ry * 2f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Volumetric blurred-plasma fallback for API 24..32 rendering soft organic solar prominences
 * and glowing coronal streams rotating smoothly around the 3D incandescent Sun.
 */
private fun DrawScope.drawVolumetricPlasmaSunFallback(
    center: Offset,
    radius: Float,
    spinRad: Float,
    time: Float,
    alpha: Float
) {
    val cx = center.x
    val cy = center.y

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val glowPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radius * 0.42f, BlurMaskFilter.Blur.NORMAL)
        }

        // Layered asymmetric blurred flame plumes rotating smoothly around the sun
        val plumeCount = 9
        for (i in 0 until plumeCount) {
            val baseAngle = spinRad + i * ((2f * PI.toFloat()) / plumeCount)
            val flutter = sin(time * 2.2f + i * 1.9f) * 0.16f
            val plumeLen = radius * (1.55f + (i % 3) * 0.20f + flutter)
            val spread = 0.30f + (i % 2) * 0.08f
            val tipAngle = baseAngle + sin(time * 1.7f + i) * 0.14f

            val path = NativePath().apply {
                moveTo(cx + cos(baseAngle - spread) * radius * 0.85f, cy + sin(baseAngle - spread) * radius * 0.85f)
                quadTo(
                    cx + cos(baseAngle - spread * 0.3f) * plumeLen * 0.78f,
                    cy + sin(baseAngle - spread * 0.3f) * plumeLen * 0.78f,
                    cx + cos(tipAngle) * plumeLen,
                    cy + sin(tipAngle) * plumeLen
                )
                quadTo(
                    cx + cos(baseAngle + spread * 0.3f) * plumeLen * 0.74f,
                    cy + sin(baseAngle + spread * 0.3f) * plumeLen * 0.74f,
                    cx + cos(baseAngle + spread) * radius * 0.85f,
                    cy + sin(baseAngle + spread) * radius * 0.85f
                )
                close()
            }

            glowPaint.shader = NativeRadialGradient(
                cx, cy, plumeLen,
                intArrayOf(
                    AndroidColor.argb((225 * alpha).toInt(), 255, 214, 79),
                    AndroidColor.argb((195 * alpha).toInt(), 255, 118, 18),
                    AndroidColor.argb((80 * alpha).toInt(), 216, 67, 21),
                    AndroidColor.TRANSPARENT
                ),
                floatArrayOf(0.45f, 0.72f, 0.90f, 1.0f),
                NativeShader.TileMode.CLAMP
            )
            native.drawPath(path, glowPaint)
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFDE7).copy(alpha = alpha),
                Color(0xFFFFD54F).copy(alpha = alpha),
                Color(0xFFFB8C00).copy(alpha = alpha),
                Color(0xFFD84315).copy(alpha = alpha)
            ),
            center = Offset(cx - radius * 0.28f, cy - radius * 0.28f),
            radius = radius * 1.32f
        ),
        radius = radius,
        center = center
    )
}
