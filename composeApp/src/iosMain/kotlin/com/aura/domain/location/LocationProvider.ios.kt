package com.aura.domain.location

/**
 * iOS stub (lazy state). TODO(iOS): back this with `CLLocationManager` using
 * `kCLLocationAccuracyReduced` (coarse) and `requestWhenInUseAuthorization`,
 * bridging the delegate callback to a coroutine. Returns null until then.
 */
class IosLocationProvider : LocationProvider {
    override suspend fun currentCoarse(): Coordinates? = null
}
