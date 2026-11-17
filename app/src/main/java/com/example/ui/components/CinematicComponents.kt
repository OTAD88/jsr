package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeliveryStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberEmblem
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CreamPaper
import com.example.ui.theme.CreamPaperWarm
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.OchreAccent
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PeekingPanelSurface
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.SageOliveAccent
import com.example.ui.theme.SlateDarkAccent
import com.example.ui.theme.StoneBackdropDeep
import com.example.ui.theme.StoneBackdropLight
import com.example.ui.theme.StoneBackdropMid
import com.example.ui.theme.StoneBackdropShadow
import com.example.ui.theme.StoneMutedText
import com.example.ui.theme.SwatchStoneDark
import com.example.ui.theme.SwatchStoneMid
import com.example.ui.theme.TaupePill
import com.example.ui.theme.TaupePillHover
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.Screen

val LocalHasRootBackdrop = staticCompositionLocalOf { false }

/**
 * Checks system accessibility settings to determine if animations/motion should be reduced.
 */
@Composable
fun rememberIsReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            val animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f || animatorScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Full-bleed architectural studio backdrop recreating the exact sunlit gobo shaft & soft
 * feathered shadows of the reference image: warm sunlit plaster at upper-left, soft feathered
 * penumbra edges, a diagonal warm sunlight beam slicing down-right, and deep espresso-taupe
 * shadows in the lower-left and upper-right/right zones.
 */
