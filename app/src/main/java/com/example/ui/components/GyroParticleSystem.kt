package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CinematicSceneType
import com.example.ui.theme.CinematicThemeSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GyroParticleKind {
    SNOW_FROST,
    COSMIC_DUST,
    SUN_MOTES,
    SAKURA_PETALS,
    CAMPFIRE_EMBERS,
    FIREFLIES,
    WATER_BUBBLES,
    SPARKLES
}

data class GyroTiltState(
    val x: Float = 0f,
    val y: Float = 0f
)

data class GyroParticleSeed(
    val baseX: Float,
    val baseY: Float,
    val speed: Float,
    val depth: Float,
    val sizeWeight: Float,
    val phaseOffset: Float,
    val driftAngle: Float
)

fun particleKindForTheme(
    spec: CinematicThemeSpec?,
    isDark: Boolean,
    lightEnabled: Boolean
): GyroParticleKind {
    if (spec == null) {
        return if (isDark) GyroParticleKind.COSMIC_DUST else GyroParticleKind.SUN_MOTES
    }
    return when (spec.sceneType) {
        CinematicSceneType.FROST_CAMPFIRE -> GyroParticleKind.SNOW_FROST
        CinematicSceneType.ARCTIC_STORM -> GyroParticleKind.SNOW_FROST
        CinematicSceneType.SAKURA_GARDEN, CinematicSceneType.ROYAL_ROSE -> GyroParticleKind.SAKURA_PETALS
        CinematicSceneType.OBSIDIAN_MAGMA -> GyroParticleKind.CAMPFIRE_EMBERS
        CinematicSceneType.OVERLAND_CAMP_TRUCK -> if (isDark) GyroParticleKind.CAMPFIRE_EMBERS else GyroParticleKind.SUN_MOTES
        CinematicSceneType.FOREST_CABIN,
        CinematicSceneType.EMERALD_PHANTOM,
        CinematicSceneType.LUMINOUS_BUTTERFLIES -> GyroParticleKind.FIREFLIES
        CinematicSceneType.OCEAN_ODYSSEY,
        CinematicSceneType.OCEAN_PEARL,
        CinematicSceneType.DEEP_TRENCH,
        CinematicSceneType.FRUTIGER_AERO -> GyroParticleKind.WATER_BUBBLES
        CinematicSceneType.ASTRONAUT -> if (isDark) GyroParticleKind.COSMIC_DUST else GyroParticleKind.SUN_MOTES
        CinematicSceneType.COSMIC_VORTEX,
        CinematicSceneType.ASTRAL_EXPRESS,
        CinematicSceneType.STAR_DESERT,
        CinematicSceneType.MOONLIGHT_CRESCENT -> GyroParticleKind.COSMIC_DUST
        CinematicSceneType.CRYSTAL_PRISM,
        CinematicSceneType.FOOTBALL_STADIUM,
        CinematicSceneType.AURORA_PRINCESS -> GyroParticleKind.SPARKLES
        CinematicSceneType.SUNLIT_BEDROOM,
        CinematicSceneType.SUNRISE_TREE_MOUNTAIN_WATER -> if (isDark) GyroParticleKind.COSMIC_DUST else GyroParticleKind.SUN_MOTES
        CinematicSceneType.STUDY_LOFT,
        CinematicSceneType.GRAND_MUSEUM,
        CinematicSceneType.SKY_SUMMIT,
        CinematicSceneType.SUNSET_PLUME,
        CinematicSceneType.COTTON_CLOUD,
        CinematicSceneType.DESERT_FALCON,
        CinematicSceneType.LAVENDER_BREEZE -> GyroParticleKind.SUN_MOTES
        CinematicSceneType.DEV_TERMINAL,
        CinematicSceneType.NEON_METROPOLIS,
        CinematicSceneType.CYBER_NEON,
        CinematicSceneType.TITANIUM_MECH,
        CinematicSceneType.SHADOW_SAMURAI,
        CinematicSceneType.THUNDER_APEX -> if (lightEnabled && !isDark) GyroParticleKind.SPARKLES else GyroParticleKind.COSMIC_DUST
    }
}

