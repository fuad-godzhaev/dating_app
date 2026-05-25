package com.aura.p2p.discovery

import com.aura.domain.location.Coordinates
import com.aura.p2p.transport.wire.AgeRange

/**
 * The local user's PERSISTED feed-filter preferences ("show me ..."), edited from the Home screen.
 * Distinct from [DiscoveryPreferences] (what the user ADVERTISES about themselves). These are turned
 * into a runtime [DiscoveryFilters] (which also needs the live [Coordinates]) and applied to every
 * discovered candidate, so profiles that don't match are never shown.
 *
 * Defaults are permissive (= "no filtering"): any distance, 18..99, any gender, any interests.
 */
data class FeedFilterPrefs(
    /** Max distance in km; 0 = no distance limit. */
    val maxDistanceKm: Int = 0,
    val ageMin: Int = 18,
    val ageMax: Int = 99,
    /** Required candidate gender; null/blank = any. */
    val gender: String? = null,
    /** Require at least one shared interest; empty = don't filter on interests. */
    val interests: List<String> = emptyList(),
) {
    /** Combine the persisted prefs with the live [self] location into a runtime [DiscoveryFilters]. */
    fun toDiscoveryFilters(self: Coordinates?): DiscoveryFilters {
        val lo = ageMin.coerceIn(18, 120)
        val hi = ageMax.coerceIn(lo, 120)
        return DiscoveryFilters(
            gender = gender?.takeIf { it.isNotBlank() },
            interestsAny = interests,
            ageRange = AgeRange(lo, hi),
            maxDistanceKm = if (maxDistanceKm > 0) maxDistanceKm.toDouble() else null,
            selfCoordinates = self,
        )
    }

    /** True when nothing is actually being filtered (used to skip the location fetch). */
    val isDefault: Boolean
        get() = maxDistanceKm <= 0 && ageMin <= 18 && ageMax >= 99 && gender.isNullOrBlank() && interests.isEmpty()
}

/**
 * Persists the user's [FeedFilterPrefs] across launches. Platform-backed (Android SharedPreferences;
 * iOS in-memory stub - Android-primary), wired through Koin like the other platform seams.
 */
interface FeedFilterStore {
    fun load(): FeedFilterPrefs
    fun save(prefs: FeedFilterPrefs)
}
