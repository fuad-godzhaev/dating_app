package fyp.project.datingapp.p2p.relay

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the dual token-bucket without wall-clock Thread.sleep — a
 * [MutableClock] advances virtual time so refill rates can be probed
 * deterministically.
 */
class IngestRateLimiterTest {

    private class MutableClock(var t: Long = 0) : EpochClock {
        override fun nowMs(): Long = t
    }

    @Test
    fun burstExhaustsMinuteBucket() = runTest {
        val clock = MutableClock()
        val limiter = IngestRateLimiter(perMinute = 5, perDay = 500, clock = clock)
        repeat(5) { assertTrue(limiter.tryConsume(), "call ${it + 1} should succeed") }
        assertFalse(limiter.tryConsume(), "6th call must be rejected")
    }

    @Test
    fun minuteBucketRefillsProportionally() = runTest {
        val clock = MutableClock()
        val limiter = IngestRateLimiter(perMinute = 60, perDay = 1_000_000, clock = clock)
        repeat(60) { limiter.tryConsume() }
        assertFalse(limiter.tryConsume())

        // 1 token per second → advance 10s to get 10 tokens back.
        clock.t += 10_000
        repeat(10) { assertTrue(limiter.tryConsume(), "refilled call ${it + 1} should succeed") }
        assertFalse(limiter.tryConsume())
    }

    @Test
    fun dayBucketCapsLongRunThroughput() = runTest {
        val clock = MutableClock()
        // per-minute is so high that the day bucket is the only real gate.
        val limiter = IngestRateLimiter(perMinute = 1_000_000, perDay = 3, clock = clock)
        assertTrue(limiter.tryConsume())
        assertTrue(limiter.tryConsume())
        assertTrue(limiter.tryConsume())
        assertFalse(limiter.tryConsume(), "day bucket exhausted")
    }

    @Test
    fun failedCallDoesNotDebitEitherBucket() = runTest {
        val clock = MutableClock()
        val limiter = IngestRateLimiter(perMinute = 1, perDay = 100, clock = clock)
        assertTrue(limiter.tryConsume())
        // minute bucket is empty; failure should leave day bucket untouched.
        assertFalse(limiter.tryConsume())

        // Advance past 60s → minute bucket refills 1 token.
        clock.t += 60_000
        assertTrue(limiter.tryConsume())

        val snap = limiter.snapshot()
        // Two successful calls → 98 day tokens left. The 60s clock advance
        // also refills the day bucket by perDay/86_400_000 * 60_000 ≈ 0.07,
        // so allow a small tolerance for that proportional refill.
        assertEquals(98.0, snap.dayTokens, 0.1)
    }

    private fun assertEquals(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "expected $expected ± $tolerance, got $actual"
        )
    }
}
