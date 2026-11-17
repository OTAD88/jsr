package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import com.example.R
import com.example.ui.theme.CinematicAvatarFrameStyle
import com.example.ui.theme.CinematicSceneType
import com.example.ui.theme.CinematicThemeSpec
import com.example.ui.theme.CinematicThemeVariant
import com.example.ui.viewmodel.Screen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class SceneCameraPose(
    val panX: Float,
    val panY: Float,
    val zoom: Float,
    val angleDeg: Float,
    val altitudeDepth: Float
)

fun sceneCameraPoseFor(screen: Screen): SceneCameraPose = when (screen) {
    // High orbit / initial panoramic hero pose
    Screen.HOME -> SceneCameraPose(panX = 0f, panY = 0f, zoom = 1.0f, angleDeg = 0f, altitudeDepth = 0f)
    // Entering Settings: camera descends smoothly from high space/sky straight down to Earth / floor!
    Screen.SETTINGS -> SceneCameraPose(panX = 0.04f, panY = -0.14f, zoom = 1.16f, angleDeg = -3.0f, altitudeDepth = 1.0f)
    // Entering Account Profile: camera angles & zooms into close portrait perspective
    Screen.PROFILE -> SceneCameraPose(panX = -0.15f, panY = 0.06f, zoom = 1.24f, angleDeg = 12.0f, altitudeDepth = 0.35f)
    // Conversation: warm intimate mid-horizon framing
    Screen.CONVERSATION -> SceneCameraPose(panX = 0.08f, panY = -0.05f, zoom = 1.08f, angleDeg = -2.5f, altitudeDepth = 0.55f)
    // Store & Appearance: sweeping elevated showcase perspective
    Screen.THEME_STORE, Screen.APPEARANCE -> SceneCameraPose(panX = -0.08f, panY = 0.07f, zoom = 0.95f, angleDeg = 5.5f, altitudeDepth = 0.22f)
    // Discovery & Requests: dynamic explorer radar tilt
    Screen.ADD_FRIENDS_HUB, Screen.NEARBY_PEOPLE, Screen.ADD_BY_ID, Screen.FRIEND_REQUESTS ->
        SceneCameraPose(panX = 0.12f, panY = -0.08f, zoom = 1.12f, angleDeg = -8.5f, altitudeDepth = 0.68f)
    // Privacy & Friend Controls: grounded mid-low lookout
    Screen.FRIEND_SETTINGS, Screen.GLOBAL_PRIVACY, Screen.REQUEST_PRIVACY ->
        SceneCameraPose(panX = -0.09f, panY = -0.10f, zoom = 1.14f, angleDeg = 4.5f, altitudeDepth = 0.85f)
}

@Composable
fun CinematicSceneCanvas(
    spec: CinematicThemeSpec,
    isDark: Boolean,
    currentScreen: Screen = Screen.HOME,
    lightEnabled: Boolean = true,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val variant = if (isDark) spec.darkVariant else spec.lightVariant
    val targetPose = sceneCameraPoseFor(currentScreen)
    val animDuration = if (isReducedMotion) 0 else 850
    val animSpec = tween<Float>(durationMillis = animDuration, easing = FastOutSlowInEasing)

    val panX by animateFloatAsState(targetValue = targetPose.panX, animationSpec = animSpec, label = "cam_pan_x")
    val panY by animateFloatAsState(targetValue = targetPose.panY, animationSpec = animSpec, label = "cam_pan_y")
    val zoom by animateFloatAsState(targetValue = targetPose.zoom, animationSpec = animSpec, label = "cam_zoom")
    val angleDeg by animateFloatAsState(targetValue = targetPose.angleDeg, animationSpec = animSpec, label = "cam_angle")
    val altitudeDepth by animateFloatAsState(targetValue = targetPose.altitudeDepth, animationSpec = animSpec, label = "cam_depth")
    val pose = SceneCameraPose(panX = panX, panY = panY, zoom = zoom, angleDeg = angleDeg, altitudeDepth = altitudeDepth)

    val transition = rememberInfiniteTransition(label = "cinematic_scene_${spec.id}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scene_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_pulse"
    )

    val seeds = remember(spec.sceneType) {
        val list = ArrayList<Triple<Float, Float, Float>>(36)
        var s = spec.sceneType.ordinal * 7919 + 104729
        for (i in 0 until 36) {
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val x = (s % 1000) / 1000f
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val y = (s % 1000) / 1000f
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val weight = 0.4f + ((s % 100) / 100f) * 0.9f
            list.add(Triple(x, y, weight))
        }
        list
    }

    val context = LocalContext.current
    val sunriseArtwork: ImageBitmap? = remember(spec.sceneType) {
        if (spec.sceneType == CinematicSceneType.SUNRISE_TREE_MOUNTAIN_WATER) {
            runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.theme_sunrise_tree_mountain_water)
                    ?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(variant.bgTop, variant.bgMid, pose.altitudeDepth * 0.35f),
                    variant.bgMid,
                    lerp(variant.bgBottom, variant.accent, pose.altitudeDepth * (if (isDark) 0.16f else 0.12f))
                )
            )
        )

        when (spec.sceneType) {
            CinematicSceneType.ASTRONAUT -> drawAstronautScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.OCEAN_ODYSSEY -> drawOceanOdysseyScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.SKY_SUMMIT -> drawSkySummitScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.NEON_METROPOLIS -> drawNeonMetropolisScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.STAR_DESERT -> drawStarDesertScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.ASTRAL_EXPRESS -> drawAstralExpressScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.SUNLIT_BEDROOM -> drawSunlitBedroomScene(w, h, isDark, variant, phase, pulse, seeds, pose, lightEnabled)
            CinematicSceneType.OVERLAND_CAMP_TRUCK -> drawOverlandCampTruckScene(w, h, isDark, variant, phase, pulse, seeds, pose, lightEnabled)
            CinematicSceneType.FROST_CAMPFIRE -> drawFrostCampfireScene(w, h, isDark, variant, phase, pulse, seeds, pose, lightEnabled)
            CinematicSceneType.FOREST_CABIN -> drawForestCabinScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.STUDY_LOFT -> drawStudyLoftScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.DEV_TERMINAL -> drawDevTerminalScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.GRAND_MUSEUM -> drawGrandMuseumScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.FOOTBALL_STADIUM -> drawFootballStadiumScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.SAKURA_GARDEN -> drawSakuraScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.AURORA_PRINCESS -> drawAuroraScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.LUMINOUS_BUTTERFLIES -> drawButterfliesScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.OCEAN_PEARL -> drawOceanPearlScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.ROYAL_ROSE -> drawRoyalRoseScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.MOONLIGHT_CRESCENT -> drawMoonlightScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.CRYSTAL_PRISM -> drawCrystalPrismScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.LAVENDER_BREEZE -> drawLavenderScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.SUNSET_PLUME -> drawSunsetPlumeScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.COTTON_CLOUD -> drawCottonCloudScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.CYBER_NEON -> drawCyberNeonScene(w, h, isDark, variant, phase, pulse, pose)
            CinematicSceneType.OBSIDIAN_MAGMA -> drawObsidianMagmaScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.ARCTIC_STORM -> drawArcticStormScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.SHADOW_SAMURAI -> drawShadowSamuraiScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.COSMIC_VORTEX -> drawCosmicVortexScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.DEEP_TRENCH -> drawDeepTrenchScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.DESERT_FALCON -> drawDesertFalconScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.TITANIUM_MECH -> drawTitaniumMechScene(w, h, isDark, variant, phase, pulse, pose)
            CinematicSceneType.EMERALD_PHANTOM -> drawEmeraldPhantomScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.THUNDER_APEX -> drawThunderApexScene(w, h, isDark, variant, phase, pulse, seeds, pose)
            CinematicSceneType.FRUTIGER_AERO -> drawFrutigerAeroScene(w, h, isDark, variant, phase, pulse, seeds, pose, lightEnabled)
            CinematicSceneType.SUNRISE_TREE_MOUNTAIN_WATER -> drawSunriseTreeMountainWaterScene(
                w, h, isDark, variant, phase, pulse, seeds, pose, lightEnabled, sunriseArtwork
            )
        }
    }
}