@Composable
fun EditorialSunlightBackdrop(
    modifier: Modifier = Modifier,
    isMeshActive: Boolean = true,
    currentScreen: Screen = Screen.HOME,
    forceRender: Boolean = false
) {
    if (!forceRender && LocalHasRootBackdrop.current) return
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val transition = rememberInfiniteTransition(label = "sunlight_drift")
    val lightShift by transition.animateFloat(
        initialValue = if (isReducedMotion) 0f else -0.022f,
        targetValue = if (isReducedMotion) 0f else 0.022f,
        animationSpec = infiniteRepeatable(
            animation = tween(9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunlight_shift"
    )
    val glowPulse by transition.animateFloat(
        initialValue = if (isReducedMotion) 1.0f else 0.96f,
        targetValue = if (isReducedMotion) 1.0f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_glow_pulse"
    )
    val dustDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dark_dust_drift"
    )

    // Precompute deterministic subtle photographic film grain & sunlit dust motes
    val grainPoints = remember {
        val list = ArrayList<Triple<Float, Float, Float>>(180)
        var seed = 1337
        for (i in 0 until 180) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val x = (seed % 1000) / 1000f
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val y = (seed % 1000) / 1000f
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val alpha = 0.018f + ((seed % 100) / 100f) * 0.035f
            list.add(Triple(x, y, alpha))
        }
        list
    }

    // Sparse delicate dust motes specifically for nocturnal Dark Mode atmosphere
    val darkDustMotes = remember {
        val list = ArrayList<Triple<Float, Float, Float>>(20)
        var seed = 4099
        for (i in 0 until 20) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val x = 0.08f + ((seed % 840) / 1000f)
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val y = 0.07f + ((seed % 860) / 1000f)
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val sizeWeight = 0.85f + ((seed % 100) / 100f) * 1.15f
            list.add(Triple(x, y, sizeWeight))
        }
        list
    }

    val palette = LocalJesrPalette.current
    val activeCinematic = palette.cinematicTheme
    val isDark = palette.isDark
    val customBg = palette.customBackgroundColor
    val customLight = palette.lightColor
    val isLightEnabled = palette.isLightEnabled

    if (activeCinematic != null) {
        Box(modifier = modifier.fillMaxSize()) {
            CinematicSceneCanvas(
                spec = activeCinematic,
                isDark = isDark,
                currentScreen = currentScreen,
                lightEnabled = isLightEnabled,
                isReducedMotion = isReducedMotion,
                modifier = Modifier.fillMaxSize()
            )
            val particleKind = particleKindForTheme(activeCinematic, isDark, isLightEnabled)
            val particleIntensity = if (activeCinematic.usesFrostLight && !isLightEnabled) 0.14f else 0.92f
            GyroParticleOverlay(
                particleKind = particleKind,
                accentColor = palette.accent,
                lightColor = customLight ?: palette.accent,
                isDark = isDark,
                intensity = particleIntensity,
                isReducedMotion = isReducedMotion,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (isDark) {
            // Exact structural mirror of Light Mode, transformed from Sunlight into cool silver-blue Moonlight
            val baseColors = if (customBg != null) {
                listOf(
                    lerp(customBg, customLight ?: Color(0xFF8AA8D6), 0.25f),
                    lerp(customBg, Color(0xFF1F2734), 0.35f),
                    lerp(customBg, Color(0xFF10151E), 0.60f),
                    lerp(customBg, Color(0xFF06080D), 0.80f)
                )
            } else {
                listOf(
                    Color(0xFF3A4659),
                    Color(0xFF252E3C),
                    Color(0xFF151B25),
                    Color(0xFF090C12)
                )
            }

            // 1. Nocturnal studio plaster tone
            drawRect(
                brush = Brush.linearGradient(
                    colors = baseColors,
                    start = Offset(w * 0.12f, h * 0.06f),
                    end = Offset(w * 0.95f, h * 0.95f)
                )
            )

            if (isLightEnabled) {
                // 2. Exact 42-degree diagonal gobo moonlight/custom light shaft & feathered nocturnal shadows
                val moonCore = customLight ?: Color(0xFFD8E7FF)
                val moonMid = customLight?.let { lerp(it, Color.White, 0.25f) } ?: Color(0xFFE6F0FF)
                val moonEdge = customLight?.let { lerp(it, Color(0xFF4C5871), 0.45f) } ?: Color(0xFF6B90C4)

                drawRect(
                    brush = Brush.linearGradient(
                        0.00f to Color(0xEE06080E),
                        0.18f to Color(0xDE0F141E),
                        0.30f to Color(0xAC1E2736),
                        0.40f to moonEdge.copy(alpha = 0.32f),
                        0.49f to moonCore.copy(alpha = 0.26f),
                        0.60f to moonCore.copy(alpha = 0.40f),
                        0.70f to Color(0x80253042),
                        0.84f to Color(0xD6111622),
                        1.00f to Color(0xEC070A10),
                        start = Offset(0f, h * (0.96f + lightShift)),
                        end = Offset(w * 0.98f, h * (0.04f + lightShift))
                    )
                )

                // 3. Upper-left Moon/Light-source radial bloom
                val moonEntry = Offset(w * (0.14f + lightShift * 0.6f), h * 0.10f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            moonMid.copy(alpha = 0.56f),
                            moonCore.copy(alpha = 0.32f),
                            moonEdge.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = moonEntry,
                        radius = w * 0.86f * glowPulse
                    ),
                    radius = w * 0.86f * glowPulse,
                    center = moonEntry
                )

                // 4. Soft diagonal sheen traveling down-right through the shaft
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            moonMid.copy(alpha = if (isMeshActive) 0.26f else 0.16f),
                            moonCore.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color(0x7005070C)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }
        } else {
            val baseColors = if (customBg != null) {
                listOf(
                    lerp(customBg, customLight ?: Color(0xFFFFF8EC), 0.32f),
                    customBg,
                    lerp(customBg, Color(0xFF6B5A49), 0.34f),
                    lerp(customBg, Color(0xFF282019), 0.66f)
                )
            } else {
                listOf(
                    Color(0xFFE7D7C7),
                    Color(0xFFD5C3B0),
                    Color(0xFFA6927E),
                    Color(0xFF5D4E41)
                )
            }

            // 1. Base studio plaster tone
            drawRect(
                brush = Brush.linearGradient(
                    colors = baseColors,
                    start = Offset(w * 0.12f, h * 0.06f),
                    end = Offset(w * 0.95f, h * 0.95f)
                )
            )

            if (isLightEnabled) {
                val sunCore = customLight ?: Color(0xFFF5E9DA)
                val sunBright = customLight?.let { lerp(it, Color.White, 0.35f) } ?: Color(0xFFFFF8EC)
                val sunEdge = customLight?.let { lerp(it, Color(0xFFA48F7B), 0.35f) } ?: Color(0xFFE5CAA6)

                // 2. Photographic 42-degree diagonal gobo sunlight/custom light & feathered shadows
                drawRect(
                    brush = Brush.linearGradient(
                        0.00f to Color(0xEC211B16),
                        0.18f to Color(0xDC372D25),
                        0.30f to Color(0xA6675647),
                        0.40f to Color(0x40A48F7B),
                        0.49f to sunCore.copy(alpha = if (customLight != null) 0.18f else 0.04f),
                        0.60f to sunCore.copy(alpha = if (customLight != null) 0.34f else 0.18f),
                        0.70f to Color(0x787B6958),
                        0.84f to Color(0xD244382E),
                        1.00f to Color(0xEA241D17),
                        start = Offset(0f, h * (0.96f + lightShift)),
                        end = Offset(w * 0.98f, h * (0.04f + lightShift))
                    )
                )

                // 3. Secondary warm sun/custom light radial bloom from upper-left window opening
                val sunEntry = Offset(w * (0.14f + lightShift * 0.6f), h * 0.10f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sunBright.copy(alpha = 0.58f),
                            sunCore.copy(alpha = 0.34f),
                            sunEdge.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = sunEntry,
                        radius = w * 0.86f * glowPulse
                    ),
                    radius = w * 0.86f * glowPulse,
                    center = sunEntry
                )

                // 4. Soft diagonal light warmth traveling down-right through the shaft
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            sunBright.copy(alpha = if (isMeshActive) 0.28f else 0.18f),
                            sunEdge.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color(0x6616120F)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }
        }

        // 5. Subtle studio vignette & analog grain / dust motes
        val moteColor = customLight?.copy(alpha = 0.34f)
            ?: if (isDark) Color(0x52EAF3FF) else Color(0x40FFF8EA)

        grainPoints.forEachIndexed { idx, (gx, gy, alpha) ->
            val beamCoord = (gx * 0.55f) + ((1f - gy) * 0.45f)
            val inBeamShaft = beamCoord in 0.40f..0.68f
            if (isLightEnabled && idx % 10 == 0 && inBeamShaft) {
                drawCircle(
                    color = moteColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(gx * w, gy * h)
                )
            } else {
                drawCircle(
                    color = if (idx % 2 == 0) {
                        Color.White.copy(alpha = alpha)
                    } else {
                        InkBlack.copy(alpha = alpha * 1.15f)
                    },
                    radius = 0.95.dp.toPx(),
                    center = Offset(gx * w, gy * h)
                )
            }
        }

        // 6. Sparse atmospheric dust motes in Dark Mode
        if (isDark) {
            val baseDustTint = customLight ?: Color(0xFFDCEBFF)
            darkDustMotes.forEachIndexed { idx, (dx, dy, weight) ->
                val direction = if (idx % 2 == 0) 1f else -1f
                val driftOffsetX = (dustDrift - 0.5f) * direction * (14.dp.toPx())
                val driftOffsetY = (dustDrift - 0.5f) * (-direction) * (10.dp.toPx())
                val center = Offset(
                    x = (dx * w + driftOffsetX).coerceIn(0f, w),
                    y = (dy * h + driftOffsetY).coerceIn(0f, h)
                )
                val moteRadius = (0.95f * weight).dp.toPx()
                val alpha = 0.18f + (idx % 4) * 0.06f

                // Soft tiny halo on a few larger dust specks
                if (idx % 3 == 0) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                baseDustTint.copy(alpha = alpha * 0.75f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = moteRadius * 3.4f
                        ),
                        radius = moteRadius * 3.4f,
                        center = center
                    )
                }
                drawCircle(
                    color = baseDustTint.copy(alpha = alpha),
                    radius = moteRadius,
                    center = center
                )
            }
        }
    }
    GyroParticleOverlay(
        particleKind = particleKindForTheme(null, isDark, isLightEnabled),
        accentColor = palette.accent,
        lightColor = customLight ?: if (isDark) Color(0xFFDCEBFF) else Color(0xFFFFF6D6),
        isDark = isDark,
        intensity = if (isLightEnabled || isDark) 0.72f else 0.25f,
        isReducedMotion = isReducedMotion,
        modifier = Modifier.fillMaxSize()
    )
    }
}

