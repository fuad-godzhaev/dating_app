package fyp.project.datingapp.p2p.ble

import kotlinx.coroutines.flow.Flow

/**
 * Opt-in, short-range Bluetooth LE proximity discovery (ADR-0002). A separate
 * subsystem from the DHT/GossipSub discovery path: it is **excluded from
 * cold-start**, has **no mesh**, and runs only while the user explicitly toggles
 * it on. It advertises a tiny beacon (truncated DID + PeerId) under a fixed app
 * service UUID and scans for the same, surfacing nearby peers.
 *
 * Privacy: BLE advertising is observable by any nearby scanner. The beacon
 * carries only truncated identifiers, never a precise location. A full identity
 * exchange (the untruncated DID + dialable multiaddrs needed to actually connect)
 * is left to a future GATT handshake — TODO below.
 */
interface BleProximity {

    /** True if this device has a usable BLE radio and the APIs are available. */
    val isSupported: Boolean

    /**
     * Start advertising [beacon] and scanning for nearby peers. Collecting the
     * returned flow begins scanning; cancelling it (and/or calling [stop]) tears
     * both down. Re-collecting restarts. Emits one [BleSighting] per scan result.
     */
    fun start(beacon: BleBeacon): Flow<BleSighting>

    /** Stop advertising and scanning. Idempotent. */
    fun stop()
}

/**
 * The compact payload advertised over BLE. Both fields are short suffixes (not
 * the full identifiers) to fit the ~31-byte legacy advertising budget; they are
 * a proximity hint, not enough to dial. [didSuffix] lets a scanner recognise a
 * peer it already knows from the network; [peerIdSuffix] disambiguates devices.
 */
data class BleBeacon(
    val didSuffix: String,
    val peerIdSuffix: String,
) {
    companion object {
        const val SUFFIX_LEN = 8
        fun of(did: String, peerId: String): BleBeacon =
            BleBeacon(did.takeLast(SUFFIX_LEN), peerId.takeLast(SUFFIX_LEN))
    }
}

/** A nearby peer seen over BLE. [rssi] is signal strength (proximity proxy). */
data class BleSighting(
    val deviceAddress: String,
    val didSuffix: String?,
    val peerIdSuffix: String?,
    val rssi: Int,
)
