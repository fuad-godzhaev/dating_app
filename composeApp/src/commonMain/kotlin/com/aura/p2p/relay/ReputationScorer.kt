package com.aura.p2p.relay

/**
 * Provides the current relay-cache capacity.
 *
 * Phase A deliberately stubs this to return a constant 20. The full
 * Newcomer → Veteran tier ladder (5 / 15 / 30 / 50 based on account age
 * and distinct-peer serve attestations) is described in
 * `relay-architecture.md` §3.7 and is deferred — the plan document lists
 * reputation under "deferred / hooks exist for upgrading later".
 *
 * Kept as an interface so the upgrade is a DI-graph swap rather than a
 * surgery on [RelayPolicy].
 */
interface ReputationScorer {
    /** Max number of relay-cached rows the local device may hold. */
    suspend fun currentCapacity(): Int
}

/**
 * Newcomer-tier stub: always returns 20 (the Phase A fixed cap from
 * `p2p-subsystem-design.md` §4 non-goals).
 */
class FixedCapacityScorer(private val capacity: Int = DEFAULT_CAPACITY) : ReputationScorer {
    override suspend fun currentCapacity(): Int = capacity

    companion object {
        const val DEFAULT_CAPACITY: Int = 20
    }
}