/**
 * Primary floating editorial cream card (`RoundedCornerShape(28.dp)`) with soft studio shadow
 * cast down-right away from the upper-left sun source, sunlit top-left rim, and natural
 * diagonal gobo light & shadow falling softly across the card surface.
 */
@Composable
fun EditorialFloatingCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    backgroundColor: Color = LocalJesrPalette.current.cardSurface,
    elevation: Dp = 20.dp,
    contentPadding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalJesrPalette.current
    val resolvedBg = if (backgroundColor == CreamPaper && palette.isDark) {
        palette.cardSurface
    } else {
        backgroundColor
    }
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.988f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "floating_card_scale"
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    val isTranslucentCard = resolvedBg.alpha < 0.95f

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (!isTranslucentCard) {
                    Modifier
                        .drawBehind {
                            val corner = 28.dp.toPx()
                            drawRoundRect(
                                color = StoneBackdropDeep.copy(alpha = if (palette.isDark) 0.45f else 0.28f),
                                topLeft = Offset(10.dp.toPx(), 16.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(corner, corner)
                            )
                            drawRoundRect(
                                color = StoneBackdropShadow.copy(alpha = if (palette.isDark) 0.30f else 0.22f),
                                topLeft = Offset(18.dp.toPx(), 26.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        }
                        .shadow(
                            elevation = elevation,
                            shape = shape,
                            ambientColor = StoneBackdropDeep.copy(alpha = 0.55f),
                            spotColor = StoneBackdropDeep.copy(alpha = 0.65f)
                        )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(resolvedBg)
            .then(clickMod)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (palette.isDark) {
                        listOf(Color(0x36FFFDF7), palette.highlight, Color(0x10000000))
                    } else {
                        listOf(Color(0xCCFFFFFF), PaperHighlight, Color(0x2828221D))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(900f, 1400f)
                ),
                shape = shape
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (!palette.isLightEnabled || palette.cinematicTheme != null) return@Canvas
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val customLight = palette.lightColor

            if (palette.isDark) {
                val cardGlow = customLight ?: Color(0xFFE8F2FF)
                val cardEdge = customLight?.let { lerp(it, Color(0xFF4C5871), 0.4f) } ?: Color(0xFFB0CBEF)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cardGlow.copy(alpha = 0.22f),
                            cardEdge.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.22f, h * 0.16f),
                        radius = w * 0.92f
                    ),
                    radius = w * 0.92f,
                    center = Offset(w * 0.22f, h * 0.16f)
                )
                drawRect(
                    brush = Brush.linearGradient(
                        0.00f to Color(0x3605070B),
                        0.22f to Color(0x200B1018),
                        0.38f to Color(0x00FFFFFF),
                        0.62f to cardGlow.copy(alpha = 0.15f),
                        0.74f to Color(0x140E1420),
                        1.00f to Color(0x2E05070B),
                        start = Offset(0f, h * 0.92f),
                        end = Offset(w * 0.96f, h * 0.06f)
                    )
                )
            } else {
                val cardGlow = customLight ?: Color(0xFFFFFDF8)
                val cardWarm = customLight?.let { lerp(it, Color.White, 0.25f) } ?: Color(0xFFFFF5E4)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cardGlow.copy(alpha = 0.30f),
                            cardWarm.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.22f, h * 0.16f),
                        radius = w * 0.92f
                    ),
                    radius = w * 0.92f,
                    center = Offset(w * 0.22f, h * 0.16f)
                )
                drawRect(
                    brush = Brush.linearGradient(
                        0.00f to Color(0x2E3B3026),
                        0.22f to Color(0x1C5C4C3E),
                        0.38f to Color(0x00FFFFFF),
                        0.62f to cardGlow.copy(alpha = 0.14f),
                        0.74f to Color(0x105E4F41),
                        1.00f to Color(0x263A2F26),
                        start = Offset(0f, h * 0.92f),
                        end = Offset(w * 0.96f, h * 0.06f)
                    )
                )
            }
        }

        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Compatibility GlassCard styled cleanly into the Editorial Paper palette.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = CreamPaper,
    borderColor: Color = PaperBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    EditorialFloatingCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        elevation = 10.dp,
        contentPadding = 16.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * Minimal geometric brand emblem (amber circle + clean dark ring, zero letters/numbers).
 */