private fun DrawScope.drawAstronautScene(
    w: Float,
    h: Float,
    isDark: Boolean,
    v: CinematicThemeVariant,
    phase: Float,
    pulse: Float,
    seeds: List<Triple<Float, Float, Float>>,
    pose: SceneCameraPose
) {
    val minD = size.minDimension
    val bobY = sin(phase * 2f * PI.toFloat()) * minD * 0.032f
    val bobX = cos(phase * 2f * PI.toFloat()) * minD * 0.014f
    val tiltDeg = sin(phase * 2f * PI.toFloat()) * 4.5f + pose.angleDeg
    // As altitudeDepth increases (0f space -> 1f Settings Earth surface), sky recedes upward & Earth rises dramatically!
    val depth = pose.altitudeDepth
    val spaceDriftY = -h * depth * 0.24f + pose.panY * h * 0.25f

    if (isDark) {
        // Distant cosmos layer rising smoothly upward as camera descends toward Earth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x557B61FF), Color(0x2858D5FF), Color.Transparent),
                center = Offset(w * (0.25f + pose.panX * 0.4f), h * 0.25f + spaceDriftY),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * (0.25f + pose.panX * 0.4f), h * 0.25f + spaceDriftY)
        )
        drawStarfield(w, h, seeds, Color.White.copy(alpha = (1f - depth * 0.45f).coerceIn(0.35f, 1f)), phase)

        // Ringed planet at upper corner (drifts up when camera drops to Earth)
        val planetCenter = Offset(w * (0.80f + pose.panX * 0.5f), h * 0.19f + spaceDriftY)
        val planetR = minD * 0.11f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA8D8FF), Color(0xFF4267B2), Color(0xFF1A2552)),
                center = Offset(planetCenter.x - planetR * 0.3f, planetCenter.y - planetR * 0.3f),
                radius = planetR * 1.4f
            ),
            radius = planetR,
            center = planetCenter
        )
        rotate(-18f + pose.angleDeg * 0.4f, pivot = planetCenter) {
            drawOval(
                color = Color(0x8858D5FF),
                topLeft = Offset(planetCenter.x - planetR * 1.65f, planetCenter.y - planetR * 0.32f),
                size = Size(planetR * 3.3f, planetR * 0.64f),
                style = Stroke(width = (minD * 0.012f).coerceAtLeast(1.2f))
            )
        }
    } else {
        // Light mode: Celestial sky-blue heaven with Astronaut silhouetted in front of radiant Sun!
        val sunCenter = Offset(w * (0.50f + pose.panX * 0.35f), h * 0.33f + spaceDriftY * 0.6f)
        val sunRadius = minD * 0.23f * (0.94f + (pose.zoom - 1f) * 0.45f)

        rotate(phase * 360f, pivot = sunCenter) {
            for (i in 0 until 16) {
                val angle = (i * 22.5f) * (PI.toFloat() / 180f)
                val inner = sunRadius * 0.92f
                val outer = sunRadius * (1.85f + (i % 2) * 0.45f + pulse * 0.18f)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x99FFF7B8), Color.Transparent),
                        start = Offset(sunCenter.x + cos(angle) * inner, sunCenter.y + sin(angle) * inner),
                        end = Offset(sunCenter.x + cos(angle) * outer, sunCenter.y + sin(angle) * outer)
                    ),
                    start = Offset(sunCenter.x + cos(angle) * inner, sunCenter.y + sin(angle) * inner),
                    end = Offset(sunCenter.x + cos(angle) * outer, sunCenter.y + sin(angle) * outer),
                    strokeWidth = (minD * 0.025f).coerceAtLeast(2f),
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFCE0), Color(0xCCFFE073), Color(0x557FE3FF), Color.Transparent),
                center = sunCenter,
                radius = sunRadius * 2.5f
            ),
            radius = sunRadius * 2.5f,
            center = sunCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFEA8A), Color(0xFFFFBE3B)),
                center = sunCenter,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )

        drawSoftCloudCluster(Offset(w * ((0.18f + phase * 0.2f) % 1.2f - 0.1f), h * 0.22f + spaceDriftY * 0.4f), minD * 0.18f, Color.White.copy(alpha = 0.65f))
        drawSoftCloudCluster(Offset(w * ((0.82f - phase * 0.15f + 1.2f) % 1.2f - 0.1f), h * (0.72f - depth * 0.12f)), minD * 0.22f, Color.White.copy(alpha = 0.62f))
    }

    // Dynamic Descending Earth Horizon / Planetary Surface (rises dramatically into view in Settings!)
    val earthCenterY = h * (1.44f - depth * 0.50f)
    val earthRadius = w * (0.92f + depth * 0.42f)
    val earthCenter = Offset(w * (0.50f - pose.panX * 0.22f), earthCenterY)

    // Glowing atmospheric halo around Earth limb
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isDark) {
                listOf(Color(0xFF47C5FF).copy(alpha = 0.35f + depth * 0.42f), Color(0xFF19589E).copy(alpha = 0.22f + depth * 0.22f), Color.Transparent)
            } else {
                listOf(Color(0xFF8BE5FF).copy(alpha = 0.45f + depth * 0.35f), Color.White.copy(alpha = 0.45f), Color.Transparent)
            },
            center = earthCenter,
            radius = earthRadius * 1.18f
        ),
        radius = earthRadius * 1.18f,
        center = earthCenter
    )
    // Solid Earth sphere (Ocean & Green/Gold continents visible especially when descending to Settings!)
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isDark) {
                listOf(Color(0xFF1E75C4), Color(0xFF0E3E7A), Color(0xFF061A3B))
            } else {
                listOf(Color(0xFF56C2FF), Color(0xFF218AD9), Color(0xFF135C99))
            },
            center = Offset(earthCenter.x, earthCenter.y - earthRadius * 0.32f),
            radius = earthRadius * 1.1f
        ),
        radius = earthRadius,
        center = earthCenter
    )
    // Landmasses / Green Continents & Cloud Swirls on Earth surface
    val landColor = if (isDark) Color(0xFF2FA877).copy(alpha = 0.55f + depth * 0.35f) else Color(0xFF4CD48B).copy(alpha = 0.58f + depth * 0.32f)
    drawOval(
        color = landColor,
        topLeft = Offset(earthCenter.x - earthRadius * 0.42f, earthCenter.y - earthRadius * 0.90f),
        size = Size(earthRadius * 0.52f, earthRadius * 0.24f)
    )
    drawOval(
        color = landColor,
        topLeft = Offset(earthCenter.x + earthRadius * 0.08f, earthCenter.y - earthRadius * 0.84f),
        size = Size(earthRadius * 0.46f, earthRadius * 0.22f)
    )
    drawSoftCloudCluster(
        Offset(earthCenter.x - earthRadius * 0.16f + phase * earthRadius * 0.08f, earthCenter.y - earthRadius * 0.92f),
        earthRadius * 0.15f,
        Color.White.copy(alpha = 0.48f + depth * 0.28f)
    )

    // Hero Floating Astronaut Figure (scales & glides across screens based on camera pose)
    val astroCenter = Offset(
        w * (0.50f + pose.panX * 0.65f) + bobX,
        h * (0.42f - depth * 0.12f + pose.panY * 0.4f) + bobY
    )
    val unit = minD * 0.095f * (1f + (pose.zoom - 1f) * 0.55f)
    rotate(tiltDeg, pivot = astroCenter) {
        val suitMain = if (isDark) Color(0xFFF1F5FD) else Color(0xFF16355C)
        val suitShade = if (isDark) Color(0xFF9EB4D8) else Color(0xFF285B94)
        val visorGold = if (isDark) Color(0xFF38D4FF) else Color(0xFFFFC947)
        val rimGlow = if (isDark) Color(0x9958D5FF) else Color(0xDDFFE885)

        // Backpack & thruster glow
        drawRoundRect(
            color = suitShade,
            topLeft = Offset(astroCenter.x - unit * 0.95f, astroCenter.y - unit * 0.35f),
            size = Size(unit * 1.9f, unit * 1.25f),
            cornerRadius = CornerRadius(unit * 0.3f, unit * 0.3f)
        )
        // Thruster spark exhaust
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(rimGlow, Color.Transparent),
                center = Offset(astroCenter.x - unit * 0.55f, astroCenter.y + unit * 1.15f),
                radius = unit * (0.65f + pulse * 0.25f)
            ),
            radius = unit * (0.65f + pulse * 0.25f),
            center = Offset(astroCenter.x - unit * 0.55f, astroCenter.y + unit * 1.15f)
        )

        // Arms & Legs floating in Zero-G
        drawLine(
            color = suitMain,
            start = Offset(astroCenter.x - unit * 0.65f, astroCenter.y - unit * 0.1f),
            end = Offset(astroCenter.x - unit * 1.45f, astroCenter.y - unit * 0.65f),
            strokeWidth = unit * 0.42f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = suitMain,
            start = Offset(astroCenter.x + unit * 0.65f, astroCenter.y - unit * 0.1f),
            end = Offset(astroCenter.x + unit * 1.40f, astroCenter.y + unit * 0.28f),
            strokeWidth = unit * 0.42f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = suitMain,
            start = Offset(astroCenter.x - unit * 0.32f, astroCenter.y + unit * 0.85f),
            end = Offset(astroCenter.x - unit * 0.68f, astroCenter.y + unit * 1.78f),
            strokeWidth = unit * 0.45f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = suitMain,
            start = Offset(astroCenter.x + unit * 0.34f, astroCenter.y + unit * 0.85f),
            end = Offset(astroCenter.x + unit * 0.72f, astroCenter.y + unit * 1.62f),
            strokeWidth = unit * 0.45f,
            cap = StrokeCap.Round
        )

        // Torso & Chest Control Module
        drawRoundRect(
            color = suitMain,
            topLeft = Offset(astroCenter.x - unit * 0.62f, astroCenter.y - unit * 0.35f),
            size = Size(unit * 1.24f, unit * 1.35f),
            cornerRadius = CornerRadius(unit * 0.38f, unit * 0.38f)
        )
        drawRoundRect(
            color = v.accent,
            topLeft = Offset(astroCenter.x - unit * 0.30f, astroCenter.y + unit * 0.05f),
            size = Size(unit * 0.60f, unit * 0.42f),
            cornerRadius = CornerRadius(unit * 0.1f, unit * 0.1f)
        )

        // Helmet & Sun/Cosmos Reflective Visor
        val helmetCenter = Offset(astroCenter.x, astroCenter.y - unit * 0.92f)
        drawCircle(
            color = rimGlow,
            radius = unit * 0.76f,
            center = helmetCenter,
            style = Stroke(width = (unit * 0.12f).coerceAtLeast(1.5f))
        )
        drawCircle(
            color = suitMain,
            radius = unit * 0.68f,
            center = helmetCenter
        )
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(visorGold, lerp(visorGold, Color(0xFF0A1428), 0.72f))
            ),
            topLeft = Offset(helmetCenter.x - unit * 0.48f, helmetCenter.y - unit * 0.36f),
            size = Size(unit * 0.96f, unit * 0.68f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = unit * 0.11f,
            center = Offset(helmetCenter.x - unit * 0.22f, helmetCenter.y - unit * 0.14f)
        )
    }
}

// 1. Abyssal Ocean Odyssey: Camera dives from surface waves down to glowing seafloor & coral reef
private fun DrawScope.drawOceanOdysseyScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val minD = size.minDimension
    val depth = pose.altitudeDepth
    // Light rays piercing down from ocean surface
    withTransform({
        rotate(pose.angleDeg * 0.45f, pivot = Offset(w * 0.5f, h * 0.5f))
    }) {
        for (i in 0..3) {
            drawLine(
                brush = Brush.linearGradient(listOf(v.lightColor.copy(alpha = 0.40f - depth * 0.15f), Color.Transparent)),
                start = Offset(w * (0.14f + i * 0.24f + pose.panX * 0.3f), 0f),
                end = Offset(w * (0.34f + i * 0.20f + pulse * 0.06f), h * (0.85f - depth * 0.15f)),
                strokeWidth = w * 0.09f
            )
        }
    }
    // Seafloor & glowing corals rise smoothly as camera dives in Settings
    val floorY = h * (0.94f - depth * 0.28f)
    val floorPath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, floorY)
        cubicTo(w * 0.28f, floorY - h * 0.07f, w * 0.68f, floorY + h * 0.05f, w, floorY - h * 0.04f)
        lineTo(w, h); close()
    }
    drawPath(floorPath, color = lerp(v.bgBottom, v.accent, 0.32f).copy(alpha = 0.85f))

    // Floating deep-sea explorer / submarine silhouette
    val subCenter = Offset(w * (0.50f + pose.panX * 0.6f), h * (0.38f + depth * 0.14f) + sin(phase * 6.28f) * minD * 0.025f)
    val r = minD * 0.11f * (1f + (pose.zoom - 1f) * 0.5f)
    rotate(pose.angleDeg * 0.6f, pivot = subCenter) {
        // Spotlight beam ahead
        val beam = Path().apply {
            moveTo(subCenter.x + r * 0.6f, subCenter.y)
            lineTo(subCenter.x + r * 3.1f, subCenter.y - r * 1.1f)
            lineTo(subCenter.x + r * 3.1f, subCenter.y + r * 1.1f)
            close()
        }
        drawPath(beam, brush = Brush.horizontalGradient(listOf(v.lightColor.copy(alpha = 0.42f), Color.Transparent), startX = subCenter.x, endX = subCenter.x + r * 3.1f))
        drawOval(color = v.accent, topLeft = Offset(subCenter.x - r, subCenter.y - r * 0.58f), size = Size(r * 2f, r * 1.16f))
        drawCircle(color = v.lightColor, radius = r * 0.32f, center = Offset(subCenter.x + r * 0.32f, subCenter.y))
    }
    // Rising luminous bubbles
    seeds.take(20).forEach { (sx, sy, weight) ->
        val cx = ((sx + pose.panX * 0.4f + 1f) % 1f) * w
        val cy = ((sy - phase * (0.35f + weight * 0.25f) + 1.5f) % 1f) * h
        drawCircle(color = v.lightColor.copy(alpha = 0.65f), radius = (2.5f + weight * 3f).dp.toPx(), center = Offset(cx, cy))
    }
}

