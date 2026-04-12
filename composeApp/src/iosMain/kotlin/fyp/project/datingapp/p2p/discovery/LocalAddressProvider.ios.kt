package fyp.project.datingapp.p2p.discovery

/**
 * iOS stub (lazy state). TODO(iOS): read the device IPv4 via `getifaddrs` or
 * Network framework `NWPath`. Returns null until then, so the iOS announcer
 * produces no dialable multiaddr.
 */
class IosLocalAddressProvider : LocalAddressProvider {
    override suspend fun localIpv4(): String? = null
}
