package com.aura.p2p.relay

import com.aura.database.appView.entities.PeerProfileEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SweepJobTest {

    private class MutableClock(var t: Long = 0) : EpochClock {
        override fun nowMs(): Long = t
    }

    private fun row(did: String, expiresAt: Long?) = PeerProfileEntity(
        did = did,
        displayName = did,
        profileCid = "cid-$did",
        commitCid = "cid-$did",
        commitSignature = ByteArray(0),
        signingKey = ByteArray(0),
        receivedAt = 0,
        lastUpdatedAt = 0,
        lastSeenAt = 0,
        ownerDid = did,
        cachedAt = 0,
        expiresAt = expiresAt,
    )

    @Test
    fun sweepDeletesOnlyExpiredRows() = runTest {
        val dao = FakeDiscoveryDao()
        val clock = MutableClock(t = 1_000)
        val sweep = SweepJob(dao, clock)

        dao.upsertProfile(row("alice", expiresAt = 500))   // expired
        dao.upsertProfile(row("bob", expiresAt = 2_000))   // fresh
        dao.upsertProfile(row("carol", expiresAt = null))  // discovery-only, never expires

        val removed = sweep.runOnce()
        assertEquals(1, removed)

        assertNull(dao.getProfileByDid("alice"))
        assertNotNull(dao.getProfileByDid("bob"))
        assertNotNull(dao.getProfileByDid("carol"))
    }

    @Test
    fun sweepIsNoOpWhenNothingExpired() = runTest {
        val dao = FakeDiscoveryDao()
        val clock = MutableClock(t = 0)
        val sweep = SweepJob(dao, clock)
        dao.upsertProfile(row("alice", expiresAt = 10_000))
        assertEquals(0, sweep.runOnce())
        assertNotNull(dao.getProfileByDid("alice"))
    }
}
