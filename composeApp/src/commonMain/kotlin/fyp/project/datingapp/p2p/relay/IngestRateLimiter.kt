package fyp.project.datingapp.p2p.relay

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dual token-bucket limiter for [RelayPolicy] ingest.
 *
 * Two independent buckets are consulted on every `tryConsume`:
 *   - Minute bucket: [perMinute] tokens refilled proportionally at
 *     [perMinute] / 60_000 ms. Catches bursty machine-speed ingestion.
 *   - Day bucket: [perDay] tokens refilled proportionally at
 *     [perDay] / 86_400_000 ms. Caps the long-run throughput regardless of
 *     burst pattern.
 *
 * Both buckets start full. An ingest succeeds only when both have ≥ 1
 * token available; otherwise the call returns `false` and no tokens move.
 * This avoids the "double-debit" pitfall where the minute bucket succeeds
 * and the day bucket rejects, leaving the minute bucket below where a user
 * would expect after a failed call.
 *
 * Thread safety: a single [Mutex] guards both buckets' refill + consume
 * sequence so concurrent callers cannot race past each other's decrements.
 */
class IngestRateLimiter(
    private val perMinute: Int = DEFAULT_PER_MINUTE,
    private val perDay: Int = DEFAULT_PER_DAY,
    private val clock: EpochClock = SystemClock,
) {
    private val mutex = Mutex()

    private var minuteTokens: Double = perMinute.toDouble()
    private var dayTokens: Double = perDay.toDouble()
    private var lastRefillMs: Long = clock.nowMs()

    suspend fun tryConsume(): Boolean = mutex.withLock {
        refill()
        if (minuteTokens >= 1.0 && dayTokens >= 1.0) {
            minuteTokens -= 1.0
            dayTokens -= 1.0
            true
        } else {
            false
        }
    }

    /** Exposed for diagnostics/tests only. */
    suspend fun snapshot(): Snapshot = mutex.withLock {
        refill()
        Snapshot(minuteTokens, dayTokens)
    }

    private fun refill() {
        val now = clock.nowMs()
        val elapsed = now - lastRefillMs
        if (elapsed <= 0) return
        lastRefillMs = now
        val minuteRate = perMinute / 60_000.0
        val dayRate = perDay / 86_400_000.0
        minuteTokens = minOf(perMinute.toDouble(), minuteTokens + elapsed * minuteRate)
        dayTokens = minOf(perDay.toDouble(), dayTokens + elapsed * dayRate)
    }

    data class Snapshot(val minuteTokens: Double, val dayTokens: Double)

    companion object {
        const val DEFAULT_PER_MINUTE: Int = 5
        const val DEFAULT_PER_DAY: Int = 500
    }
}
