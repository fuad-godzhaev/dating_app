package com.aura.domain.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

/**
 * Coarse location via the AOSP [LocationManager] (no Google Play Services).
 * Reads the freshest last-known fix across the network / passive / GPS
 * providers. Returns null if [Manifest.permission.ACCESS_COARSE_LOCATION] was
 * not granted (the permission is requested at app open in MainActivity) or no
 * cached fix exists. A live single-update request is intentionally avoided here
 * to keep this cheap and synchronous-ish; the geohash cell is coarse enough that
 * a recent cached fix suffices.
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission") // guarded by the runtime check below
    override suspend fun currentCoarse(): Coordinates? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) best = loc
        }
        return best?.let { Coordinates(it.latitude, it.longitude) }
    }
}
