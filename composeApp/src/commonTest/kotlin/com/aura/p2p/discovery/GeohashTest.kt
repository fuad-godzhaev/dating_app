package com.aura.p2p.discovery

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeohashTest {

    @Test fun encode_knownVectors() {
        // Canonical published geohash vectors.
        // Wikipedia: (57.64911, 10.40744) -> "u4pruydqqvj" (11 chars).
        assertEquals("u4pruydqqvj", Geohash.encode(57.64911, 10.40744, 11))
        // geohash.org: (42.6, -5.6) -> "ezs42" (5 chars).
        assertEquals("ezs42", Geohash.encode(42.6, -5.6, 5))
        // Origin sits in cell "s0000..." (longitude/latitude both >= midpoint 0).
        assertEquals("s0000", Geohash.encode(0.0, 0.0, 5))
    }

    @Test fun encode_precisionControlsLength() {
        for (p in 1..12) {
            assertEquals(p, Geohash.encode(53.3498, -6.2603, p).length)
        }
    }

    @Test fun encode_prefixIsStable() {
        // A longer encode must extend the shorter one (prefix property).
        val full = Geohash.encode(53.3498, -6.2603, 9)
        assertTrue(full.startsWith(Geohash.encode(53.3498, -6.2603, 5)))
    }

    @Test fun encode_rejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { Geohash.encode(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { Geohash.encode(0.0, 181.0) }
        assertFailsWith<IllegalArgumentException> { Geohash.encode(0.0, 0.0, 0) }
    }

    @Test fun geohash5_takesFirstFiveChars() {
        assertEquals("u4pru", Geohash.geohash5("u4pruydqqvj"))
        assertEquals(5, Geohash.geohash5("ezs42").length)
        assertFailsWith<IllegalArgumentException> { Geohash.geohash5("u4pr") }
    }

    @Test fun neighbours_returnsEightDistinctAdjacentCells() {
        val cell = Geohash.geohash5(Geohash.encode(53.3498, -6.2603, 9))
        val n = Geohash.neighbours(cell)
        assertEquals(8, n.size)
        assertEquals(8, n.toSet().size, "neighbours must be distinct")
        assertTrue(cell !in n, "centre cell must not be in its own neighbour set")
        assertTrue(n.all { it.length == cell.length }, "neighbours preserve precision")
    }

    @Test fun neighbours_areReciprocal() {
        // ordering: [N, NE, E, SE, S, SW, W, NW]
        val cell = Geohash.geohash5(Geohash.encode(53.3498, -6.2603, 9))
        val north = Geohash.neighbours(cell)[0]
        val east = Geohash.neighbours(cell)[2]
        // South of the north cell, and West of the east cell, return to centre.
        assertEquals(cell, Geohash.neighbours(north)[4], "S(N(c)) == c")
        assertEquals(cell, Geohash.neighbours(east)[6], "W(E(c)) == c")
    }

    @Test fun neighbours_eastShiftMatchesAdjacentEncode() {
        // A point one cell-width east of a cell centre lands in that cell's E neighbour.
        val lat = 53.0
        val lon = -6.0
        val cell = Geohash.geohash5(Geohash.encode(lat, lon, 9))
        val east = Geohash.neighbours(cell)[2]
        // ~0.044 deg lon per 5-char cell at this latitude; step a full cell east.
        val shifted = Geohash.geohash5(Geohash.encode(lat, lon + 0.05, 9))
        assertTrue(
            shifted == east || shifted == cell,
            "east shift should stay in centre or land in E neighbour, got $shifted (cell=$cell, E=$east)",
        )
    }

    @Test fun decode_centreReEncodesToSameGeohash() {
        // The decoded cell centre must re-encode to the original geohash.
        val gh = Geohash.encode(53.3498, -6.2603, 9)
        val (lat, lon) = Geohash.decode(gh)
        assertEquals(gh, Geohash.encode(lat, lon, 9))
        // And the centre is within the ~cell of the original point (a few km at p5).
        val (lat5, lon5) = Geohash.decode(Geohash.geohash5(gh))
        assertTrue(Geohash.distanceKm(53.3498, -6.2603, lat5, lon5) < 5.0)
    }

    @Test fun decode_rejectsInvalidChar() {
        assertFailsWith<IllegalArgumentException> { Geohash.decode("abia") } // 'a','i' not in base32
    }

    @Test fun distanceKm_sanity() {
        // Identical points are zero distance.
        assertEquals(0.0, Geohash.distanceKm(53.3498, -6.2603, 53.3498, -6.2603), 1e-9)
        // Dublin -> London is ~463 km (allow 10 km slack).
        val d = Geohash.distanceKm(53.3498, -6.2603, 51.5074, -0.1278)
        assertTrue(abs(d - 463.0) < 10.0, "expected ~463 km, got $d")
        // Symmetric.
        val rev = Geohash.distanceKm(51.5074, -0.1278, 53.3498, -6.2603)
        assertTrue(abs(d - rev) < 1e-6)
    }
}
