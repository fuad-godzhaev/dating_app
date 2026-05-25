package com.aura.p2p.relay

import kotlin.random.Random

/**
 * One-shot interaction tokens that gate [RelayPolicy.put] ingestion.
 *
 * The Home feature issues a fresh token via [issue] at the moment a profile
 * card is actually rendered to the user (hook point: `Msg.PictureLoaded`),
 * and passes it to `RelayPolicy.put(envelope, token)`. Tokens expire after
 * [ttlMs] and are single-use — consumption deletes the token so a leaked or
 * replayed token cannot populate the cache twice.
 *
 * A background bot that never renders anything cannot mint tokens on its
 * own: the token-issuing hook lives inside the UI layer, not the
 * transport.
 *
 * In-memory only. A fresh process starts with no tokens, matching the
 * design intent that only *this run's* rendered cards may be cached.
 */
class SessionInteractionTokens(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val clock: EpochClock = SystemClock,
    private val random: Random = Random.Default,
) {
    private data class Entry(val expiresAt: Long)

    // The token map is only ever touched from the UI thread (issue on
    // `Msg.PictureLoaded`; consume inside `RelayPolicy.put`, which we call
    // from the same MVIKotlin executor). Kotlin/Native doesn't expose
    // `java.util.concurrent` so we deliberately skip locking — if we ever
    // need concurrent access, switch to `kotlinx.atomicfu.locks.synchronized`.
    private val tokens = mutableMapOf<String, Entry>()

    /** Mint a fresh token. Caller must feed this verbatim to [RelayPolicy.put]. */
    fun issue(): String {
        val token = generateToken()
        purgeExpired()
        tokens[token] = Entry(clock.nowMs() + ttlMs)
        return token
    }

    /**
     * Attempt to consume [token]. Returns true exactly once per issuance, if
     * the token is known and within its TTL. Subsequent calls (and calls
     * with unknown tokens) return false.
     */
    fun consume(token: String?): Boolean {
        if (token == null) return false
        purgeExpired()
        val entry = tokens.remove(token) ?: return false
        return clock.nowMs() < entry.expiresAt
    }

    /** Count of tokens that haven't yet been consumed or expired. Tests use this. */
    fun outstandingCount(): Int {
        purgeExpired()
        return tokens.size
    }

    private fun purgeExpired() {
        val now = clock.nowMs()
        val iter = tokens.iterator()
        while (iter.hasNext()) {
            if (iter.next().value.expiresAt <= now) iter.remove()
        }
    }

    private fun generateToken(): String {
        // 16 random bytes rendered as hex — 128 bits of entropy is well past
        // what any realistic adversary can brute-force in the 30-s TTL.
        val bytes = ByteArray(16).also { random.nextBytes(it) }
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    companion object {
        /** 30 seconds. Long enough for any real UI render, short enough to cap replay. */
        const val DEFAULT_TTL_MS: Long = 30_000L
    }
}