/**
 * Pure physics integrator combining physical angular rotation (gyroscope rad/s)
 * and gravity inclination with smooth damping so parallax depth feels responsive
 * without runaway drift.
 */
fun stepGyroTiltFromSensors(
    current: GyroTiltState,
    gyroPitchRate: Float,
    gyroRollRate: Float,
    gravityX: Float,
    gravityY: Float,
    dtSeconds: Float
): GyroTiltState {
    val clampedDt = dtSeconds.coerceIn(0f, 0.1f)
    // Gyro angular impulse + gravity tilt baseline (-1..1)
    val gravTargetX = (-gravityX / 9.81f).coerceIn(-1f, 1f) * 0.65f
    val gravTargetY = ((gravityY - 4.5f) / 9.81f).coerceIn(-1f, 1f) * 0.65f
    val blendedX = current.x + (gyroRollRate * clampedDt * 0.42f) + (gravTargetX - current.x) * (clampedDt * 2.8f)
    val blendedY = current.y + (gyroPitchRate * clampedDt * 0.42f) + (gravTargetY - current.y) * (clampedDt * 2.8f)
    return GyroTiltState(
        x = blendedX.coerceIn(-1.25f, 1.25f),
        y = blendedY.coerceIn(-1.25f, 1.25f)
    )
}

fun computeParallaxOffset(
    tilt: GyroTiltState,
    depth: Float,
    maxShiftPx: Float
): Offset = Offset(
    x = tilt.x * depth * maxShiftPx,
    y = tilt.y * depth * maxShiftPx
)

@Composable
fun rememberGyroTiltState(isReducedMotion: Boolean): GyroTiltState {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(GyroTiltState()) }

    DisposableEffect(context, isReducedMotion) {
        if (isReducedMotion) {
            tilt = GyroTiltState()
            return@DisposableEffect onDispose {}
        }

        val sensorManager = runCatching {
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }.getOrNull()
        val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || (gyroSensor == null && gravitySensor == null)) {
            return@DisposableEffect onDispose {}
        }

        var lastTimestampNs = 0L
        var lastGravX = 0f
        var lastGravY = 4.5f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                        lastGravX = event.values.getOrElse(0) { 0f }
                        lastGravY = event.values.getOrElse(1) { 4.5f }
                        if (gyroSensor == null) {
                            tilt = stepGyroTiltFromSensors(
                                current = tilt,
                                gyroPitchRate = 0f,
                                gyroRollRate = 0f,
                                gravityX = lastGravX,
                                gravityY = lastGravY,
                                dtSeconds = 0.032f
                            )
                        }
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        val dt = if (lastTimestampNs == 0L) {
                            0.016f
                        } else {
                            ((event.timestamp - lastTimestampNs) / 1_000_000_000f).coerceIn(0.002f, 0.08f)
                        }
                        lastTimestampNs = event.timestamp
                        val pitchRate = event.values.getOrElse(0) { 0f }
                        val rollRate = event.values.getOrElse(1) { 0f }
                        tilt = stepGyroTiltFromSensors(
                            current = tilt,
                            gyroPitchRate = pitchRate,
                            gyroRollRate = rollRate,
                            gravityX = lastGravX,
                            gravityY = lastGravY,
                            dtSeconds = dt
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        runCatching {
            gyroSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
            gravitySensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            runCatching { sensorManager.unregisterListener(listener) }
        }
    }

    return tilt
}

