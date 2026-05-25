package com.aura.p2p.discovery

import com.aura.domain.location.Coordinates
import com.aura.p2p.transport.wire.AgeRange
import com.aura.p2p.transport.wire.PresenceRecord

/**
 * Client-side filter applied to verified, non-expired candidates. Every field is
 * optional; a null/empty field means "don't filter on this". Matching is local
 * and cheap — the network layer already narrowed to the geohash neighbourhood.
 *
 * Distance filtering is best-effort: it needs both [maxDistanceKm] and the
 * seeker's own [selfCoordinates], and estimates the candidate's position from
 * the (coarse) geohash it advertised, so it is accurate to the cell, not the
 * metre.
 */
data class DiscoveryFilters(
    /** Require the candidate's gender to equal this (case-insensitive). */
    val gender: String? = null,
    /** Require at least one shared interest with this list. */
    val interestsAny: List<String> = emptyList(),
    /** Require the candidate's advertised age range to overlap this one. */
    val ageRange: AgeRange? = null,
    /** Drop candidates farther than this from [selfCoordinates]. */
    val maxDistanceKm: Double? = null,
    /** The seeker's own location, required for [maxDistanceKm] to apply. */
    val selfCoordinates: Coordinates? = null,
) {
    fun matches(record: PresenceRecord): Boolean {
        if (gender != null && !record.gender.equals(gender, ignoreCase = true)) return false
        if (interestsAny.isNotEmpty() && record.interests.none { it in interestsAny }) return false
        if (ageRange != null && !rangesOverlap(ageRange, record.ageRange)) return false
        if (maxDistanceKm != null && selfCoordinates != null) {
            val (lat, lon) = Geohash.decode(record.geohash)
            val km = Geohash.distanceKm(selfCoordinates.latitude, selfCoordinates.longitude, lat, lon)
            if (km > maxDistanceKm) return false
        }
        return true
    }

    private fun rangesOverlap(a: AgeRange, b: AgeRange): Boolean = a.min <= b.max && b.min <= a.max
}
