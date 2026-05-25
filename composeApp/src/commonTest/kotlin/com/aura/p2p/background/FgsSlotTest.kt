package com.aura.p2p.background

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Deterministic rank slotting for the reachable FGS role (Idea D). */
class FgsSlotTest {

    @Test
    fun slotLengthIsTwentyFourOverRCappedAtSixHours() {
        assertEquals(6 * 60, FgsSlot.slotLengthMin(1))   // 24h/1 capped at 6h
        assertEquals(6 * 60, FgsSlot.slotLengthMin(4))   // 24h/4 = 6h exactly
        assertEquals(24 * 60 / 6, FgsSlot.slotLengthMin(6)) // 24h/6 = 4h (< cap)
        assertEquals(24 * 60 / 12, FgsSlot.slotLengthMin(12)) // 2h
    }

    @Test
    fun ranksTileTheDayWithoutAllOverlapping() {
        // R=6 -> 6 shifts of 4h starting every 4h: 0,240,480,720,960,1200.
        val r = 6
        val starts = (0 until r).map { FgsSlot.slot(it, r)[0] }
        assertEquals(listOf(0, 240, 480, 720, 960, 1200), starts)
        // Every minute of the day is covered by exactly one shift (4h length, 4h spacing).
        for (minute in 0 until FgsSlot.DAY_MIN step 37) {
            val active = (0 until r).count { i ->
                val s = FgsSlot.slot(i, r); FgsSlot.isActiveAt(minute, s[0], s[1])
            }
            assertEquals(1, active, "minute $minute should be covered by exactly one shift")
        }
    }

    @Test
    fun denseRosterKeepsAtLeastOneUpButNotAll() {
        // R=12 -> 2h shifts every 2h: full coverage, only one up at a time.
        val r = 12
        for (minute in listOf(0, 59, 120, 725, 1439)) {
            val active = (0 until r).count { i ->
                val s = FgsSlot.slot(i, r); FgsSlot.isActiveAt(minute, s[0], s[1])
            }
            assertEquals(1, active)
        }
    }

    @Test
    fun slotWrapsPastMidnight() {
        // Last rank of R=5: start = 4*288 = 1152, len = min(6h, 24h/5=288) = 288 -> ends 1440 (no wrap).
        // Construct an explicit wrap: start 1300, len 300 -> covers 1300..1439 and 0..159.
        assertTrue(FgsSlot.isActiveAt(1400, 1300, 300))
        assertTrue(FgsSlot.isActiveAt(100, 1300, 300))
        assertFalse(FgsSlot.isActiveAt(200, 1300, 300))
    }

    @Test
    fun minutesUntilActiveIsZeroWhenActiveElsePositive() {
        val s = FgsSlot.slot(2, 6) // start 480, len 240 -> active 480..719
        assertEquals(0, FgsSlot.minutesUntilActive(500, s[0], s[1]))
        assertEquals(80, FgsSlot.minutesUntilActive(400, s[0], s[1])) // 480-400
        // After the slot today, waits until tomorrow's start.
        assertEquals(FgsSlot.DAY_MIN - 720 + 480, FgsSlot.minutesUntilActive(720, s[0], s[1]))
    }

    @Test
    fun rankFromIdIsStableAndInRange() {
        val a = FgsSlot.rankFromId("12D3KooWExamplePeerId", 6)
        val b = FgsSlot.rankFromId("12D3KooWExamplePeerId", 6)
        assertEquals(a, b)
        assertTrue(a in 0 until 6)
        assertTrue(FgsSlot.rankFromId("did:key:zABC", 1) == 0)
    }
}
