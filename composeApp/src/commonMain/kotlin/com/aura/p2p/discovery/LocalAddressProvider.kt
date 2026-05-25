package com.aura.p2p.discovery

/**
 * The device's routable IPv4, used to build the dialable multiaddr advertised in
 * a PresenceRecord. go-libp2p can't self-detect its address on Android (SELinux
 * blocks the netlink enumeration it uses, b/155595000), so this is read from the
 * platform instead (Android: ConnectivityManager via DeviceNet). Returns null
 * when no routable IPv4 is available.
 */
interface LocalAddressProvider {
    suspend fun localIpv4(): String?
}