@Composable
fun GyroParticleOverlay(
    particleKind: GyroParticleKind,
    accentColor: Color,
    lightColor: Color,
    isDark: Boolean,
    intensity: Float = 1f,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (intensity <= 0.02f) return

    val tilt = rememberGyroTiltState(isReducedMotion = isReducedMotion)
    val transition = rememberInfiniteTransition(label = "gyro_particles")
    val driftPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_drift_phase"
    )
    val twinklePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_twinkle_phase"
    )

    val seeds = remember(particleKind) {
        val count = when (particleKind) {
            GyroParticleKind.SNOW_FROST -> 54
            GyroParticleKind.SAKURA_PETALS -> 32
            GyroParticleKind.CAMPFIRE_EMBERS -> 38
            else -> 42
        }
        val list = ArrayList<GyroParticleSeed>(count)
        var s = particleKind.ordinal * 4871 + 9187
        for (i in 0 until count) {
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val bx = (s % 1000) / 1000f
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val by = (s % 1000) / 1000f
            val layer = i % 3
            val depth = when (layer) {
                0 -> 0.36f
                1 -> 0.72f
                else -> 1.18f
            }
            s = (s * 1103515245 + 12345) and 0x7fffffff
            val weight = 0.75f + ((s % 100) / 100f) * 0.85f
            list.add(
                GyroParticleSeed(
                    baseX = bx,
                    baseY = by,
                    speed = 0.65f + (layer * 0.45f) + (weight * 0.3f),
                    depth = depth,
                    sizeWeight = weight,
                    phaseOffset = (i * 0.137f) % 1f,
                    driftAngle = ((i * 53) % 360) * (PI.toFloat() / 180f)
                )
            )
        }
        list
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val maxShiftPx = size.minDimension * 0.085f
        val clampedIntensity = intensity.coerceIn(0f, 1.25f)

        seeds.forEachIndexed { idx, p ->
            val parallax = computeParallaxOffset(tilt, p.depth, maxShiftPx)
            val t = (driftPhase * p.speed + p.phaseOffset) % 1f
            val sway = sin((driftPhase * 2f * PI.toFloat()) + idx * 0.7f) * (14.dp.toPx() * p.depth)

            val rawX: Float
            val rawY: Float
            when (particleKind) {
                GyroParticleKind.SNOW_FROST -> {
                    rawX = ((p.baseX * w) + sway * 1.4f + t * w * 0.10f + parallax.x + w) % w
                    rawY = ((p.baseY * h) + t * h + parallax.y + h) % h
                }
                GyroParticleKind.CAMPFIRE_EMBERS, GyroParticleKind.WATER_BUBBLES -> {
                    rawX = ((p.baseX * w) + sway * 1.1f + parallax.x + w) % w
                    rawY = ((p.baseY * h) - t * h + parallax.y + 2f * h) % h
                }
                GyroParticleKind.SAKURA_PETALS -> {
                    rawX = ((p.baseX * w) + t * w * 0.28f + sway + parallax.x + w) % w
                    rawY = ((p.baseY * h) + t * h + parallax.y + h) % h
                }
                else -> {
                    rawX = ((p.baseX * w) + sway * 1.25f + parallax.x + w) % w
                    val vert = cos((driftPhase * 2f * PI.toFloat()) + p.driftAngle) * 16.dp.toPx() * p.depth
                    rawY = ((p.baseY * h) + vert + parallax.y + h) % h
                }
            }

            val center = Offset(rawX, rawY)
            val twinkle = 0.55f + 0.45f * sin(twinklePhase * PI.toFloat() + idx).let { it * it }
            val alpha = ((0.22f + p.depth * 0.32f) * twinkle * clampedIntensity).coerceIn(0.05f, 0.88f)
            val r = (1.5f + p.depth * 1.8f * p.sizeWeight).dp.toPx()

            drawSingleGyroParticle(
                kind = particleKind,
                center = center,
                radius = r,
                alpha = alpha,
                rotationDeg = (driftPhase * 360f * p.speed + idx * 31f) % 360f,
                accentColor = accentColor,
                lightColor = lightColor,
                isDark = isDark
            )
        }
    }
}