@Composable
fun JesrBrandEmblem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(AmberEmblem),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(
                color = InkBlack,
                radius = size.minDimension / 2.2f,
                style = Stroke(width = 2.2.dp.toPx())
            )
            drawCircle(
                color = InkBlack,
                radius = 2.dp.toPx()
            )
        }
    }
}

/**
 * Circular icon button with spring press compression, down-right physical depress into shadow,
 * and Material ripple feedback.
 */
@Composable
fun EditorialInteractiveIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalJesrPalette.current.pillSurface,
    contentColor: Color = LocalJesrPalette.current.iconColor,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    testTag: String? = null
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.87f else 1.0f,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 520f),
        label = "icon_btn_scale"
    )
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 520f),
        label = "icon_btn_offset"
    )
    val animatedContainer by animateColorAsState(
        targetValue = if (isPressed) TaupePillHover else containerColor,
        animationSpec = tween(120),
        label = "icon_btn_bg"
    )

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = pressOffsetY.toPx()
            }
            .clip(CircleShape)
            .background(animatedContainer)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                onClick = onClick
            )
            .then(tagModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Elevated pure-white capsule pill with dramatic soft studio drop shadow and responsive press motion.
 */
@Composable
fun ElevatedHeroPill(
    title: String,
    subtitle: String? = null,
    isActive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "elevated_hero_pill"
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "hero_pill_scale"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f),
        label = "hero_pill_offset"
    )
    val pillColor by animateColorAsState(
        targetValue = if (isPressed) CreamPaper else PureWhitePill,
        animationSpec = tween(110),
        label = "hero_pill_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = pressOffset.toPx() * 0.4f
                translationY = pressOffset.toPx()
            }
            // Deep realistic directional shadow under the raised white pill (matching reference Oisclosure shadow)
            .drawBehind {
                val radius = size.height / 2f
                val shadowFactor = if (isPressed) 0.55f else 1.0f
                drawRoundRect(
                    color = StoneBackdropDeep.copy(alpha = 0.22f * shadowFactor),
                    topLeft = Offset(3.dp.toPx() * shadowFactor, 10.dp.toPx() * shadowFactor),
                    size = size,
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = StoneBackdropShadow.copy(alpha = 0.14f * shadowFactor),
                    topLeft = Offset(7.dp.toPx() * shadowFactor, 16.dp.toPx() * shadowFactor),
                    size = size,
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 16.dp,
                shape = RoundedCornerShape(50),
                ambientColor = StoneBackdropShadow.copy(alpha = 0.50f),
                spotColor = StoneBackdropShadow.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(50))
            .background(pillColor)
            .border(0.8.dp, Color(0x18000000), RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = InkBlack),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinematicLiveBeacon(
                isActive = isActive,
                activeColor = SageOliveAccent,
                inactiveColor = SwatchStoneDark
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = StoneMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Subtle vertical divider + crisp arrow matching the reference card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(PaperBorder)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = title,
                tint = InkBlack,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Minimal list row with circular muted swatch + title + trailing arrow or Cobalt counter (`+98 more`).
 */
@Composable
fun SwatchListRow(
    title: String,
    swatchColor: Color,
    subtitle: String? = null,
    trailingBadge: String? = null,
    trailingBadgeColor: Color = CobaltAccent,
    showArrow: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "swatch_row"
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 500f),
        label = "swatch_row_scale"
    )
    val rowBg by animateColorAsState(
        targetValue = if (isPressed) TaupePill.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(100),
        label = "swatch_row_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = InkBlack),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = StoneMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!trailingBadge.isNullOrBlank()) {
            Text(
                text = trailingBadge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = trailingBadgeColor
            )
        } else if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = InkCharcoal,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * Warm taupe pill bar matching the "Show more v" / "Condition" stacked capsules in the reference image.
 */
@Composable
fun TaupePillRow(
    title: String,
    subtitle: String? = null,
    isExpanded: Boolean? = null,
    backgroundColor: Color = TaupePill,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "taupe_pill_row"
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 520f),
        label = "taupe_pill_scale"
    )
    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) TaupePillHover else backgroundColor,
        animationSpec = tween(100),
        label = "taupe_pill_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = InkBlack),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = InkCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = StoneMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val icon = trailingIcon ?: when (isExpanded) {
            true -> Icons.Default.KeyboardArrowUp
            false -> Icons.Default.KeyboardArrowDown
            null -> Icons.Default.KeyboardArrowDown
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InkCharcoal,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Signature solid deep-black pill CTA button matching "More Filters" at the bottom of the reference UI.
 */
