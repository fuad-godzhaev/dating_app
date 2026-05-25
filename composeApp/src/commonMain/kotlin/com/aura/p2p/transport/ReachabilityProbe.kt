package com.aura.p2p.transport

/** What AutoNAT believes about this node's inbound reachability. */
enum class Reachability { REACHABLE, UNREACHABLE, UNKNOWN }

/**
 * Idea C (sim-results phase2): the opt-in reachable role (mailbox holder / relay coordinator) is
 * only USEFUL when the node is actually inbound-reachable (public IPv4, public/IPv6, or cone NAT) -
 * a node behind CGNAT cannot be a holder or a relay server no matter how long it stays online, so
 * keeping a foreground service up there only burns battery and the Android-15 6 h FGS budget. This
 * probe gates the role on reachability (NOT on Wi-Fi vs cellular - reachability, not network type).
 *
 * The production signal is libp2p AutoNAT (exposed from the Go host as `reachability()` - see the
 * Go changes + INTEGRATION.md; the gomobile `.aar` must be rebuilt to surface it). Until that lands,
 * the platform probes return [Reachability.UNKNOWN], which [permitsServing] treats as "proceed" so
 * behaviour is unchanged - the gate only ever PAUSES a node once AutoNAT can prove it UNREACHABLE.
 */
interface ReachabilityProbe {
    suspend fun current(): Reachability
}

/** Idea C policy: serve unless we KNOW we are unreachable (UNKNOWN proceeds, fail-open). */
fun Reachability.permitsServing(): Boolean = this != Reachability.UNREACHABLE