// 2. Alpine Sky Glider: Camera glides from sky & clouds down to snowy mountain valley
private fun DrawScope.drawSkySummitScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    if (isDark) {
        drawStarfield(w, h, seeds.take(20), Color.White.copy(alpha = 1f - depth * 0.35f), phase)
    } else {
        drawCircle(
            brush = Brush.radialGradient(listOf(v.lightColor, v.accent.copy(alpha = 0.25f), Color.Transparent), center = Offset(w * 0.72f, h * (0.20f - depth * 0.08f)), radius = minD * 0.42f),
            radius = minD * 0.42f,
            center = Offset(w * 0.72f, h * (0.20f - depth * 0.08f))
        )
    }
    withTransform({
        rotate(pose.angleDeg * 0.55f, pivot = Offset(w * 0.5f, h * 0.5f))
    }) {
        // Far snow-capped mountain range rising cleanly on camera descent
        val ridgeY = h * (0.76f - depth * 0.24f)
        val farPeaks = Path().apply {
            moveTo(0f, h); lineTo(0f, ridgeY); lineTo(w * 0.24f, ridgeY - h * 0.20f)
            lineTo(w * 0.48f, ridgeY - h * 0.06f); lineTo(w * 0.74f, ridgeY - h * 0.26f)
            lineTo(w, ridgeY - h * 0.08f); lineTo(w, h); close()
        }
        drawPath(farPeaks, color = lerp(v.bgBottom, v.accent, 0.35f).copy(alpha = 0.75f))

        val valleyY = h * (0.88f - depth * 0.22f)
        val nearValley = Path().apply {
            moveTo(0f, h); lineTo(0f, valleyY); lineTo(w * 0.35f, valleyY - h * 0.12f)
            lineTo(w * 0.65f, valleyY + h * 0.02f); lineTo(w, valleyY - h * 0.14f); lineTo(w, h); close()
        }
        drawPath(nearValley, color = lerp(v.bgTop, v.frameSecondary, 0.25f).copy(alpha = 0.90f))

        // Drifting cloud layers between peaks
        drawSoftCloudCluster(Offset(w * ((0.22f + phase * 0.2f + pose.panX) % 1.2f - 0.1f), ridgeY - h * 0.12f), minD * 0.2f, Color.White.copy(alpha = if (isDark) 0.24f else 0.72f))
        drawSoftCloudCluster(Offset(w * ((0.76f - phase * 0.16f + 1.2f) % 1.2f - 0.1f), ridgeY - h * 0.05f), minD * 0.18f, Color.White.copy(alpha = if (isDark) 0.20f else 0.65f))
    }
    // Soaring sky eagle / glider silhouette
    val bird = Offset(w * (0.48f + pose.panX * 0.6f), h * (0.34f + depth * 0.10f) + sin(phase * 6.28f) * minD * 0.02f)
    val wing = minD * 0.12f * (1f + (pose.zoom - 1f) * 0.4f)
    drawLine(v.accent, start = Offset(bird.x - wing, bird.y - wing * 0.25f), end = bird, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawLine(v.accent, start = bird, end = Offset(bird.x + wing, bird.y - wing * 0.25f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
}

// 3. Neon Skybridge Metropolis: Camera descends from orbital skyline down between glowing neon towers
private fun DrawScope.drawNeonMetropolisScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val horizonY = h * (0.72f - depth * 0.24f)
    withTransform({
        rotate(pose.angleDeg * 0.5f, pivot = Offset(w * 0.5f, h * 0.5f))
    }) {
        // Futuristic glowing tower silhouettes rising into view
        val towers = listOf(0.06f to 0.34f, 0.20f to 0.48f, 0.36f to 0.30f, 0.54f to 0.52f, 0.72f to 0.38f, 0.86f to 0.44f)
        towers.forEachIndexed { idx, (tx, heightRatio) ->
            val bw = w * 0.11f
            val bh = h * heightRatio * (0.75f + depth * 0.45f)
            val left = w * tx + pose.panX * w * (0.3f + (idx % 2) * 0.2f)
            val top = horizonY - bh + depth * h * 0.08f
            drawRoundRect(
                color = lerp(v.bgTop, v.frameSecondary, 0.22f).copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(bw, bh + h * 0.3f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Illuminated neon skyscraper edge line
            drawLine(
                color = (if (idx % 2 == 0) v.accent else v.lightColor).copy(alpha = 0.65f + pulse * 0.25f),
                start = Offset(left + 2.dp.toPx(), top),
                end = Offset(left + 2.dp.toPx(), horizonY + h * 0.2f),
                strokeWidth = 1.8.dp.toPx()
            )
        }
        // Moving neon sky-traffic pulses
        for (lane in 0..2) {
            val y = h * (0.28f + lane * 0.16f - depth * 0.08f)
            val startX = ((phase + lane * 0.33f) % 1.2f - 0.1f) * w
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, v.accent, Color.Transparent), startX = startX, endX = startX + w * 0.24f),
                start = Offset(startX, y),
                end = Offset(startX + w * 0.24f, y),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    if (isDark) drawStarfield(w, h, seeds.take(14), Color.White.copy(alpha = 0.65f), phase)
}

// 4. Celestial Dune Observatory: Camera drops from starry Milky Way onto golden dunes & glowing observatory
private fun DrawScope.drawStarDesertScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    // Milky way diagonal glow band
    drawLine(
        brush = Brush.linearGradient(listOf(Color.Transparent, v.frameSecondary.copy(alpha = 0.32f), v.lightColor.copy(alpha = 0.24f), Color.Transparent)),
        start = Offset(0f, h * (0.55f - depth * 0.2f)),
        end = Offset(w, h * (0.10f - depth * 0.15f)),
        strokeWidth = w * 0.36f
    )
    drawStarfield(w, h, seeds, v.lightColor.copy(alpha = 1f - depth * 0.3f), phase)

    val crescent = Offset(w * (0.76f + pose.panX * 0.4f), h * (0.20f - depth * 0.06f))
    drawCircle(color = v.lightColor, radius = minD * 0.09f, center = crescent)
    if (isDark) drawCircle(color = v.bgTop, radius = minD * 0.075f, center = Offset(crescent.x + minD * 0.028f, crescent.y - minD * 0.015f))

    withTransform({
        rotate(pose.angleDeg * 0.4f, pivot = Offset(w * 0.5f, h * 0.65f))
    }) {
        val duneY = h * (0.82f - depth * 0.26f)
        val dune = Path().apply {
            moveTo(0f, h); lineTo(0f, duneY)
            quadraticBezierTo(w * 0.38f, duneY - h * 0.16f, w * 0.72f, duneY - h * 0.02f)
            quadraticBezierTo(w * 0.88f, duneY + h * 0.04f, w, duneY - h * 0.09f)
            lineTo(w, h); close()
        }
        drawPath(dune, color = lerp(v.bgBottom, v.accent, 0.38f))
        // Glowing observatory dome perched on the dune ridge
        val dome = Offset(w * (0.64f + pose.panX * 0.3f), duneY - h * 0.03f)
        drawCircle(
            brush = Brush.radialGradient(listOf(v.accent.copy(alpha = 0.45f + pulse * 0.2f), Color.Transparent), center = dome, radius = minD * 0.20f),
            radius = minD * 0.20f,
            center = dome
        )
        drawCircle(color = v.inkPrimary.copy(alpha = 0.72f), radius = minD * 0.06f, center = dome)
    }
}

// 5. Astral Star-Rail Express: Celestial glowing viaduct sweeping across stars with rotating horizon tilt
private fun DrawScope.drawAstralExpressScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    drawStarfield(w, h, seeds, v.lightColor, phase)
    withTransform({
        rotate(pose.angleDeg * 0.85f, pivot = Offset(w * 0.5f, h * 0.52f))
        scale(scaleX = 1f + (pose.zoom - 1f) * 0.45f, scaleY = 1f + (pose.zoom - 1f) * 0.45f, pivot = Offset(w * 0.5f, h * 0.52f))
    }) {
        val arcY = h * (0.64f - depth * 0.16f)
        val rail = Path().apply {
            moveTo(-w * 0.15f, arcY + h * 0.14f)
            cubicTo(w * 0.28f, arcY - h * 0.16f, w * 0.70f, arcY - h * 0.06f, w * 1.15f, arcY - h * 0.22f)
        }
        drawPath(
            path = rail,
            brush = Brush.horizontalGradient(listOf(v.frameSecondary.copy(alpha = 0.3f), v.lightColor, v.accent)),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        // Moving luminous star-train comet along viaduct curve
        val t = (phase + depth * 0.15f) % 1f
        val trainX = w * (-0.05f + t * 1.1f)
        val trainY = arcY + sin(t * PI.toFloat()) * (-h * 0.14f) - t * h * 0.18f + h * 0.05f
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, v.lightColor, v.accent.copy(alpha = 0.35f), Color.Transparent), center = Offset(trainX, trainY), radius = minD * 0.16f),
            radius = minD * 0.16f,
            center = Offset(trainX, trainY)
        )
        drawRoundRect(
            color = v.lightColor,
            topLeft = Offset(trainX - minD * 0.06f, trainY - minD * 0.018f),
            size = Size(minD * 0.12f, minD * 0.036f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
    }
}

// 6. Cozy Sunlit & Moonlit Bedroom: Light = golden sunbeams pouring through window onto bed & plant;
// Dark = silver moonlight + starry window sky + bedside fairy lamp; Settings = camera pans onto window study desk nook
private fun DrawScope.drawSunlitBedroomScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose, lightEnabled: Boolean
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panShiftX = pose.panX * w * 0.45f - depth * w * 0.14f

    // Bedroom wall & floor divider
    val floorY = h * (0.76f - depth * 0.08f)
    drawRect(
        color = lerp(v.bgBottom, Color(0xFF1F1610), if (isDark) 0.55f else 0.15f),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )

    // Arched Bedroom Window looking out to daylight sky or crescent night stars
    val winLeft = w * 0.18f + panShiftX
    val winTop = h * (0.14f - depth * 0.03f)
    val winW = w * (0.50f + depth * 0.14f)
    val winH = h * (0.38f + depth * 0.08f)

    drawRoundRect(
        color = if (isDark) Color(0xFF060B1A) else Color(0xFF8FD6FF),
        topLeft = Offset(winLeft, winTop),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
    )
    if (isDark) {
        seeds.take(14).forEachIndexed { i, (sx, sy, weight) ->
            val starX = winLeft + sx * winW
            val starY = winTop + sy * winH * 0.78f
            val twinkle = 0.4f + 0.6f * kotlin.math.abs(sin(phase * 2f * PI.toFloat() + i))
            drawCircle(Color.White.copy(alpha = twinkle), radius = (1.1f + weight).dp.toPx(), center = Offset(starX, starY))
        }
        val moonC = Offset(winLeft + winW * 0.72f, winTop + winH * 0.28f)
        drawCircle(v.lightColor, radius = minD * 0.06f, center = moonC)
        drawCircle(Color(0xFF060B1A), radius = minD * 0.048f, center = Offset(moonC.x + minD * 0.02f, moonC.y - minD * 0.01f))
    } else {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFFCE0), Color(0xFFFFDF7A), Color.Transparent), center = Offset(winLeft + winW * 0.30f, winTop + winH * 0.25f), radius = winW * 0.6f),
            radius = winW * 0.6f,
            center = Offset(winLeft + winW * 0.30f, winTop + winH * 0.25f)
        )
    }

    // Window frame & mullions
    drawRoundRect(
        color = if (isDark) Color(0xFF283459) else Color(0xFFFFF8EC),
        topLeft = Offset(winLeft, winTop),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Stroke(width = 4.dp.toPx())
    )
    drawLine(
        color = if (isDark) Color(0xFF283459) else Color(0xFFFFF8EC),
        start = Offset(winLeft + winW * 0.5f, winTop),
        end = Offset(winLeft + winW * 0.5f, winTop + winH),
        strokeWidth = 3.dp.toPx()
    )
    drawLine(
        color = if (isDark) Color(0xFF283459) else Color(0xFFFFF8EC),
        start = Offset(winLeft, winTop + winH * 0.52f),
        end = Offset(winLeft + winW, winTop + winH * 0.52f),
        strokeWidth = 3.dp.toPx()
    )

    // Diagonal Volumetric Sunlight / Moonlight Shaft entering room onto bed
    if (lightEnabled) {
        val beamPath = Path().apply {
            moveTo(winLeft, winTop)
            lineTo(winLeft + winW, winTop + winH * 0.35f)
            lineTo(winLeft + winW * 1.45f, h)
            lineTo(winLeft - winW * 0.10f, h)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0x52B8D4FF), Color(0x289EBFFF), Color.Transparent)
                } else {
                    listOf(Color(0x70FFF6B3), Color(0x38FFE082), Color.Transparent)
                },
                start = Offset(winLeft, winTop),
                end = Offset(winLeft + winW, h)
            )
        )
    }

    // Cozy Bed foreground (fades back smoothly in Settings as camera pans right onto window study desk)
    val bedAlpha = (1f - depth * 0.65f).coerceIn(0.28f, 1f)
    val bedX = w * 0.14f + panShiftX * 0.8f
    drawRoundRect(
        color = v.frameSecondary.copy(alpha = 0.72f * bedAlpha),
        topLeft = Offset(bedX, floorY - h * 0.11f),
        size = Size(w * 0.44f, h * 0.12f),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f * bedAlpha),
        topLeft = Offset(bedX + w * 0.03f, floorY - h * 0.14f),
        size = Size(w * 0.12f, h * 0.05f),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )

    // Window desk & warm fairy lamp (grows prominently in Settings!)
    val deskX = w * (0.64f - depth * 0.18f) + panShiftX
    val lampPos = Offset(deskX + w * 0.12f, floorY - h * 0.14f)
    drawRect(
        color = if (isDark) Color(0xFF2B2233) else Color(0xFFA66F42),
        topLeft = Offset(deskX, floorY - h * 0.09f),
        size = Size(w * 0.28f, h * 0.025f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFD878).copy(alpha = if (isDark) 0.72f else 0.45f), Color.Transparent),
            center = lampPos,
            radius = minD * (0.14f + depth * 0.08f)
        ),
        radius = minD * (0.14f + depth * 0.08f),
        center = lampPos
    )
    drawCircle(color = Color(0xFFFFE59E), radius = minD * 0.026f, center = lampPos)
}

