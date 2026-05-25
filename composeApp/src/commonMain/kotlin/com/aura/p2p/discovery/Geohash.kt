package com.aura.p2p.discovery

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geohash encoding + neighbour math used by the discovery layer
 * (`p2p-subsystem-design.md` §6.2/§6.3). Pure Kotlin, no platform deps, so it
 * lives in commonMain and is unit-tested directly.
 *
 * A geohash interleaves longitude (even bit positions) and latitude (odd bit
 * positions) into a base32 string; each appended character narrows the cell by
 * 5 bits. Discovery keys off the **first 5 characters** ([geohash5], ~5 km
 * cells) and subscribes the user's own cell plus its 8 [neighbours] so a peer
 * standing just over a cell boundary is still seen.
 */
object Geohash {

    // Geohash base32 ("ghs") alphabet: 0-9 b-z, excluding a, i, l, o.
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BITS = intArrayOf(16, 8, 4, 2, 1)

    /** Number of leading characters that define a discovery cell (~5 km). */
    const val GEOHASH5_LEN: Int = 5

    /**
     * Encode a coordinate to a geohash of [precision] characters (default 9 ≈
     * 4.8 m). Longitude must be in [-180, 180], latitude in [-90, 90].
     */
    fun encode(lat: Double, lon: Double, precision: Int = 9): String {
        require(precision > 0) { "precision must be positive, got $precision" }
        require(lat in -90.0..90.0) { "latitude out of range: $lat" }
        require(lon in -180.0..180.0) { "longitude out of range: $lon" }

        var latMin = -90.0; var latMax = 90.0
        var lonMin = -180.0; var lonMax = 180.0
        val out = StringBuilder(precision)
        var even = true
        var bit = 0
        var ch = 0

        while (out.length < precision) {
            if (even) {
                val mid = (lonMin + lonMax) / 2
                if (lon >= mid) { ch = ch or BITS[bit]; lonMin = mid } else { lonMax = mid }
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) { ch = ch or BITS[bit]; latMin = mid } else { latMax = mid }
            }
            even = !even
            if (bit < 4) {
                bit++
            } else {
                out.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return out.toString()
    }

    /**
     * Decode a geohash to the centre of its cell as `(latitude, longitude)`.
     * Inverse of [encode] up to cell resolution — the result is the midpoint of
     * the cell, so a round-trip through [encode] reproduces the same geohash but
     * not the exact original coordinate. Used to estimate a candidate's distance
     * from its advertised geohash.
     */
    fun decode(geohash: String): Pair<Double, Double> {
        require(geohash.isNotEmpty()) { "geohash must be non-empty" }
        var latMin = -90.0; var latMax = 90.0
        var lonMin = -180.0; var lonMax = 180.0
        var even = true
        for (c in geohash.lowercase()) {
            val cd = BASE32.indexOf(c)
            require(cd >= 0) { "invalid geohash character '$c'" }
            for (mask in BITS) {
                if (even) {
                    val mid = (lonMin + lonMax) / 2
                    if (cd and mask != 0) lonMin = mid else lonMax = mid
                } else {
                    val mid = (latMin + latMax) / 2
                    if (cd and mask != 0) latMin = mid else latMax = mid
                }
                even = !even
            }
        }
        return ((latMin + latMax) / 2) to ((lonMin + lonMax) / 2)
    }

    /** First [GEOHASH5_LEN] characters of a geohash (the ~5 km discovery cell). */
    fun geohash5(geohash: String): String {
        require(geohash.length >= GEOHASH5_LEN) {
            "geohash too short for geohash5: '$geohash'"
        }
        return geohash.substring(0, GEOHASH5_LEN)
    }

    /**
     * The 8 cells surrounding [cell] (same length as [cell]): N, NE, E, SE, S,
     * SW, W, NW. Used to subscribe a 3x3 block of GossipSub topics so boundary
     * peers are not missed (§6.3). Returned cells are distinct.
     */
    fun neighbours(cell: String): List<String> {
        require(cell.isNotEmpty()) { "cell must be non-empty" }
        val n = adjacent(cell, Direction.N)
        val s = adjacent(cell, Direction.S)
        val e = adjacent(cell, Direction.E)
        val w = adjacent(cell, Direction.W)
        return listOf(
            n,
            adjacent(n, Direction.E),
            e,
            adjacent(s, Direction.E),
            s,
            adjacent(s, Direction.W),
            w,
            adjacent(n, Direction.W),
        )
    }

    /**
     * Great-circle distance in kilometres between two coordinates (haversine).
     * Used for the client-side `maxDistanceKm` filter once a candidate's coarse
     * location is known.
     */
    fun distanceKm(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val r = 6371.0088 // mean Earth radius, km
        val dLat = (bLat - aLat).toRadians()
        val dLon = (bLon - aLon).toRadians()
        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)
        val h = sinLat * sinLat + cos(aLat.toRadians()) * cos(bLat.toRadians()) * sinLon * sinLon
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    // ---- adjacent-cell math (Movable Type Scripts geohash-js algorithm) ------
    // border[dir][parity] = the chars in `dir` whose neighbour is in the parent's
    // sibling cell; neighbor[dir][parity] = the per-char remap within the parent.
    // parity index: 0 = even-length geohash, 1 = odd-length geohash.

    private enum class Direction { N, S, E, W }

    private val NEIGHBOR = mapOf(
        Direction.N to arrayOf(
            "p0r21436x8zb9dcf5h7kjnmqesgutwvy",
            "bc01fg45238967deuvhjyznpkmstqrwx",
        ),
        Direction.S to arrayOf(
            "14365h7k9dcfesgujnmqp0r2twvyx8zb",
            "238967debc01fg45kmstqrwxuvhjyznp",
        ),
        Direction.E to arrayOf(
            "bc01fg45238967deuvhjyznpkmstqrwx",
            "p0r21436x8zb9dcf5h7kjnmqesgutwvy",
        ),
        Direction.W to arrayOf(
            "238967debc01fg45kmstqrwxuvhjyznp",
            "14365h7k9dcfesgujnmqp0r2twvyx8zb",
        ),
    )

    private val BORDER = mapOf(
        Direction.N to arrayOf("prxz", "bcfguvyz"),
        Direction.S to arrayOf("028b", "0145hjnp"),
        Direction.E to arrayOf("bcfguvyz", "prxz"),
        Direction.W to arrayOf("0145hjnp", "028b"),
    )

    private fun adjacent(geohash: String, dir: Direction): String {
        val gh = geohash.lowercase()
        val last = gh.last()
        var parent = gh.dropLast(1)
        val parity = gh.length % 2 // 0 = even, 1 = odd
        if (BORDER.getValue(dir)[parity].indexOf(last) != -1 && parent.isNotEmpty()) {
            parent = adjacent(parent, dir)
        }
        return parent + BASE32[NEIGHBOR.getValue(dir)[parity].indexOf(last)]
    }
}