private fun DrawScope.drawSingleGyroParticle(
    kind: GyroParticleKind,
    center: Offset,
    radius: Float,
    alpha: Float,
    rotationDeg: Float,
    accentColor: Color,
    lightColor: Color,
    isDark: Boolean
) {
    when (kind) {
        GyroParticleKind.SNOW_FROST -> {
            val snowTint = if (isDark) Color(0xFFE6F6FF) else Color.White
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(snowTint.copy(alpha = alpha * 0.92f), lightColor.copy(alpha = alpha * 0.35f), Color.Transparent),
                    center = center,
                    radius = radius * 2.1f
                ),
                radius = radius * 2.1f,
                center = center
            )
            drawCircle(color = snowTint.copy(alpha = alpha), radius = radius * 0.8f, center = center)
        }
        GyroParticleKind.SAKURA_PETALS -> {
            rotate(rotationDeg, pivot = center) {
                drawOval(
                    color = accentColor.copy(alpha = alpha * 0.85f),
                    topLeft = Offset(center.x - radius * 1.3f, center.y - radius * 0.72f),
                    size = Size(radius * 2.6f, radius * 1.44f)
                )
            }
        }
        GyroParticleKind.CAMPFIRE_EMBERS -> {
            val ember = Color(0xFFFF9838)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEE9C).copy(alpha = alpha), ember.copy(alpha = alpha * 0.55f), Color.Transparent),
                    center = center,
                    radius = radius * 2.3f
                ),
                radius = radius * 2.3f,
                center = center
            )
            drawCircle(color = Color(0xFFFFF3B8).copy(alpha = alpha), radius = radius * 0.68f, center = center)
        }
        GyroParticleKind.FIREFLIES -> {
            val glowCol = Color(0xFFBFFFA0)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowCol.copy(alpha = alpha), accentColor.copy(alpha = alpha * 0.45f), Color.Transparent),
                    center = center,
                    radius = radius * 2.8f
                ),
                radius = radius * 2.8f,
                center = center
            )
            drawCircle(color = Color.White.copy(alpha = alpha), radius = radius * 0.55f, center = center)
        }
        GyroParticleKind.WATER_BUBBLES -> {
            drawCircle(
                color = lightColor.copy(alpha = alpha * 0.55f),
                radius = radius * 1.25f,
                center = center,
                style = Stroke(width = (radius * 0.28f).coerceAtLeast(1f))
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.75f),
                radius = radius * 0.28f,
                center = Offset(center.x - radius * 0.42f, center.y - radius * 0.42f)
            )
        }
        GyroParticleKind.SPARKLES -> {
            rotate(rotationDeg * 0.5f, pivot = center) {
                drawLine(
                    color = lightColor.copy(alpha = alpha),
                    start = Offset(center.x - radius * 1.5f, center.y),
                    end = Offset(center.x + radius * 1.5f, center.y),
                    strokeWidth = (radius * 0.35f).coerceAtLeast(1f)
                )
                drawLine(
                    color = accentColor.copy(alpha = alpha),
                    start = Offset(center.x, center.y - radius * 1.5f),
                    end = Offset(center.x, center.y + radius * 1.5f),
                    strokeWidth = (radius * 0.35f).coerceAtLeast(1f)
                )
            }
        }
        GyroParticleKind.COSMIC_DUST, GyroParticleKind.SUN_MOTES -> {
            val tint = if (kind == GyroParticleKind.SUN_MOTES && !isDark) {
                lightColor
            } else {
                lightColor.copy(alpha = 0.9f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = alpha * 0.78f), accentColor.copy(alpha = alpha * 0.24f), Color.Transparent),
                    center = center,
                    radius = radius * 2.2f
                ),
                radius = radius * 2.2f,
                center = center
            )
            drawCircle(color = tint.copy(alpha = alpha), radius = radius * 0.7f, center = center)
        }
    }
}