// 7. Overland Camp Truck: Light = camper relaxing beside truck; Dark = glowing pitched camp tent + sleeper;
// Settings = camera moves straight INSIDE the pickup truck cabin!
private fun DrawScope.drawOverlandCampTruckScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose, lightEnabled: Boolean
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panShiftX = pose.panX * w * 0.4f

    // Horizon ridge & sky
    if (isDark) drawStarfield(w, h, seeds.take(26), v.lightColor, phase)
    else {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFF4BD), Color(0x55FFD673), Color.Transparent), center = Offset(w * 0.78f, h * 0.22f), radius = minD * 0.48f),
            radius = minD * 0.48f,
            center = Offset(w * 0.78f, h * 0.22f)
        )
    }

    val hillY = h * (0.58f - depth * 0.06f)
    val mountain = Path().apply {
        moveTo(0f, h); lineTo(0f, hillY)
        lineTo(w * 0.24f, hillY - h * 0.12f)
        lineTo(w * 0.56f, hillY + h * 0.02f)
        lineTo(w * 0.84f, hillY - h * 0.10f)
        lineTo(w, hillY + h * 0.03f)
        lineTo(w, h); close()
    }
    drawPath(mountain, color = lerp(v.bgBottom, v.frameSecondary, if (isDark) 0.22f else 0.35f))

    val groundY = h * 0.74f
    drawRect(color = lerp(v.bgBottom, Color(0xFF1F1912), if (isDark) 0.65f else 0.18f), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))

    if (depth < 0.72f) {
        // Exterior Campsite View: Overland Truck + Day Chair / Night Tent & Sleeper
        val fade = ((0.72f - depth) / 0.72f).coerceIn(0f, 1f)
        val scaleF = 1f + depth * 0.38f
        withTransform({
            scale(scaleF, scaleF, pivot = Offset(w * 0.32f, groundY))
            translate(left = panShiftX)
        }) {
            // Overland Pickup Truck Silhouette
            val truckX = w * 0.08f
            val truckW = w * 0.38f
            val truckH = h * 0.10f
            val truckColor = (if (isDark) Color(0xFF283848) else Color(0xFF2B6F4C)).copy(alpha = fade)
            // Body + truck bed
            drawRoundRect(
                color = truckColor,
                topLeft = Offset(truckX, groundY - truckH * 1.15f),
                size = Size(truckW, truckH * 0.62f),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            // Truck cab
            drawRoundRect(
                color = truckColor,
                topLeft = Offset(truckX + truckW * 0.44f, groundY - truckH * 1.85f),
                size = Size(truckW * 0.42f, truckH * 0.85f),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            // Cab windshield
            drawRoundRect(
                color = (if (isDark) Color(0xFF4F789E) else Color(0xFFDFF4FF)).copy(alpha = 0.85f * fade),
                topLeft = Offset(truckX + truckW * 0.56f, groundY - truckH * 1.72f),
                size = Size(truckW * 0.24f, truckH * 0.42f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Headlight beam if dark
            if (isDark && lightEnabled) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFE59E).copy(alpha = 0.55f * fade), Color.Transparent), center = Offset(truckX + truckW, groundY - truckH * 0.85f), radius = minD * 0.18f),
                    radius = minD * 0.18f,
                    center = Offset(truckX + truckW, groundY - truckH * 0.85f)
                )
            }
            // Tires
            val wheelR = truckH * 0.34f
            drawCircle(Color(0xFF14171A).copy(alpha = fade), radius = wheelR, center = Offset(truckX + truckW * 0.22f, groundY - wheelR * 0.5f))
            drawCircle(Color(0xFF14171A).copy(alpha = fade), radius = wheelR, center = Offset(truckX + truckW * 0.78f, groundY - wheelR * 0.5f))
            drawCircle(Color(0xFFA0ABB5).copy(alpha = fade), radius = wheelR * 0.42f, center = Offset(truckX + truckW * 0.22f, groundY - wheelR * 0.5f))
            drawCircle(Color(0xFFA0ABB5).copy(alpha = fade), radius = wheelR * 0.42f, center = Offset(truckX + truckW * 0.78f, groundY - wheelR * 0.5f))

            if (isDark) {
                // Dark mode: Pitched triangular glowing camp tent with sleeper silhouette inside
                val tentL = w * 0.56f
                val tentR = w * 0.88f
                val tentMidX = (tentL + tentR) * 0.5f
                val tentTopY = groundY - h * 0.16f
                val tentPath = Path().apply {
                    moveTo(tentL, groundY); lineTo(tentMidX, tentTopY); lineTo(tentR, groundY); close()
                }
                drawPath(tentPath, color = Color(0xFFFF9B3D).copy(alpha = 0.86f * fade))
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFDE8F).copy(alpha = 0.48f * fade), Color.Transparent), center = Offset(tentMidX, groundY - h * 0.06f), radius = minD * 0.22f),
                    radius = minD * 0.22f,
                    center = Offset(tentMidX, groundY - h * 0.06f)
                )
                // Sleeping silhouette resting inside tent
                drawOval(
                    color = Color(0xFF2B180B).copy(alpha = 0.82f * fade),
                    topLeft = Offset(tentMidX - w * 0.08f, groundY - h * 0.032f),
                    size = Size(w * 0.16f, h * 0.028f)
                )
                drawCircle(Color(0xFF2B180B).copy(alpha = 0.85f * fade), radius = minD * 0.020f, center = Offset(tentMidX - w * 0.06f, groundY - h * 0.035f))
            } else {
                // Light mode: Camper sitting relaxed in folding camp chair beside the truck!
                val chairX = w * 0.64f
                val chairY = groundY - h * 0.055f
                drawLine(Color(0xFF3E2A1A).copy(alpha = fade), Offset(chairX, chairY), Offset(chairX - w * 0.03f, groundY), strokeWidth = 3.dp.toPx())
                drawLine(Color(0xFF3E2A1A).copy(alpha = fade), Offset(chairX + w * 0.05f, chairY), Offset(chairX + w * 0.07f, groundY), strokeWidth = 3.dp.toPx())
                drawLine(Color(0xFFD17A2E).copy(alpha = fade), Offset(chairX, chairY - h * 0.06f), Offset(chairX + w * 0.05f, chairY), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                // Sitting person silhouette
                drawCircle(Color(0xFF1F2B21).copy(alpha = fade), radius = minD * 0.024f, center = Offset(chairX + w * 0.03f, chairY - h * 0.065f))
                drawLine(Color(0xFF1F2B21).copy(alpha = fade), Offset(chairX + w * 0.02f, chairY - h * 0.04f), Offset(chairX + w * 0.045f, chairY), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }

    if (depth >= 0.35f) {
        // Interior Truck Cab View as Camera Enters Settings (depth -> 1.0)!
        val cabAlpha = ((depth - 0.35f) / 0.65f).coerceIn(0f, 1f)
        // Roof & windshield side A-pillars
        drawRect(
            color = Color(0xFF0E141C).copy(alpha = 0.88f * cabAlpha),
            topLeft = Offset(0f, h * 0.64f),
            size = Size(w, h * 0.36f)
        )
        drawLine(Color(0xFF0E141C).copy(alpha = 0.90f * cabAlpha), Offset(0f, h * 0.66f), Offset(w * 0.08f, 0f), strokeWidth = 18.dp.toPx())
        drawLine(Color(0xFF0E141C).copy(alpha = 0.90f * cabAlpha), Offset(w, h * 0.66f), Offset(w * 0.92f, 0f), strokeWidth = 18.dp.toPx())
        // Glowing Speedometer & Dash Gauge Dials
        val gaugeCenter = Offset(w * 0.32f, h * 0.72f)
        drawCircle(color = Color(0xFF1A2838).copy(alpha = cabAlpha), radius = minD * 0.085f, center = gaugeCenter)
        drawCircle(color = v.accent.copy(alpha = 0.85f * cabAlpha), radius = minD * 0.072f, center = gaugeCenter, style = Stroke(width = 2.dp.toPx()))
        // Steering wheel rim
        val wheelC = Offset(w * 0.32f, h * 0.80f)
        drawCircle(color = Color(0xFF1F242B).copy(alpha = cabAlpha), radius = minD * 0.16f, center = wheelC, style = Stroke(width = 10.dp.toPx()))
        drawLine(Color(0xFF1F242B).copy(alpha = cabAlpha), Offset(wheelC.x - minD * 0.15f, wheelC.y), Offset(wheelC.x + minD * 0.15f, wheelC.y), strokeWidth = 7.dp.toPx())
        // Radio console screen in center of cab
        drawRoundRect(
            color = v.frameSecondary.copy(alpha = 0.55f * cabAlpha),
            topLeft = Offset(w * 0.54f, h * 0.70f),
            size = Size(w * 0.24f, h * 0.10f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
    }
}

// 8. Frost Peak & Campfire: Light = bright alpine snows; Dark = traveler warming hands beside crackling campfire;
// Settings = traveler sleeping peacefully on camp bed beside embers; lightEnabled = Snow & Frost Crystals!
private fun DrawScope.drawFrostCampfireScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose, lightEnabled: Boolean
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panShift = pose.panX * w * 0.35f

    if (isDark) drawStarfield(w, h, seeds.take(24), Color(0xFFD6F2FF), phase)

    // Jagged snowy mountain peaks in background
    val mtnY = h * (0.54f - depth * 0.04f)
    val peaks = Path().apply {
        moveTo(0f, h); lineTo(0f, mtnY + h * 0.06f)
        lineTo(w * 0.22f, mtnY - h * 0.14f)
        lineTo(w * 0.44f, mtnY + h * 0.04f)
        lineTo(w * 0.72f, mtnY - h * 0.18f)
        lineTo(w, mtnY); lineTo(w, h); close()
    }
    drawPath(peaks, color = if (isDark) Color(0xFF15314F) else Color(0xFFAEDBF2))

    // Snowy foreground ridge
    val ridgeY = h * 0.72f
    val ridge = Path().apply {
        moveTo(0f, h); lineTo(0f, ridgeY)
        quadraticTo(w * 0.48f, ridgeY - h * 0.07f, w, ridgeY + h * 0.02f)
        lineTo(w, h); close()
    }
    drawPath(ridge, color = if (isDark) Color(0xFF1C354D) else Color(0xFFF2FAFF))

    // Campfire Hearth (Blazing orange-gold fire warm glow at Dark + glowing sleeping embers at Settings)
    val firePos = Offset(w * 0.56f + panShift * 0.5f, ridgeY - h * 0.01f)
    if (isDark || depth > 0.4f) {
        val fireStrength = if (isDark) (0.9f + pulse * 0.22f) else 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFD86E).copy(alpha = 0.65f * fireStrength), Color(0xFFFF6D1F).copy(alpha = 0.32f * fireStrength), Color.Transparent),
                center = firePos,
                radius = minD * (0.28f + pulse * 0.04f)
            ),
            radius = minD * (0.28f + pulse * 0.04f),
            center = firePos
        )
        // Firewood logs
        drawLine(Color(0xFF4A2511), Offset(firePos.x - minD * 0.05f, firePos.y + 4.dp.toPx()), Offset(firePos.x + minD * 0.05f, firePos.y - 2.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF4A2511), Offset(firePos.x + minD * 0.05f, firePos.y + 4.dp.toPx()), Offset(firePos.x - minD * 0.05f, firePos.y - 2.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        // Flickering Flame Tongue
        val flameH = minD * (if (depth > 0.6f) 0.07f else 0.11f) * (0.9f + pulse * 0.25f)
        val flame = Path().apply {
            moveTo(firePos.x - minD * 0.036f, firePos.y)
            quadraticTo(firePos.x - minD * 0.02f, firePos.y - flameH * 0.7f, firePos.x + sin(phase * 14f) * 3.dp.toPx(), firePos.y - flameH)
            quadraticTo(firePos.x + minD * 0.025f, firePos.y - flameH * 0.6f, firePos.x + minD * 0.036f, firePos.y)
            close()
        }
        drawPath(flame, brush = Brush.verticalGradient(listOf(Color(0xFFFFF6B3), Color(0xFFFF8824))))
    }

    // Person Silhouette State:
    // - Settings (depth >= 0.55f): Sleeping peacefully on camp bed/sleeping bag right next to the campfire!
    // - Dark (depth < 0.55f): Sitting bundled on log warming outstretched hands near the fire!
    // - Light (depth < 0.55f): Standing bundled on snowy alpine trail looking out!
    val personX = w * 0.34f + panShift * 0.5f
    val silhouette = if (isDark) Color(0xFF0A1421) else Color(0xFF132E45)
    if (depth >= 0.55f) {
        // Warm camp bedroll & sleeping traveler silhouette beside the fire
        drawRoundRect(
            color = Color(0xFF28557A),
            topLeft = Offset(personX - w * 0.10f, ridgeY - h * 0.028f),
            size = Size(w * 0.22f, h * 0.038f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawCircle(color = silhouette, radius = minD * 0.024f, center = Offset(personX + w * 0.09f, ridgeY - h * 0.026f))
    } else if (isDark) {
        // Sitting hiker warming hands beside campfire
        drawCircle(color = silhouette, radius = minD * 0.026f, center = Offset(personX, ridgeY - h * 0.095f))
        drawLine(silhouette, Offset(personX, ridgeY - h * 0.075f), Offset(personX - w * 0.015f, ridgeY - h * 0.02f), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(silhouette, Offset(personX, ridgeY - h * 0.06f), Offset(personX + w * 0.07f, ridgeY - h * 0.055f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    } else {
        // Standing bundled hiker in snow
        drawCircle(color = silhouette, radius = minD * 0.025f, center = Offset(personX, ridgeY - h * 0.12f))
        drawLine(silhouette, Offset(personX, ridgeY - h * 0.098f), Offset(personX, ridgeY - h * 0.03f), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(silhouette, Offset(personX - w * 0.01f, ridgeY - h * 0.03f), Offset(personX - w * 0.02f, ridgeY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(silhouette, Offset(personX + w * 0.01f, ridgeY - h * 0.03f), Offset(personX + w * 0.02f, ridgeY), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
    }

    // Frost & Falling Snow Layer (controlled by lightEnabled toggle!)
    if (lightEnabled) {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x36FFFFFF), Color.Transparent), center = Offset(0f, 0f), radius = w * 0.55f),
            radius = w * 0.55f,
            center = Offset(0f, 0f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x36A8E4FF), Color.Transparent), center = Offset(w, 0f), radius = w * 0.55f),
            radius = w * 0.55f,
            center = Offset(w, 0f)
        )
        seeds.forEachIndexed { idx, (sx, sy, weight) ->
            val fx = ((sx + phase * (0.18f + weight * 0.16f)) % 1f) * w
            val fy = ((sy + phase * (0.55f + weight * 0.35f)) % 1f) * h
            drawCircle(color = Color.White.copy(alpha = 0.5f + (idx % 3) * 0.15f), radius = (1.6f * weight).dp.toPx(), center = Offset(fx, fy))
        }
    }
}

// 9. Mystic Pine Forest Cabin: Towering pines & moonlight/sunlight glade stepping toward glowing cabin window
private fun DrawScope.drawForestCabinScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panX = pose.panX * w * 0.35f

    if (isDark) drawStarfield(w, h, seeds.take(22), v.lightColor, phase)
    else {
        drawLine(
            brush = Brush.linearGradient(listOf(v.lightColor.copy(alpha = 0.45f), Color.Transparent)),
            start = Offset(w * 0.2f, 0f),
            end = Offset(w * 0.65f, h * 0.85f),
            strokeWidth = w * 0.25f
        )
    }

    // Layered pine silhouettes
    val treeColor = if (isDark) Color(0xFF06150E) else Color(0xFF185E37)
    for (i in 0..5) {
        val tx = w * (0.08f + i * 0.18f) + panX * (0.3f + (i % 2) * 0.4f)
        val th = h * (0.34f + (i % 3) * 0.08f + depth * 0.08f)
        val treePath = Path().apply {
            moveTo(tx, h * 0.85f - th)
            lineTo(tx - w * 0.09f, h * 0.85f)
            lineTo(tx + w * 0.09f, h * 0.85f)
            close()
        }
        drawPath(treePath, color = treeColor.copy(alpha = 0.68f))
    }

    // Cozy Wooden Forest Cabin in clearing (grows closer in Settings!)
    val cabinScale = 0.8f + depth * 0.6f
    withTransform({ scale(cabinScale, cabinScale, pivot = Offset(w * 0.56f, h * 0.72f)) }) {
        val cx = w * 0.42f + panX * 0.5f
        val cy = h * 0.60f
        drawRect(color = if (isDark) Color(0xFF211610) else Color(0xFF6B4226), topLeft = Offset(cx, cy), size = Size(w * 0.28f, h * 0.14f))
        val roof = Path().apply { moveTo(cx - w * 0.03f, cy); lineTo(cx + w * 0.14f, cy - h * 0.09f); lineTo(cx + w * 0.31f, cy); close() }
        drawPath(roof, color = if (isDark) Color(0xFF140D09) else Color(0xFF472B17))
        // Glowing warm cabin window
        val winCenter = Offset(cx + w * 0.14f, cy + h * 0.065f)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFD878).copy(alpha = 0.65f + pulse * 0.2f), Color.Transparent), center = winCenter, radius = minD * 0.18f), radius = minD * 0.18f, center = winCenter)
        drawRoundRect(color = Color(0xFFFFE18C), topLeft = Offset(winCenter.x - w * 0.04f, winCenter.y - h * 0.025f), size = Size(w * 0.08f, h * 0.05f), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
    }
}

