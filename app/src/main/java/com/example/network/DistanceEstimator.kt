package com.example.network

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Log-Distance Path Loss Model for RF / BLE distance estimation.
 * Formula: RSSI = -10 * n * log10(d) + A
 * Solved:  d = 10 ^ ((A - RSSI) / (10 * n))
 */
object DistanceEstimator {
    // Nominal reference RSSI at 1 meter in dBm
    private const val RSSI_AT_ONE_METER = -59.0

    // Path loss exponent (2.0 line of sight, 2.4-2.8 indoor/mixed obstacle terrain)
    private const val PATH_LOSS_EXPONENT = 2.4

    /**
     * Calculates estimated distance in meters based on RSSI value.
     * Returns a float clamped between 0.5 meters and 90 meters.
     */
    fun estimateMeters(rssi: Int): Float {
        if (rssi >= 0 || rssi < -110) return 1.0f
        val ratio = (RSSI_AT_ONE_METER - rssi) / (10.0 * PATH_LOSS_EXPONENT)
        val rawDistance = 10.0.pow(ratio)
        // Clamp sensibly to realistic mesh peer distance and round to 1 decimal place
        val clamped = rawDistance.coerceIn(0.5, 90.0)
        return ((clamped * 10).roundToInt() / 10.0).toFloat()
    }

    /**
     * Formats distance in clear Arabic with unit.
     */
    fun formatArabic(meters: Float): String {
        val rounded = meters.roundToInt()
        return when {
            rounded <= 1 -> "حوالي متر واحد (~1 م)"
            rounded == 2 -> "حوالي مترين (~2 م)"
            rounded in 3..10 -> "حوالي $rounded أمتار (~$rounded م)"
            else -> "حوالي $rounded متراً (~$rounded م)"
        }
    }
}