@Composable
fun SolidBlackPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = InkBlack,
    testTag: String = "solid_black_pill_button"
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion && enabled) 0.945f else 1.0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 540f),
        label = "cta_scale"
    )
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion && enabled) 2.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 540f),
        label = "cta_offset"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = PureWhitePill,
            disabledContainerColor = SwatchStoneDark,
            disabledContentColor = CreamPaper
        ),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = pressOffsetY.toPx()
            }
            .testTag(testTag)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Peeking secondary layered card inspired by the multi-colored earthy pill stack on the right
 * side of the reference image (dark slate, terracotta, ochre, sage olive, taupe).
 */
@Composable
fun PeekingTelemetryPanel(
    neighborsCount: Int,
    relayedCount: Int,
    queuedCount: Int,
    isMeshActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = StoneBackdropDeep.copy(alpha = 0.4f),
                spotColor = StoneBackdropDeep.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(PeekingPanelSurface)
            .border(0.8.dp, PaperHighlight, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag("peeking_telemetry_panel")
            .padding(vertical = 22.dp, horizontal = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Mesh Telemetry",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StoneMutedText,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(2.dp)
                    .background(InkBlack)
            )
            Spacer(modifier = Modifier.height(4.dp))

            val strings = LocalJesrStrings.current
            TelemetryColorPill(
                label = if (isMeshActive) strings.telemetryActive else strings.telemetryIdle,
                color = SlateDarkAccent,
                textColor = PureWhitePill
            )
            TelemetryColorPill(
                label = strings.telemetryNodes(neighborsCount),
                color = SwatchStoneMid,
                textColor = InkBlack
            )
            TelemetryColorPill(
                label = "SOS",
                color = TerracottaAccent,
                textColor = PureWhitePill
            )
            TelemetryColorPill(
                label = strings.telemetryRelayed(relayedCount),
                color = OchreAccent,
                textColor = PureWhitePill
            )
            TelemetryColorPill(
                label = "E2EE",
                color = SageOliveAccent,
                textColor = PureWhitePill
            )
            TelemetryColorPill(
                label = strings.telemetryQueued(queuedCount),
                color = SwatchStoneDark,
                textColor = InkBlack
            )
        }
    }
}

