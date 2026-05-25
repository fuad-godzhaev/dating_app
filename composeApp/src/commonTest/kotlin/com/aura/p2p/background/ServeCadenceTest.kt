package com.aura.p2p.background

import kotlin.test.Test
import kotlin.test.assertEquals

class ServeCadenceTest {
    @Test
    fun peakHoursUseFifteenMinutes() {
        for (h in 18..23) assertEquals(15L, ServeCadence.delayMinutesForHour(h))
    }

    @Test
    fun offPeakHoursUseFortyFiveMinutes() {
        for (h in listOf(0, 6, 12, 17)) assertEquals(45L, ServeCadence.delayMinutesForHour(h))
    }

    @Test
    fun normalisesOutOfRangeHours() {
        assertEquals(ServeCadence.delayMinutesForHour(20), ServeCadence.delayMinutesForHour(44)) // 44%24=20
    }
}
