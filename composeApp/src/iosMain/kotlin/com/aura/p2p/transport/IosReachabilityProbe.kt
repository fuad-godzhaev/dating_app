package com.aura.p2p.transport

/** iOS [ReachabilityProbe]: fail-open stopgap until AutoNAT is surfaced (Aura is Android-primary). */
class IosReachabilityProbe : ReachabilityProbe {
    override suspend fun current(): Reachability = Reachability.UNKNOWN
}