// 10. Cozy Study Loft: Bookshelves & glowing banker's lamp focusing onto open study books
private fun DrawScope.drawStudyLoftScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panX = pose.panX * w * 0.35f

    // Bookshelf columns in background
    val shelfColor = if (isDark) Color(0xFF211A14) else Color(0xFFD6BE9E)
    for (s in 0..2) {
        val sy = h * (0.18f + s * 0.14f - depth * 0.06f)
        drawRect(color = shelfColor, topLeft = Offset(w * 0.08f + panX * 0.4f, sy), size = Size(w * 0.84f, 6.dp.toPx()))
        for (b in 0..7) {
            val bh = h * (0.06f + (b % 3) * 0.015f)
            drawRoundRect(
                color = lerp(v.accent, v.frameSecondary, (b % 4) / 3f).copy(alpha = 0.42f),
                topLeft = Offset(w * (0.12f + b * 0.095f) + panX * 0.4f, sy - bh),
                size = Size(w * 0.05f, bh),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }

    // Study Desk & Warm Illuminated Reading Cone (zooms into focus in Settings)
    val deskY = h * (0.72f - depth * 0.06f)
    drawRect(color = if (isDark) Color(0xFF17110B) else Color(0xFFA67B51), topLeft = Offset(0f, deskY), size = Size(w, h - deskY))
    val lampHead = Offset(w * 0.68f + panX * 0.5f, deskY - h * 0.18f)
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.58f + pulse * 0.12f), Color.Transparent), center = Offset(lampHead.x - w * 0.08f, deskY), radius = minD * (0.34f + depth * 0.12f)),
        radius = minD * (0.34f + depth * 0.12f),
        center = Offset(lampHead.x - w * 0.08f, deskY)
    )
    // Open Study Book on desk
    val bookX = w * 0.28f + panX * 0.4f
    val bookScale = 1f + depth * 0.35f
    withTransform({ scale(bookScale, bookScale, pivot = Offset(bookX + w * 0.15f, deskY)) }) {
        drawRoundRect(color = Color(0xFFFFFAF0), topLeft = Offset(bookX, deskY - h * 0.025f), size = Size(w * 0.14f, h * 0.028f), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
        drawRoundRect(color = Color(0xFFFFF5E0), topLeft = Offset(bookX + w * 0.145f, deskY - h * 0.025f), size = Size(w * 0.14f, h * 0.028f), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
    }
    if (isDark) drawStarfield(w, h, seeds.take(10), v.lightColor, phase)
}

// 11. Developer Code Studio: Dual angled monitors & illuminated terminal syntax grid
private fun DrawScope.drawDevTerminalScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val scaleM = 1f + depth * 0.52f
    val panX = pose.panX * w * 0.3f

    withTransform({
        scale(scaleM, scaleM, pivot = Offset(w * 0.5f, h * 0.46f))
        translate(left = panX)
    }) {
        // Desk top
        drawRect(color = if (isDark) Color(0xFF0A0F1A) else Color(0xFFCBD8EA), topLeft = Offset(0f, h * 0.68f), size = Size(w, h * 0.32f))
        // Primary Widescreen Code Monitor
        val monL = w * 0.16f
        val monT = h * 0.28f
        val monW = w * 0.68f
        val monH = h * 0.32f
        drawRoundRect(color = if (isDark) Color(0xFF060B14) else Color(0xFF141D2E), topLeft = Offset(monL, monT), size = Size(monW, monH), cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()))
        drawRoundRect(color = v.accent.copy(alpha = 0.65f), topLeft = Offset(monL, monT), size = Size(monW, monH), cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
        // Animated syntax code bars inside IDE
        for (line in 0 until 7) {
            val ly = monT + h * 0.045f + line * h * 0.036f
            val lw = monW * (0.28f + ((line * 37) % 52) / 100f)
            val lColor = when (line % 3) {
                0 -> v.accent
                1 -> v.lightColor
                else -> v.frameSecondary
            }
            drawLine(color = lColor.copy(alpha = 0.78f), start = Offset(monL + w * 0.04f, ly), end = Offset(monL + w * 0.04f + lw, ly), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        // Keyboard glow bar
        drawRoundRect(color = v.accent.copy(alpha = 0.35f + pulse * 0.2f), topLeft = Offset(w * 0.28f, h * 0.73f), size = Size(w * 0.44f, h * 0.032f), cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()))
    }
    if (isDark) drawStarfield(w, h, seeds.take(12), v.lightColor, phase)
}

// 12. Grand Heritage Museum: Marble columns & rotunda spotlight descending onto pedestal in Settings
private fun DrawScope.drawGrandMuseumScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panX = pose.panX * w * 0.35f

    // Rotunda Dome Skylight Beam (opens directly overhead in Settings)
    val domeC = Offset(w * 0.5f + panX * 0.3f, h * (0.18f - depth * 0.05f))
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.55f + depth * 0.25f), Color.Transparent), center = domeC, radius = minD * (0.36f + depth * 0.16f)),
        radius = minD * (0.36f + depth * 0.16f),
        center = domeC
    )
    // Classical Gallery Pillars
    val colColor = if (isDark) Color(0xFF282233) else Color(0xFFE6DAC6)
    for (c in listOf(0.10f, 0.26f, 0.74f, 0.90f)) {
        val x = w * c + panX * 0.45f
        drawRect(color = colColor, topLeft = Offset(x - w * 0.025f, h * 0.18f), size = Size(w * 0.05f, h * 0.62f))
    }
    // Exhibit Pedestal & Golden Crown Artifact in center
    val pedY = h * (0.68f - depth * 0.06f)
    drawRect(color = lerp(colColor, v.accent, 0.28f), topLeft = Offset(w * 0.42f + panX * 0.3f, pedY), size = Size(w * 0.16f, h * 0.24f))
    drawCircle(color = v.accent, radius = minD * (0.045f + depth * 0.025f), center = Offset(w * 0.50f + panX * 0.3f, pedY - minD * 0.048f))
    drawStarfield(w, h, seeds.take(12), v.lightColor, phase)
}

// 13. Champions Football Arena: Stadium floodlights & turf dropping onto center penalty spot in Settings
private fun DrawScope.drawFootballStadiumScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panX = pose.panX * w * 0.3f

    // Stadium Roof Floodlights
    for (fx in listOf(0.18f, 0.82f)) {
        val tower = Offset(w * fx + panX * 0.4f, h * 0.14f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f + pulse * 0.15f), v.frameSecondary.copy(alpha = 0.28f), Color.Transparent), center = tower, radius = minD * 0.32f),
            radius = minD * 0.32f,
            center = tower
        )
        drawLine(
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), start = tower, end = Offset(w * 0.5f, h * 0.75f)),
            start = tower,
            end = Offset(w * 0.5f, h * 0.75f),
            strokeWidth = w * 0.28f
        )
    }

    // Lush Green Pitch Turf (rises prominently when entering Settings!)
    val turfY = h * (0.66f - depth * 0.18f)
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0E6E36), Color(0xFF073B1C))),
        topLeft = Offset(0f, turfY),
        size = Size(w, h - turfY)
    )
    // Center Kickoff Circle & Midline
    val pitchCenter = Offset(w * 0.5f + panX * 0.4f, turfY + (h - turfY) * 0.46f)
    drawLine(Color.White.copy(alpha = 0.55f), Offset(0f, pitchCenter.y), Offset(w, pitchCenter.y), strokeWidth = 2.dp.toPx())
    drawOval(
        color = Color.White.copy(alpha = 0.65f),
        topLeft = Offset(pitchCenter.x - w * (0.18f + depth * 0.08f), pitchCenter.y - h * (0.045f + depth * 0.025f)),
        size = Size(w * (0.36f + depth * 0.16f), h * (0.09f + depth * 0.05f)),
        style = Stroke(width = 2.dp.toPx())
    )
    if (isDark) drawStarfield(w, h, seeds.take(12), v.lightColor, phase)
}

