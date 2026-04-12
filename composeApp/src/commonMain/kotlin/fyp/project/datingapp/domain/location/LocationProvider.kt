package fyp.project.datingapp.domain.location

/** A coarse WGS84 coordinate. Precision is intentionally city-block level. */
data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Coarse device location for discovery (ADR-0002: the geohash cell a peer
 * announces under). Deliberately coarse — the app never needs or stores a
 * precise fix. Returns null when location is unavailable or permission was
 * denied; callers degrade gracefully (no announce / no discovery) rather than
 * prompting again.
 *
 * Android backs this with the AOSP `LocationManager` (no Google Play Services,
 * per the locked Phase C decision). iOS is a stub until it leaves the lazy state.
 */
interface LocationProvider {
    suspend fun currentCoarse(): Coordinates?
}
