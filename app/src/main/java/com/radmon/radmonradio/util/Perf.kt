/**
 * Performance monitoring utilities.
 */
package com.radmon.radmonradio.util
import android.app.ActivityManager
import android.content.Context
enum class Tier { LOW, MID, HIGH }
object Perf {
    @Volatile
    var tier: Tier = Tier.HIGH
        private set
    fun init(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isLowRam = am.isLowRamDevice
            val mem = am.memoryClass 
            val cores = Runtime.getRuntime().availableProcessors()
            tier = when {
                isLowRam || cores <= 4 || mem <= 256 -> Tier.LOW
                cores <= 6 || mem <= 512 -> Tier.MID
                else -> Tier.HIGH
            }
        }
    }
    val particleMax: Int
        get() = when (tier) {
            Tier.LOW -> 32
            Tier.MID -> 64
            Tier.HIGH -> 96
        }
    val baseBurstCount: Int
        get() = when (tier) {
            Tier.LOW -> 10
            Tier.MID -> 20
            Tier.HIGH -> 28
        }
    val blurRadiusDp: Int
        get() = when (tier) {
            Tier.LOW -> 40
            Tier.MID -> 72
            Tier.HIGH -> 100
        }
}
