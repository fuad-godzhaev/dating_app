package fyp.project.datingapp.p2p.relay

/**
 * Small epoch-milliseconds clock abstraction. Production code passes
 * [SystemClock]; tests inject a mutable implementation to time-travel
 * through TTL boundaries without Thread.sleep.
 */
fun interface EpochClock {
    fun nowMs(): Long
}

/** Real wall-clock source backed by kotlinx-datetime. */
object SystemClock : EpochClock {
    override fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}