@Composable
private fun TelemetryColorPill(
    label: String,
    color: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Compact cinematic badge pill tuned for the editorial light-and-ink aesthetic.
 */
@Composable
fun CinematicPill(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TaupePill,
    borderColor: Color = PaperBorder,
    textColor: Color = InkBlack
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(0.6.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
    }
}

/**
 * Animated signal waveform bars that pulse softly with mesh activity.
 */
@Composable
fun CinematicSignalWaveform(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    activeColor: Color = SageOliveAccent,
    inactiveColor: Color = SwatchStoneDark
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val transition = rememberInfiniteTransition(label = "waveform")

    val h1 by transition.animateFloat(
        initialValue = 4f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val h2 by transition.animateFloat(
        initialValue = 14f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val h3 by transition.animateFloat(
        initialValue = 7f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = if (isActive && !isReducedMotion) listOf(h1, h2, h3) else listOf(7f, 12f, 7f)
        heights.forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightVal.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Animated glowing status beacon with expanding ripples indicating active local mesh.
 */
@Composable
fun CinematicLiveBeacon(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SageOliveAccent,
    inactiveColor: Color = SwatchStoneDark
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isReducedMotion) 1.0f else 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_wave"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = if (isReducedMotion) 0.7f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive && !isReducedMotion) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha))
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else inactiveColor)
        )
    }
}

/**
 * Tactical Radar Sweep Visualizer on a deep dark studio instrument surface.
 */
@Composable
fun CinematicRadarSweepBeam(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    nodes: List<Offset> = emptyList(),
    tintColor: Color = AmberEmblem
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val infiniteTransition = rememberInfiniteTransition(label = "radar_beam")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val echoRadiusRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "echo_shockwave"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f - 4.dp.toPx()

        listOf(0.33f, 0.66f, 1.0f).forEach { ratio ->
            drawCircle(
                color = tintColor.copy(alpha = 0.22f),
                radius = maxRadius * ratio,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawLine(
            color = tintColor.copy(alpha = 0.15f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = tintColor.copy(alpha = 0.15f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
        )

        if (isActive && !isReducedMotion) {
            val waveAlpha = (1f - echoRadiusRatio).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = tintColor.copy(alpha = waveAlpha),
                radius = maxRadius * echoRadiusRatio,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val sweepAngle = 45f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        tintColor.copy(alpha = 0.04f),
                        tintColor.copy(alpha = 0.35f)
                    ),
                    center = center
                ),
                startAngle = angle - sweepAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
            )

            val rad = Math.toRadians(angle.toDouble())
            val lineEnd = Offset(
                x = (center.x + maxRadius * kotlin.math.cos(rad)).toFloat(),
                y = (center.y + maxRadius * kotlin.math.sin(rad)).toFloat()
            )
            drawLine(
                color = tintColor,
                start = center,
                end = lineEnd,
                strokeWidth = 2.dp.toPx()
            )
        }

        nodes.forEach { relOffset ->
            val nodeX = center.x + (relOffset.x - 0.5f) * maxRadius * 1.6f
            val nodeY = center.y + (relOffset.y - 0.5f) * maxRadius * 1.6f
            val nodePos = Offset(nodeX, nodeY)

            val nodeAngle = Math.toDegrees(kotlin.math.atan2((nodeY - center.y).toDouble(), (nodeX - center.x).toDouble())).let {
                if (it < 0) it + 360 else it
            }
            val angleDiff = kotlin.math.abs(angle - nodeAngle.toFloat())
            val isSwept = angleDiff < 25f || angleDiff > 335f

            val pingAlpha = if (isSwept && isActive) 0.95f else 0.55f
            val pingRadius = if (isSwept && isActive) 6.dp.toPx() else 4.dp.toPx()

            drawCircle(
                color = SageOliveAccent.copy(alpha = pingAlpha * 0.45f),
                radius = pingRadius * 2.2f,
                center = nodePos
            )
            drawCircle(
                color = PureWhitePill.copy(alpha = pingAlpha),
                radius = pingRadius,
                center = nodePos
            )
        }

        drawCircle(
            color = AmberEmblem,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun MessageDeliveryBadge(
    status: DeliveryStatus,
    hopCount: Int = 0,
    isOnDarkBubble: Boolean = false,
    modifier: Modifier = Modifier
) {
    val neutralTint = if (isOnDarkBubble) Color(0xFFB5ADA3) else StoneMutedText
    val deliveredTint = if (isOnDarkBubble) AmberEmblem else SageOliveAccent
    val strings = LocalJesrStrings.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            DeliveryStatus.LOCAL_PENDING -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = strings.deliveryLocal,
                    tint = neutralTint,
                    modifier = Modifier.size(12.dp)
                )
                Text(strings.deliveryLocal, fontSize = 10.sp, color = neutralTint)
            }
            DeliveryStatus.SENDING -> {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = strings.deliverySending,
                    tint = AmberEmblem,
                    modifier = Modifier.size(12.dp)
                )
                Text(strings.deliveryHop(hopCount), fontSize = 10.sp, color = AmberEmblem)
            }
            DeliveryStatus.QUEUED_RELAY -> {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = strings.deliveryQueued,
                    tint = AmberEmblem,
                    modifier = Modifier.size(12.dp)
                )
                Text(strings.deliveryQueued, fontSize = 10.sp, color = AmberEmblem)
            }
            DeliveryStatus.DELIVERED -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = strings.deliveryDelivered,
                    tint = deliveredTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(strings.deliveryDelivered, fontSize = 10.sp, color = deliveredTint)
            }
            DeliveryStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = strings.deliveryFailed,
                    tint = TerracottaAccent,
                    modifier = Modifier.size(12.dp)
                )
                Text(strings.deliveryFailed, fontSize = 10.sp, color = TerracottaAccent)
            }
        }
    }
}