// 14. Frutiger Aero Glass: Glossy 2000s aqua skies, lush emerald rolling hills, water spheres, bokeh flares & aqua waves
private fun DrawScope.drawFrutigerAeroScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose, lightEnabled: Boolean
) {
    val depth = pose.altitudeDepth
    val minD = size.minDimension
    val panX = pose.panX * w * 0.35f
    val panY = pose.panY * h * 0.20f

    // 1. Radiant Sunlight Flare (Light) or Bioluminescent Aurora Sky Ribbon (Dark)
    val flareCenter = Offset(w * (0.24f + pose.panX * 0.28f), h * (0.18f - depth * 0.05f) + panY)
    if (lightEnabled) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isDark) v.accent else Color.White).copy(alpha = if (isDark) 0.38f else 0.78f),
                    v.lightColor.copy(alpha = if (isDark) 0.24f else 0.42f),
                    Color.Transparent
                ),
                center = flareCenter,
                radius = minD * (0.48f + pulse * 0.06f)
            ),
            radius = minD * (0.48f + pulse * 0.06f),
            center = flareCenter
        )
    }

    if (isDark) {
        drawStarfield(w, h, seeds.take(18), Color(0xFFB8FFFF), phase)
        // Glowing Aqua Aurora wave in night Aero mode
        val aurora = Path().apply {
            moveTo(0f, h * 0.28f)
            cubicTo(w * 0.30f, h * (0.16f + pulse * 0.05f), w * 0.70f, h * 0.38f, w, h * 0.22f)
            lineTo(w, h * 0.46f)
            cubicTo(w * 0.68f, h * 0.56f, w * 0.32f, h * 0.36f, 0f, h * 0.48f)
            close()
        }
        drawPath(
            aurora,
            brush = Brush.horizontalGradient(
                listOf(v.accent.copy(alpha = 0.20f), v.frameSecondary.copy(alpha = 0.26f), v.lightColor.copy(alpha = 0.16f))
            )
        )
    } else {
        // Fluffy glossy clouds gliding across bright cerulean sky
        drawSoftCloudCluster(
            Offset(w * ((0.18f + phase * 0.18f) % 1.25f - 0.12f) + panX * 0.25f, h * (0.20f - depth * 0.04f)),
            minD * 0.21f,
            Color.White.copy(alpha = 0.78f)
        )
        drawSoftCloudCluster(
            Offset(w * ((0.80f - phase * 0.14f + 1.25f) % 1.25f - 0.12f) + panX * 0.3f, h * (0.31f - depth * 0.06f)),
            minD * 0.17f,
            Color.White.copy(alpha = 0.68f)
        )
    }

    // 2. Signature Diagonal Translucent Glass Sweep Ribbon across canvas
    withTransform({
        rotate(-14f + pose.angleDeg * 0.45f, pivot = Offset(w * 0.5f, h * 0.5f))
    }) {
        val ribbonPath = Path().apply {
            moveTo(-w * 0.2f, h * 0.52f)
            cubicTo(w * 0.25f, h * (0.38f - pulse * 0.04f), w * 0.72f, h * 0.62f, w * 1.2f, h * 0.40f)
            lineTo(w * 1.2f, h * 0.54f)
            cubicTo(w * 0.72f, h * 0.74f, w * 0.25f, h * (0.50f - pulse * 0.04f), -w * 0.2f, h * 0.64f)
            close()
        }
        drawPath(
            ribbonPath,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
                    v.accent.copy(alpha = if (isDark) 0.18f else 0.22f),
                    Color.White.copy(alpha = if (isDark) 0.08f else 0.26f)
                )
            )
        )
    }

    // 3. Soft Aero Lens Bokeh Discs
    val bokehSpots = listOf(
        Triple(0.18f, 0.36f, 0.065f),
        Triple(0.36f, 0.24f, 0.045f),
        Triple(0.76f, 0.34f, 0.075f),
        Triple(0.62f, 0.56f, 0.052f)
    )
    bokehSpots.forEachIndexed { idx, (bx, by, br) ->
        val drift = sin(phase * 2f * PI.toFloat() + idx * 1.3f) * minD * 0.018f
        val bc = Offset(w * bx + panX * (0.25f + idx * 0.1f) + drift, h * by + panY * 0.4f)
        drawCircle(
            color = (if (idx % 2 == 0) Color.White else v.lightColor).copy(alpha = if (isDark) 0.12f else 0.26f),
            radius = minD * br,
            center = bc
        )
        drawCircle(
            color = Color.White.copy(alpha = if (isDark) 0.18f else 0.34f),
            radius = minD * br,
            center = bc,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // 4. Layered Glossy Emerald Meadow Hills (rise as camera descends toward ground in Settings!)
    val backHillY = h * (0.78f - depth * 0.18f)
    val backHill = Path().apply {
        moveTo(0f, h)
        lineTo(0f, backHillY)
        cubicTo(w * 0.34f, backHillY - h * 0.14f, w * 0.68f, backHillY + h * 0.04f, w, backHillY - h * 0.10f)
        lineTo(w, h)
        close()
    }
    drawPath(
        backHill,
        brush = Brush.verticalGradient(
            colors = if (isDark) listOf(Color(0xFF106E5C), Color(0xFF062C2B)) else listOf(Color(0xFF39D97B), Color(0xFF0FA360)),
            startY = backHillY - h * 0.15f,
            endY = h
        )
    )

    val frontHillY = h * (0.86f - depth * 0.20f)
    val frontHill = Path().apply {
        moveTo(0f, h)
        lineTo(0f, frontHillY + h * 0.04f)
        cubicTo(w * 0.32f, frontHillY - h * 0.06f, w * 0.70f, frontHillY - h * 0.15f, w, frontHillY)
        lineTo(w, h)
        close()
    }
    drawPath(
        frontHill,
        brush = Brush.verticalGradient(
            colors = if (isDark) listOf(Color(0xFF1A8E6F), Color(0xFF083931)) else listOf(Color(0xFF7BF26B), Color(0xFF19B56B)),
            startY = frontHillY - h * 0.15f,
            endY = h
        )
    )

    // 5. Iconic Glossy 3D Aero Aqua Water Orbs (Gelatinous glass highlight + caustic ring)
    fun drawGlossyAeroOrb(center: Offset, r: Float, coreStart: Color, coreEnd: Color) {
        // Outer aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreStart.copy(alpha = 0.45f), Color.Transparent),
                center = center,
                radius = r * 1.48f
            ),
            radius = r * 1.48f,
            center = center
        )
        // Translucent aqua body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.72f), coreStart.copy(alpha = 0.82f), coreEnd.copy(alpha = 0.90f)),
                center = Offset(center.x - r * 0.28f, center.y - r * 0.28f),
                radius = r * 1.32f
            ),
            radius = r,
            center = center
        )
        // Crisp Aero glass rim
        drawCircle(
            color = Color.White.copy(alpha = 0.68f),
            radius = r,
            center = center,
            style = Stroke(width = (r * 0.06f).coerceAtLeast(1.2f))
        )
        // Iconic glossy upper-left crescent reflection
        rotate(-28f, pivot = center) {
            drawOval(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.88f), Color.Transparent)),
                topLeft = Offset(center.x - r * 0.56f, center.y - r * 0.80f),
                size = Size(r * 1.12f, r * 0.56f)
            )
        }
    }

    val heroOrbCenter = Offset(
        w * (0.68f + pose.panX * 0.45f),
        h * (0.50f + depth * 0.08f) + sin(phase * 2f * PI.toFloat()) * minD * 0.022f
    )
    val heroOrbR = minD * (0.145f + depth * 0.055f) * (1f + (pose.zoom - 1f) * 0.35f)
    drawGlossyAeroOrb(
        center = heroOrbCenter,
        r = heroOrbR,
        coreStart = if (isDark) Color(0xFF2CEBFF) else Color(0xFF3BD4FF),
        coreEnd = if (isDark) Color(0xFF0A5C8C) else Color(0xFF007FD4)
    )

    val mintOrbCenter = Offset(
        w * (0.28f + pose.panX * 0.32f),
        h * (0.60f - depth * 0.06f) + cos(phase * 2f * PI.toFloat()) * minD * 0.018f
    )
    drawGlossyAeroOrb(
        center = mintOrbCenter,
        r = minD * (0.082f + depth * 0.025f),
        coreStart = Color(0xFF5CF5A3),
        coreEnd = Color(0xFF0B9E68)
    )

    // Deep Aero Aquamarine Tropical Fish gliding gracefully past in Settings/Profile zoom
    if (depth > 0.25f) {
        val fishAlpha = ((depth - 0.25f) / 0.75f).coerceIn(0f, 0.85f)
        val fx = w * ((0.15f + phase * 0.85f) % 1.1f - 0.05f)
        val fy = h * (0.66f - depth * 0.06f) + sin(phase * 4f * PI.toFloat()) * minD * 0.018f
        val fr = minD * 0.042f
        val tail = Path().apply {
            moveTo(fx - fr * 0.7f, fy)
            lineTo(fx - fr * 1.45f, fy - fr * 0.55f)
            lineTo(fx - fr * 1.45f, fy + fr * 0.55f)
            close()
        }
        drawPath(tail, color = Color(0xFFFFA83B).copy(alpha = fishAlpha * 0.9f))
        drawOval(
            color = Color(0xFFFF8C2B).copy(alpha = fishAlpha),
            topLeft = Offset(fx - fr * 0.85f, fy - fr * 0.45f),
            size = Size(fr * 1.7f, fr * 0.9f)
        )
        drawCircle(color = Color.White.copy(alpha = fishAlpha), radius = fr * 0.14f, center = Offset(fx + fr * 0.42f, fy - fr * 0.10f))
    }
}

private fun DrawScope.drawSakuraScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val sunMoonCenter = Offset(w * (0.78f - pose.panX * 0.35f), h * (0.22f + depth * 0.06f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(v.lightColor.copy(alpha = 0.75f), v.accent.copy(alpha = 0.2f), Color.Transparent),
            center = sunMoonCenter,
            radius = size.minDimension * 0.38f
        ),
        radius = size.minDimension * 0.38f,
        center = sunMoonCenter
    )
    drawCircle(color = v.lightColor.copy(alpha = 0.9f), radius = size.minDimension * 0.11f, center = sunMoonCenter)

    // Stylized sakura branch silhouette + Japanese Torii Teahouse rising in Settings
    val branch = Path().apply {
        moveTo(0f, h * (0.14f + pose.panY * 0.2f))
        quadraticTo(w * 0.35f, h * 0.19f, w * 0.68f, h * 0.10f)
    }
    drawPath(branch, color = if (isDark) Color(0xFF421E36) else Color(0xFF6B304D), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

    if (depth > 0.25f) {
        val gateAlpha = ((depth - 0.25f) / 0.75f).coerceIn(0f, 0.72f)
        val gy = h * 0.62f
        drawRect(color = v.accent.copy(alpha = gateAlpha), topLeft = Offset(w * 0.24f, gy), size = Size(w * 0.52f, 8.dp.toPx()))
        drawRect(color = v.frameSecondary.copy(alpha = gateAlpha), topLeft = Offset(w * 0.32f, gy + 8.dp.toPx()), size = Size(8.dp.toPx(), h * 0.25f))
        drawRect(color = v.frameSecondary.copy(alpha = gateAlpha), topLeft = Offset(w * 0.60f, gy + 8.dp.toPx()), size = Size(8.dp.toPx(), h * 0.25f))
    }

    // Drifting sakura petals
    seeds.take(20).forEachIndexed { idx, (sx, sy, weight) ->
        val x = ((sx + phase * (0.25f + weight * 0.2f) + pose.panX * 0.3f + 1f) % 1.1f) * w
        val y = ((sy + phase * (0.35f + weight * 0.25f) + 1f) % 1.1f) * h
        rotate(phase * 360f + idx * 35f + pose.angleDeg, pivot = Offset(x, y)) {
            drawOval(
                color = lerp(v.accent, Color.White, 0.35f).copy(alpha = 0.75f),
                topLeft = Offset(x - 5.dp.toPx() * weight, y - 3.dp.toPx() * weight),
                size = Size(10.dp.toPx() * weight, 6.dp.toPx() * weight)
            )
        }
    }
}

