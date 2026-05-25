package com.aura.p2p.discovery

import com.aura.domain.location.Coordinates
import com.aura.p2p.transport.wire.AgeRange
import com.aura.p2p.transport.wire.PresenceRecord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryFiltersTest {

    private fun record(
        gender: String = "female",
        interests: List<String> = listOf("climbing", "books"),
        ageRange: AgeRange = AgeRange(25, 35),
        geohash: String = Geohash.encode(53.3498, -6.2603, 9),
    ) = PresenceRecord(
        did = "did:key:zDnTest",
        peerId = "12D3KooWtest",
        multiaddrs = listOf("/ip4/10.0.2.16/tcp/4001"),
        profileCid = "bafytest",
        geohash = geohash,
        interests = interests,
        gender = gender,
        lookingFor = listOf("everyone"),
        ageRange = ageRange,
        announcedAt = "2026-05-23T12:00:00Z",
        ttl = 900_000L,
        signature = byteArrayOf(1),
    )

    @Test fun emptyFilters_matchEverything() {
        assertTrue(DiscoveryFilters().matches(record()))
    }

    @Test fun gender_isCaseInsensitive() {
        assertTrue(DiscoveryFilters(gender = "FEMALE").matches(record(gender = "female")))
        assertFalse(DiscoveryFilters(gender = "male").matches(record(gender = "female")))
    }

    @Test fun interests_requireOverlap() {
        assertTrue(DiscoveryFilters(interestsAny = listOf("books", "running")).matches(record()))
        assertFalse(DiscoveryFilters(interestsAny = listOf("running", "chess")).matches(record()))
    }

    @Test fun ageRange_requiresOverlap() {
        assertTrue(DiscoveryFilters(ageRange = AgeRange(30, 40)).matches(record(ageRange = AgeRange(25, 35))))
        assertFalse(DiscoveryFilters(ageRange = AgeRange(40, 50)).matches(record(ageRange = AgeRange(25, 35))))
    }

    @Test fun distance_filtersWhenSelfCoordinatesPresent() {
        val dublin = Coordinates(53.3498, -6.2603)
        // Candidate in Dublin: within 50 km.
        assertTrue(
            DiscoveryFilters(maxDistanceKm = 50.0, selfCoordinates = dublin)
                .matches(record(geohash = Geohash.encode(53.34, -6.26, 9))),
        )
        // Candidate in London: beyond 50 km.
        assertFalse(
            DiscoveryFilters(maxDistanceKm = 50.0, selfCoordinates = dublin)
                .matches(record(geohash = Geohash.encode(51.5074, -0.1278, 9))),
        )
    }

    @Test fun distance_skippedWithoutSelfCoordinates() {
        // No selfCoordinates -> distance filter does not apply (matches).
        assertTrue(
            DiscoveryFilters(maxDistanceKm = 1.0)
                .matches(record(geohash = Geohash.encode(51.5074, -0.1278, 9))),
        )
    }
}
