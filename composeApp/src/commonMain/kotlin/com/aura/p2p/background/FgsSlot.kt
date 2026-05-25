package com.aura.p2p.background

/**
 * Deterministic per-cell rank slotting for the opt-in "Stay online" reachable role (Idea D from
 * sim-results phase2). Instead of every FGS node holding its ~6 h reachable window at the same time
 * (evening peak), nodes in a geohash cell tile the 24 h by RANK so at least one is up at any moment
 * without all being up at once: shift length `= min(6 h, 24 h / R)` and node `i` starts at
 * `i * 24 h / R`. This de-concentrates holder/relay load and spreads coverage across the day at no
 * extra per-node battery (each node still serves only its one slot).
 *
 * Pure + platform-independent so it is unit-tested without Android. The Android service supplies the
 * roster size + this node's rank (from the cell's FGS peer set when known) or falls back to
 * [rankFromId] / a default roster size when the cell roster is not yet discovered.
 */
object FgsSlot {
    const val DAY_MIN: Int = 24 * 60
    /** Android 15+ dataSync FGS runtime cap (~6 h / 24 h); a slot can never exceed it. */
    const val MAX_SLOT_MIN: Int = 6 * 60

    /** Shift length in minutes for a roster of [rosterSize] nodes: `min(6 h, 24 h / R)`. */
    fun slotLengthMin(rosterSize: Int): Int {
        val r = rosterSize.coerceAtLeast(1)
        return minOf(MAX_SLOT_MIN, DAY_MIN / r)
    }

    /** This node's daily shift as `[startMinuteOfDay, lengthMin]` given its [rank] in `[0,R)`. */
    fun slot(rank: Int, rosterSize: Int): IntArray {
        val r = rosterSize.coerceAtLeast(1)
        val i = ((rank % r) + r) % r
        val start = (i.toLong() * DAY_MIN / r).toInt() % DAY_MIN
        return intArrayOf(start, slotLengthMin(r))
    }

    /** Whether [nowMinuteOfDay] (0..1439) falls inside the slot `[start, start+len)`, with wrap. */
    fun isActiveAt(nowMinuteOfDay: Int, start: Int, lengthMin: Int): Boolean {
        val now = ((nowMinuteOfDay % DAY_MIN) + DAY_MIN) % DAY_MIN
        val end = start + lengthMin
        return if (end <= DAY_MIN) {
            now in start until end
        } else {
            now >= start || now < (end - DAY_MIN) // wraps past midnight
        }
    }

    /**
     * Minutes from [nowMinuteOfDay] until this slot next becomes active (0 if active now). Lets the
     * Android scheduler set an exact start alarm instead of polling.
     */
    fun minutesUntilActive(nowMinuteOfDay: Int, start: Int, lengthMin: Int): Int {
        if (isActiveAt(nowMinuteOfDay, start, lengthMin)) return 0
        val now = ((nowMinuteOfDay % DAY_MIN) + DAY_MIN) % DAY_MIN
        return ((start - now) % DAY_MIN + DAY_MIN) % DAY_MIN
    }

    /**
     * Stable deterministic rank in `[0, rosterSize)` derived from a node identifier (peerId / DID),
     * for when the actual cell roster ordering is not known. FNV-1a (32-bit) so it is identical on
     * every platform (unlike String.hashCode). Gives a spread-out shift without any coordination.
     */
    fun rankFromId(id: String, rosterSize: Int): Int {
        val r = rosterSize.coerceAtLeast(1)
        var hash = 0x811c9dc5.toInt() // FNV offset basis
        for (b in id.encodeToByteArray()) {
            hash = hash xor (b.toInt() and 0xff)
            hash *= 0x01000193 // FNV prime
        }
        return ((hash % r) + r) % r
    }
}