private fun DrawScope.drawAuroraScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    if (isDark) drawStarfield(w, h, seeds, Color.White, phase)
    for (band in 0..2) {
        val yBase = h * (0.20f + band * 0.15f - depth * 0.08f)
        val wave = sin((phase + band * 0.3f) * 2f * PI.toFloat()) * h * 0.06f
        val path = Path().apply {
            moveTo(0f, yBase + wave)
            cubicTo(w * 0.33f, yBase - h * 0.14f - wave, w * 0.66f, yBase + h * 0.14f + wave, w, yBase - wave)
            lineTo(w, yBase + h * 0.22f)
            lineTo(0f, yBase + h * 0.22f)
            close()
        }
        val bandColor = when (band) {
            0 -> v.accent
            1 -> v.frameSecondary
            else -> v.lightColor
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(bandColor.copy(alpha = 0.42f + pulse * 0.12f), Color.Transparent)
            )
        )
    }
    // Ice crystal horizon ridge stepping upward in Settings
    val iceRidge = Path().apply {
        moveTo(0f, h); lineTo(0f, h * (0.86f - depth * 0.16f))
        lineTo(w * 0.32f, h * (0.74f - depth * 0.14f))
        lineTo(w * 0.68f, h * (0.80f - depth * 0.12f))
        lineTo(w, h * (0.72f - depth * 0.15f)); lineTo(w, h); close()
    }
    drawPath(iceRidge, color = v.accent.copy(alpha = 0.16f + depth * 0.18f))
}

private fun DrawScope.drawButterfliesScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawStarfield(w, h, seeds.take(18), v.lightColor, phase)
    for (i in 0..6) {
        val bx = w * ((0.14f + i * 0.14f + pose.panX * 0.25f + sin(phase * 2f * PI.toFloat() + i) * 0.06f))
        val by = h * ((0.22f + (i % 3) * 0.22f - phase * 0.15f - depth * 0.08f + 1.0f) % 0.85f)
        val wingSpread = (0.45f + 0.55f * kotlin.math.abs(sin(phase * 10f + i))) * size.minDimension * (0.036f + depth * 0.012f)
        val wingH = size.minDimension * (0.032f + depth * 0.01f)
        val wingColor = if (i % 2 == 0) v.accent else v.frameSecondary
        drawOval(color = wingColor.copy(alpha = 0.78f), topLeft = Offset(bx - wingSpread, by - wingH * 0.5f), size = Size(wingSpread, wingH))
        drawOval(color = wingColor.copy(alpha = 0.78f), topLeft = Offset(bx, by - wingH * 0.5f), size = Size(wingSpread, wingH))
    }
}

private fun DrawScope.drawOceanPearlScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    for (i in 0..2) {
        drawLine(
            brush = Brush.linearGradient(listOf(v.lightColor.copy(alpha = 0.35f), Color.Transparent)),
            start = Offset(w * (0.2f + i * 0.25f + pose.panX * 0.2f), 0f),
            end = Offset(w * (0.35f + i * 0.22f + pulse * 0.08f), h * 0.85f),
            strokeWidth = w * 0.10f
        )
    }
    if (depth > 0.3f) {
        val shellCenter = Offset(w * 0.5f, h * (0.86f - depth * 0.12f))
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, v.accent.copy(alpha = 0.55f), Color.Transparent), center = shellCenter, radius = size.minDimension * 0.22f),
            radius = size.minDimension * 0.22f,
            center = shellCenter
        )
    }
    seeds.take(18).forEach { (sx, sy, weight) ->
        val cx = sx * w
        val cy = ((sy - phase * (0.35f + weight * 0.2f) + 1.2f) % 1.0f) * h
        val r = (3f + weight * 5f).dp.toPx() * (size.minDimension / 400f).coerceIn(0.45f, 1.3f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, v.accent.copy(alpha = 0.6f))),
            radius = r,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawRoyalRoseScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val bloomCenter = Offset(w * (0.75f - depth * 0.25f + pose.panX * 0.3f), h * (0.24f + depth * 0.12f))
    for (ring in 3 downTo 1) {
        val radius = size.minDimension * 0.11f * ring * (0.94f + pulse * 0.08f + depth * 0.22f)
        rotate(phase * 45f * (if (ring % 2 == 0) 1f else -1f) + pose.angleDeg, pivot = bloomCenter) {
            for (p in 0 until 6) {
                rotate(p * 60f, pivot = bloomCenter) {
                    drawOval(
                        color = lerp(v.accent, v.frameSecondary, ring / 3f).copy(alpha = 0.26f),
                        topLeft = Offset(bloomCenter.x - radius * 0.4f, bloomCenter.y - radius),
                        size = Size(radius * 0.8f, radius * 1.4f)
                    )
                }
            }
        }
    }
    drawStarfield(w, h, seeds.take(14), v.lightColor, phase)
}

private fun DrawScope.drawMoonlightScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawStarfield(w, h, seeds, v.lightColor, phase)
    val c = Offset(w * (0.74f - pose.panX * 0.3f), h * (0.24f - depth * 0.06f))
    val r = size.minDimension * 0.14f
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.45f), Color.Transparent), center = c, radius = r * 2.4f),
        radius = r * 2.4f,
        center = c
    )
    drawCircle(color = v.lightColor, radius = r, center = c)
    drawCircle(color = v.bgTop, radius = r * 0.84f, center = Offset(c.x + r * 0.36f, c.y - r * 0.18f))
    if (depth > 0.25f) {
        // Moonlit tranquil lake surface reflection in Settings
        val lakeY = h * (0.88f - depth * 0.18f)
        drawRect(color = v.accent.copy(alpha = 0.22f * depth), topLeft = Offset(0f, lakeY), size = Size(w, h - lakeY))
    }
}

private fun DrawScope.drawCrystalPrismScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val center = Offset(w * (0.5f + pose.panX * 0.25f), h * (0.35f + depth * 0.10f))
    val r = size.minDimension * (0.26f + depth * 0.12f)
    rotate(phase * 90f + pose.angleDeg, pivot = center) {
        for (i in 0 until 6) {
            val angle = i * 60f * (PI.toFloat() / 180f)
            val p1 = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
            val p2 = Offset(center.x + cos(angle + 0.9f) * r * 0.7f, center.y + sin(angle + 0.9f) * r * 0.7f)
            val poly = Path().apply { moveTo(center.x, center.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }
            drawPath(poly, color = (if (i % 2 == 0) v.accent else v.frameSecondary).copy(alpha = 0.26f + pulse * 0.1f))
        }
    }
    drawStarfield(w, h, seeds.take(18), v.lightColor, phase)
}

private fun DrawScope.drawLavenderScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.45f), Color.Transparent), center = Offset(w * (0.25f + pose.panX * 0.3f), h * 0.2f), radius = w * 0.65f),
        radius = w * 0.65f,
        center = Offset(w * (0.25f + pose.panX * 0.3f), h * 0.2f)
    )
    for (i in 0 until 8) {
        val x = w * (0.08f + i * 0.12f)
        val sway = sin(phase * 2f * PI.toFloat() + i) * w * 0.03f
        drawLine(
            color = v.accent.copy(alpha = 0.45f),
            start = Offset(x, h),
            end = Offset(x + sway, h * (0.68f + (i % 3) * 0.05f - depth * 0.12f)),
            strokeWidth = (3f + depth * 1.5f).dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    drawStarfield(w, h, seeds.take(16), v.lightColor, phase)
}

private fun DrawScope.drawSunsetPlumeScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.7f), v.accent.copy(alpha = 0.25f), Color.Transparent), center = Offset(w * (0.5f + pose.panX * 0.3f), h * (0.28f + depth * 0.08f)), radius = w * 0.55f),
        radius = w * 0.55f,
        center = Offset(w * (0.5f + pose.panX * 0.3f), h * (0.28f + depth * 0.08f))
    )
    for (i in 0..2) {
        val y = h * (0.52f + i * 0.14f - depth * 0.08f)
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(w * 0.35f, y - h * 0.08f * (1 + pulse), w * 0.7f, y + h * 0.08f, w, y)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(path, color = (if (i % 2 == 0) v.accent else v.frameSecondary).copy(alpha = 0.24f))
    }
    drawStarfield(w, h, seeds.take(12), v.lightColor, phase)
}

private fun DrawScope.drawCottonCloudScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawArc(
        brush = Brush.sweepGradient(listOf(v.accent.copy(alpha = 0.25f), v.frameSecondary.copy(alpha = 0.25f), v.lightColor.copy(alpha = 0.25f))),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.1f + pose.panX * w * 0.2f, h * (0.22f - depth * 0.06f)),
        size = Size(w * 0.8f, h * 0.55f),
        style = Stroke(width = size.minDimension * 0.06f)
    )
    val minD = size.minDimension
    drawSoftCloudCluster(Offset(w * ((0.22f + phase * 0.25f) % 1.2f - 0.1f), h * (0.24f - depth * 0.06f)), minD * 0.2f, if (isDark) Color(0x448EC5FF) else Color.White.copy(alpha = 0.72f))
    drawSoftCloudCluster(Offset(w * ((0.78f - phase * 0.2f + 1.2f) % 1.2f - 0.1f), h * (0.68f - depth * 0.12f)), minD * (0.24f + depth * 0.06f), if (isDark) Color(0x44FFC2EC) else Color.White.copy(alpha = 0.65f))
    if (isDark) drawStarfield(w, h, seeds.take(16), v.lightColor, phase)
}

private fun DrawScope.drawCyberNeonScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant, phase: Float, pulse: Float, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val horizonY = h * (0.54f - depth * 0.12f)
    val glow = Brush.radialGradient(listOf(v.accent.copy(alpha = 0.45f), Color.Transparent), center = Offset(w * (0.5f + pose.panX * 0.25f), horizonY), radius = w * 0.65f)
    drawCircle(brush = glow, radius = w * 0.65f, center = Offset(w * (0.5f + pose.panX * 0.25f), horizonY))
    for (i in -4..4) {
        drawLine(
            color = v.accent.copy(alpha = 0.35f),
            start = Offset(w * (0.5f + pose.panX * 0.2f) + i * w * 0.04f, horizonY),
            end = Offset(w * 0.5f + i * w * 0.32f, h),
            strokeWidth = 1.2.dp.toPx()
        )
    }
    for (row in 0 until 6) {
        val t = ((row / 6f) + phase) % 1f
        val y = horizonY + (t * t) * (h - horizonY)
        drawLine(
            color = v.frameSecondary.copy(alpha = t * 0.55f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawObsidianMagmaScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, v.accent.copy(alpha = 0.32f + pulse * 0.15f + depth * 0.18f))),
        topLeft = Offset(0f, h * (0.45f - depth * 0.14f)),
        size = Size(w, h * (0.55f + depth * 0.14f))
    )
    seeds.take(24).forEach { (sx, sy, weight) ->
        val cy = ((sy - phase * (0.5f + weight * 0.4f) + 1.5f) % 1.0f) * h
        val cx = (sx * w) + sin(phase * 6f + weight * 10f) * w * 0.04f + pose.panX * w * 0.15f
        drawCircle(
            color = lerp(v.accent, v.lightColor, weight).copy(alpha = 0.85f),
            radius = (2f + weight * 2.5f).dp.toPx() * (size.minDimension / 420f).coerceIn(0.4f, 1.2f),
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawArcticStormScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val mtn = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * (0.72f - depth * 0.12f))
        lineTo(w * 0.25f, h * (0.55f - depth * 0.10f))
        lineTo(w * 0.48f, h * (0.68f - depth * 0.10f))
        lineTo(w * 0.76f, h * (0.48f - depth * 0.12f))
        lineTo(w, h * (0.66f - depth * 0.10f))
        lineTo(w, h); close()
    }
    drawPath(mtn, color = v.accent.copy(alpha = (if (isDark) 0.24f else 0.18f) + depth * 0.12f))
    seeds.take(22).forEach { (sx, sy, weight) ->
        val cx = ((sx + phase * (0.3f + weight * 0.2f)) % 1.0f) * w
        val cy = ((sy + phase * (0.4f + weight * 0.25f)) % 1.0f) * h
        drawCircle(color = v.lightColor.copy(alpha = 0.8f), radius = (1.8f * weight).dp.toPx(), center = Offset(cx, cy))
    }
}

