package com.aura.p2p.discovery

import com.aura.domain.location.LocationProvider
import kotlin.concurrent.Volatile

/**
 * Resolves the user's current geohash cell from [LocationProvider], with a
 * **debug override** so the two-emulator discovery checkpoint is deterministic
 * (emulators have no real fix, and we want both peers in the same cell). When an
 * override is set it short-circuits location entirely.
 *
 * [currentGeohash] returns the full-precision geohash (carried in the
 * PresenceRecord for distance estimation); [currentGeohash5] returns the ~5 km
 * discovery cell used for DHT keys and GossipSub topics.
 */
class GeohashLocator(
    private val locationProvider: LocationProvider,
    private val precision: Int = 9,
) {
    @Volatile
    private var override: String? = null

    /** Force a fixed geohash (debug / tests). Pass null to clear and use real location. */
    fun setDebugGeohash(geohash: String?) {
        require(geohash == null || geohash.length >= Geohash.GEOHASH5_LEN) {
            "debug geohash must be at least ${Geohash.GEOHASH5_LEN} chars"
        }
        override = geohash
    }

    /** Full-precision geohash for the current location, or null if unavailable. */
    suspend fun currentGeohash(): String? {
        override?.let { return it }
        val c = locationProvider.currentCoarse() ?: return null
        return Geohash.encode(c.latitude, c.longitude, precision)
    }

    /** The ~5 km discovery cell, or null if location is unavailable. */
    suspend fun currentGeohash5(): String? = currentGeohash()?.let { Geohash.geohash5(it) }
}
