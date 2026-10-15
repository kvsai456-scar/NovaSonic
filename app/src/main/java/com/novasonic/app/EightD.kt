package com.novasonic.app

/** Shared constants for the 8D audio effect (app <-> playback service protocol). */
object EightD {
    const val ACTION = "com.novasonic.app.SET_8D"
    const val KEY_ENABLED = "enabled"
    const val KEY_MODE = "mode"
    const val KEY_INTENSITY = "intensity"
    const val KEY_PERIOD = "period"

    const val MODE_ORBIT = 0   // "8D": the sound circles around your head
    const val MODE_SWEEP = 1   // "3D": the sound swings across the front
    const val MODE_SPATIAL = 2 // "Spatial": static wide, roomy stage
}

data class EightDState(
    val enabled: Boolean = false,
    val mode: Int = EightD.MODE_ORBIT,
    val intensity: Float = 0.85f, // 0..1
    val speed: Float = 0.7f       // 0..1 -> one lap every 20s .. 4s
) {
    val periodSeconds: Float get() = 20f - 16f * speed
}