@Composable
fun IncomingFriendRequestPrompt(
    senderId: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    Dialog(onDismissRequest = onDecline) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = CreamPaper,
            contentPadding = 22.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AmberContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = InkBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.incomingMeshReqTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.incomingMeshReqBody(senderId),
                    fontSize = 13.sp,
                    color = StoneMutedText,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("accept_friend_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PureWhitePill, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.accept, color = PureWhitePill, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(containerColor = TaupePill),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("decline_friend_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TerracottaAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.reject, color = TerracottaAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Non-interactive foreground overlay projecting warm sun rays, solar bloom,
 * and subtle architectural window shadow bands across the entire interface.
 */
@Composable
fun EditorialSunRaysOverlay(
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    if (!palette.isLightEnabled) return
    val customLight = palette.lightColor
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val transition = rememberInfiniteTransition(label = "foreground_sun_rays")
    val drift by transition.animateFloat(
        initialValue = if (isReducedMotion) 0f else -0.018f,
        targetValue = if (isReducedMotion) 0f else 0.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ray_drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (palette.isDark) {
            val rayBright = customLight ?: Color(0xFFEBF3FF)
            val rayMid = customLight?.let { lerp(it, Color(0xFFB4CEF2), 0.35f) } ?: Color(0xFFB4CEF2)
            val moonOrigin = Offset(w * (0.14f + drift * 0.5f), h * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rayBright.copy(alpha = 0.20f),
                        rayMid.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = moonOrigin,
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = moonOrigin
            )
            drawRect(
                brush = Brush.linearGradient(
                    0.00f to Color(0x3805070C),
                    0.22f to Color(0x240C111B),
                    0.36f to Color(0x00FFFFFF),
                    0.50f to rayBright.copy(alpha = 0.16f),
                    0.66f to rayMid.copy(alpha = 0.06f),
                    0.78f to Color(0x200E1420),
                    1.00f to Color(0x3405070C),
                    start = Offset(0f, h * (0.96f + drift)),
                    end = Offset(w * 0.98f, h * (0.04f + drift))
                )
            )
        } else {
            val rayBright = customLight ?: Color(0xFFFFFDF7)
            val rayWarm = customLight?.let { lerp(it, Color(0xFFFFEAC4), 0.35f) } ?: Color(0xFFFFEAC4)
            val sunOrigin = Offset(w * (0.14f + drift * 0.5f), h * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        rayBright.copy(alpha = 0.18f),
                        rayWarm.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = sunOrigin,
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = sunOrigin
            )

            drawRect(
                brush = Brush.linearGradient(
                    0.00f to Color(0x341B1510),
                    0.22f to Color(0x22382D24),
                    0.36f to Color(0x00FFFFFF),
                    0.50f to rayBright.copy(alpha = 0.14f),
                    0.66f to rayWarm.copy(alpha = 0.06f),
                    0.78f to Color(0x1E3D3127),
                    1.00f to Color(0x301A140F),
                    start = Offset(0f, h * (0.96f + drift)),
                    end = Offset(w * 0.98f, h * (0.04f + drift))
                )
            )
        }
    }
}
