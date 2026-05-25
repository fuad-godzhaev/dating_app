package com.aura.p2p.relay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionInteractionTokensTest {

    private class MutableClock(var t: Long = 0) : EpochClock {
        override fun nowMs(): Long = t
    }

    @Test
    fun issuedTokenIsConsumableOnce() {
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = MutableClock(0))
        val t = tokens.issue()
        assertTrue(tokens.consume(t), "first consume succeeds")
        assertFalse(tokens.consume(t), "second consume fails — one-shot")
    }

    @Test
    fun expiredTokenIsRejected() {
        val clock = MutableClock(0)
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = clock)
        val t = tokens.issue()
        clock.t = 30_001L
        assertFalse(tokens.consume(t))
    }

    @Test
    fun unknownTokenIsRejected() {
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = MutableClock(0))
        assertFalse(tokens.consume("deadbeef"))
        assertFalse(tokens.consume(null))
    }

    @Test
    fun outstandingCountTracksIssueAndConsume() {
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = MutableClock(0))
        val a = tokens.issue()
        tokens.issue()
        assertEquals(2, tokens.outstandingCount())
        tokens.consume(a)
        assertEquals(1, tokens.outstandingCount())
    }

    @Test
    fun expirationPurgesLazily() {
        val clock = MutableClock(0)
        val tokens = SessionInteractionTokens(ttlMs = 10_000L, clock = clock)
        tokens.issue()
        tokens.issue()
        clock.t = 10_001L
        assertEquals(0, tokens.outstandingCount())
    }
}
