package com.aura.p2p.background

/**
 * Adaptive serve-window cadence (sim-results phase2): run serve windows frequently during the
 * evening activity peak (when sends concentrate) and relaxed off-peak, instead of a fixed 15-min
 * cadence. Measured effect: ~9%/day vs ~13%/day at similar delivery latency. Pure so it is unit
 * tested; the Android scheduler reads [delayMinutesForHour] to set the next one-time serve window.
 */
object ServeCadence {
    /** Local-time hours treated as the activity peak (evening). Matches the behavioural model. */
    val PEAK_HOURS: IntRange = 18..23

    const val PEAK_CADENCE_MIN: Long = 15
    const val OFF_PEAK_CADENCE_MIN: Long = 45

    /** Minutes until the next serve window given the current local hour-of-day (0..23). */
    fun delayMinutesForHour(hour: Int): Long =
        if (((hour % 24) + 24) % 24 in PEAK_HOURS) PEAK_CADENCE_MIN else OFF_PEAK_CADENCE_MIN
}
