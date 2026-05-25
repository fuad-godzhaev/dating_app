package com.aura.p2p.transport

/**
 * Android [ReachabilityProbe]: reads libp2p AutoNAT's verdict from the gomobile host (Idea C) and
 * maps Public/Private/Unknown -> REACHABLE/UNREACHABLE/UNKNOWN. No Wi-Fi gating - it gates on actual
 * inbound reachability, so a cone/IPv6 cellular node serves and a CGNAT node (Wi-Fi or cellular)
 * pauses. UNKNOWN is fail-open ([permitsServing]) so a node serves while AutoNAT is still deciding.
 */
class AndroidReachabilityProbe(
    private val transport: Transport,
) : ReachabilityProbe {
    override suspend fun current(): Reachability = when (transport.reachability()) {
        "Public" -> Reachability.REACHABLE
        "Private" -> Reachability.UNREACHABLE
        else -> Reachability.UNKNOWN
    }
}