private fun DrawScope.drawShadowSamuraiScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val c = Offset(w * (0.5f + pose.panX * 0.25f), h * (0.34f - depth * 0.06f))
    val r = size.minDimension * (0.24f + depth * 0.06f) * (0.96f + pulse * 0.05f)
    drawCircle(
        brush = Brush.radialGradient(listOf(v.accent, v.accent.copy(alpha = 0.7f))),
        radius = r,
        center = c
    )
    val ridge = Path().apply {
        moveTo(0f, h); lineTo(0f, h * (0.74f - depth * 0.10f)); lineTo(w * 0.32f, h * (0.60f - depth * 0.08f))
        lineTo(w * 0.65f, h * (0.72f - depth * 0.08f)); lineTo(w, h * (0.58f - depth * 0.10f)); lineTo(w, h); close()
    }
    drawPath(ridge, color = (if (isDark) Color.Black else Color(0xFF2D2528)).copy(alpha = 0.55f))
    drawStarfield(w, h, seeds.take(12), v.lightColor, phase)
}

private fun DrawScope.drawCosmicVortexScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawStarfield(w, h, seeds, Color.White, phase)
    val c = Offset(w * (0.5f + pose.panX * 0.25f), h * (0.42f + pose.panY * 0.18f))
    for (ring in 1..4) {
        rotate(phase * 360f * (if (ring % 2 == 0) 1f else -0.7f) + pose.angleDeg, pivot = c) {
            drawOval(
                color = lerp(v.accent, v.frameSecondary, ring / 4f).copy(alpha = 0.45f),
                topLeft = Offset(c.x - size.minDimension * (0.1f + depth * 0.03f) * ring, c.y - size.minDimension * (0.045f + depth * 0.015f) * ring),
                size = Size(size.minDimension * (0.2f + depth * 0.06f) * ring, size.minDimension * (0.09f + depth * 0.03f) * ring),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
    drawCircle(color = if (isDark) Color.Black else Color.White, radius = size.minDimension * 0.07f, center = c)
}

private fun DrawScope.drawDeepTrenchScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val c = Offset(w * (0.5f + pose.panX * 0.25f), h * (0.48f + depth * 0.14f))
    for (rIdx in 0..2) {
        val ratio = ((phase + rIdx / 3f) % 1f)
        drawCircle(
            color = v.accent.copy(alpha = (1f - ratio) * 0.5f),
            radius = size.minDimension * (0.55f + depth * 0.15f) * ratio,
            center = c,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    drawStarfield(w, h, seeds.take(16), v.lightColor, phase)
}

private fun DrawScope.drawDesertFalconScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    drawCircle(
        brush = Brush.radialGradient(listOf(v.lightColor.copy(alpha = 0.7f), Color.Transparent), center = Offset(w * (0.7f + pose.panX * 0.3f), h * (0.24f - depth * 0.06f)), radius = size.minDimension * 0.38f),
        radius = size.minDimension * 0.38f,
        center = Offset(w * (0.7f + pose.panX * 0.3f), h * (0.24f - depth * 0.06f))
    )
    val dune1 = Path().apply {
        moveTo(0f, h * (0.72f - depth * 0.14f))
        quadraticTo(w * 0.35f, h * (0.56f - depth * 0.12f), w, h * (0.68f - depth * 0.10f))
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(dune1, color = v.accent.copy(alpha = 0.35f + depth * 0.15f))
    if (isDark) drawStarfield(w, h, seeds.take(20), v.lightColor, phase)
}

private fun DrawScope.drawTitaniumMechScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant, phase: Float, pulse: Float, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val step = size.minDimension * (0.22f + depth * 0.06f)
    val cols = (w / step).toInt() + 2
    val rows = (h / step).toInt() + 2
    for (r in 0..rows) {
        for (c in 0..cols) {
            val cx = c * step + if (r % 2 == 0) 0f else step * 0.5f + pose.panX * w * 0.2f
            val cy = r * step * 0.85f + pose.panY * h * 0.2f
            drawCircle(
                color = v.accent.copy(alpha = 0.12f + ((r + c) % 3) * 0.05f * pulse + depth * 0.08f),
                radius = step * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawEmeraldPhantomScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val forest = Path().apply {
        moveTo(0f, h); lineTo(0f, h * (0.75f - depth * 0.14f)); lineTo(w * 0.2f, h * (0.6f - depth * 0.12f))
        lineTo(w * 0.4f, h * (0.74f - depth * 0.12f)); lineTo(w * 0.6f, h * (0.56f - depth * 0.12f))
        lineTo(w * 0.85f, h * (0.72f - depth * 0.10f)); lineTo(w, h * (0.62f - depth * 0.12f)); lineTo(w, h); close()
    }
    drawPath(forest, color = v.accent.copy(alpha = 0.25f + depth * 0.15f))
    drawStarfield(w, h, seeds.take(22), v.lightColor, phase)
}

private fun DrawScope.drawThunderApexScene(
    w: Float, h: Float, isDark: Boolean, v: CinematicThemeVariant,
    phase: Float, pulse: Float, seeds: List<Triple<Float, Float, Float>>, pose: SceneCameraPose
) {
    val depth = pose.altitudeDepth
    val boltAlpha = if (pulse > 0.72f) 0.9f else 0.35f
    val bolt = Path().apply {
        moveTo(w * (0.65f + pose.panX * 0.2f), 0f)
        lineTo(w * 0.48f, h * (0.34f + depth * 0.08f))
        lineTo(w * 0.58f, h * (0.38f + depth * 0.08f))
        lineTo(w * 0.36f, h * (0.72f + depth * 0.10f))
    }
    drawPath(
        path = bolt,
        color = v.accent.copy(alpha = boltAlpha),
        style = Stroke(width = (3f + depth * 1.2f).dp.toPx(), cap = StrokeCap.Round)
    )
    drawStarfield(w, h, seeds.take(14), v.frameSecondary, phase)
}

private fun DrawScope.drawSunriseTreeMountainWaterScene(
    w: Float,
    h: Float,
    isDark: Boolean,
    v: CinematicThemeVariant,
    phase: Float,
    pulse: Float,
    seeds: List<Triple<Float, Float, Float>>,
    pose: SceneCameraPose,
    lightEnabled: Boolean,
    artwork: ImageBitmap?
) {
    val depth = pose.altitudeDepth
    if (artwork != null) {
        val imgW = artwork.width.toFloat().coerceAtLeast(1f)
        val imgH = artwork.height.toFloat().coerceAtLeast(1f)
        val scale = max(w / imgW, h / imgH) * (1.12f + (pose.zoom - 1f) * 0.45f)
        val drawW = (imgW * scale).toInt()
        val drawH = (imgH * scale).toInt()
        val offsetX = ((w - drawW) / 2f + pose.panX * w * 0.14f).toInt()
        val offsetY = ((h - drawH) / 2f + pose.panY * h * 0.12f).toInt()

        withTransform({
            rotate(degrees = pose.angleDeg * 0.18f, pivot = Offset(w / 2f, h / 2f))
        }) {
            drawImage(
                image = artwork,
                dstOffset = IntOffset(offsetX, offsetY),
                dstSize = IntSize(drawW, drawH),
                alpha = if (isDark) 0.54f else 0.90f
            )
        }
    }

    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xA6081224),
                    Color(0x52121D33),
                    Color(0x88080D17)
                )
            )
        )
        drawStarfield(w, h * 0.46f, seeds.take(22), v.lightColor, phase)
        if (lightEnabled) {
            val glowCenter = Offset(w * (0.50f + pose.panX * 0.25f), h * (0.48f - depth * 0.05f))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x55FFD68A), Color(0x1FFFA43B), Color.Transparent),
                    center = glowCenter,
                    radius = size.minDimension * 0.46f
                ),
                radius = size.minDimension * 0.46f,
                center = glowCenter
            )
        }
    } else {
        if (lightEnabled) {
            val sunCenter = Offset(w * (0.50f + pose.panX * 0.25f), h * (0.44f - depth * 0.06f))
            val radius = size.minDimension * (0.42f + pulse * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x66FFF8D1), Color(0x33FFC459), Color.Transparent),
                    center = sunCenter,
                    radius = radius
                ),
                radius = radius,
                center = sunCenter
            )
        }
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x1AFFF5E2), Color.Transparent, Color(0x33141E0F))
            )
        )
    }
}

private fun DrawScope.drawStarfield(
    w: Float, h: Float, seeds: List<Triple<Float, Float, Float>>, color: Color, phase: Float
) {
    seeds.forEachIndexed { idx, (sx, sy, weight) ->
        val twinkle = 0.35f + 0.65f * kotlin.math.abs(sin(phase * 2f * PI.toFloat() + idx))
        drawCircle(
            color = color.copy(alpha = (0.3f + weight * 0.5f) * twinkle),
            radius = (1.1f + weight * 1.4f).dp.toPx() * (size.minDimension / 420f).coerceIn(0.45f, 1.25f),
            center = Offset(sx * w, sy * h)
        )
    }
}

private fun DrawScope.drawSoftCloudCluster(center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius * 0.55f, center = center)
    drawCircle(color = color, radius = radius * 0.45f, center = Offset(center.x - radius * 0.48f, center.y + radius * 0.12f))
    drawCircle(color = color, radius = radius * 0.42f, center = Offset(center.x + radius * 0.46f, center.y + radius * 0.15f))
}

/**
 * Renders an ornate, non-obstructive themed Avatar Frame ring around JesrAvatar whenever
 * a Cinematic Theme is active.
 */
@Composable
fun CinematicAvatarFrameOverlay(
    frameStyle: CinematicAvatarFrameStyle,
    variant: CinematicThemeVariant,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = size.minDimension / 2f - 1.5.dp.toPx()
        val ringBrush = Brush.sweepGradient(
            colors = listOf(variant.framePrimary, variant.frameSecondary, variant.lightColor, variant.framePrimary),
            center = center
        )

        when (frameStyle) {
            CinematicAvatarFrameStyle.ORBITAL_RING -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 2.6.dp.toPx()))
                rotate(-24f, pivot = center) {
                    drawOval(
                        color = variant.lightColor.copy(alpha = 0.85f),
                        topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 0.36f),
                        size = Size(radius * 2.36f, radius * 0.72f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
                drawCircle(color = variant.lightColor, radius = 3.2.dp.toPx(), center = Offset(center.x + radius * 0.82f, center.y - radius * 0.56f))
            }
            CinematicAvatarFrameStyle.PETAL_HALO -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 2.5.dp.toPx()))
                for (i in 0 until 6) {
                    val a = i * 60f * (PI.toFloat() / 180f)
                    drawCircle(
                        color = variant.framePrimary,
                        radius = 2.6.dp.toPx(),
                        center = Offset(center.x + cos(a) * radius, center.y + sin(a) * radius)
                    )
                }
            }
            CinematicAvatarFrameStyle.PRISM_JEWEL -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 2.8.dp.toPx()))
                drawCircle(color = variant.lightColor, radius = 3.2.dp.toPx(), center = Offset(center.x, center.y - radius))
                drawCircle(color = variant.frameSecondary, radius = 2.6.dp.toPx(), center = Offset(center.x, center.y + radius))
            }
            CinematicAvatarFrameStyle.CRESCENT_CROWN -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 2.6.dp.toPx()))
                drawArc(
                    color = variant.lightColor,
                    startAngle = -140f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 1.12f, center.y - radius * 1.12f),
                    size = Size(radius * 2.24f, radius * 2.24f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            CinematicAvatarFrameStyle.CYBER_BEZEL -> {
                for (i in 0 until 4) {
                    drawArc(
                        brush = ringBrush,
                        startAngle = i * 90f + 10f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Butt)
                    )
                }
            }
            CinematicAvatarFrameStyle.FORGED_HEX -> {
                val hex = Path()
                for (i in 0 until 6) {
                    val a = (i * 60f - 30f) * (PI.toFloat() / 180f)
                    val px = center.x + cos(a) * (radius * 1.04f)
                    val py = center.y + sin(a) * (radius * 1.04f)
                    if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
                }
                hex.close()
                drawPath(hex, brush = ringBrush, style = Stroke(width = 2.8.dp.toPx()))
            }
            CinematicAvatarFrameStyle.FLAME_CREST -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = variant.lightColor.copy(alpha = 0.7f), radius = radius * 1.07f, center = center, style = Stroke(width = 1.dp.toPx()))
            }
            CinematicAvatarFrameStyle.PEARL_DOUBLE -> {
                drawCircle(brush = ringBrush, radius = radius, center = center, style = Stroke(width = 2.4.dp.toPx()))
                drawCircle(color = variant.lightColor.copy(alpha = 0.85f), radius = radius * 0.90f, center = center, style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}
