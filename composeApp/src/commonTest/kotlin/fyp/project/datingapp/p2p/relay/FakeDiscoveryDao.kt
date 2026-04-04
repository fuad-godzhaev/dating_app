package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.database.appView.dao.DiscoveryDao
import fyp.project.datingapp.database.appView.entities.PeerProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [DiscoveryDao] for commonTest. Implements exactly the query
 * semantics that [RelayPolicy] + [SweepJob] depend on; the Flow-returning
 * discovery queries fall back to a single emission of the current state.
 *
 * Not thread-safe — tests are single-threaded via `runTest`.
 */
class FakeDiscoveryDao : DiscoveryDao {
    private val rows = mutableMapOf<String, PeerProfileEntity>()

    fun all(): List<PeerProfileEntity> = rows.values.toList()

    override fun getDiscoverableProfiles(): Flow<List<PeerProfileEntity>> =
        flowOf(rows.values.filterNot { it.isBlocked }.sortedByDescending { it.lastSeenAt })

    override fun getProfilesByAgeRange(minAge: Int, maxAge: Int): Flow<List<PeerProfileEntity>> =
        flowOf(
            rows.values.filter { !it.isBlocked && (it.age ?: -1) in minAge..maxAge }
                .sortedByDescending { it.lastSeenAt }
        )

    override fun getProfilesByLocation(geohashPrefix: String): Flow<List<PeerProfileEntity>> =
        flowOf(
            rows.values.filter { !it.isBlocked && (it.geohash ?: "").startsWith(geohashPrefix) }
                .sortedByDescending { it.lastSeenAt }
        )

    override suspend fun getProfileByDid(did: String): PeerProfileEntity? = rows[did]

    override suspend fun hasProfile(did: String): Boolean = rows.containsKey(did)

    override suspend fun upsertProfile(profile: PeerProfileEntity) {
        rows[profile.did] = profile
    }

    override suspend fun blockPeer(did: String) {
        rows[did]?.let { rows[did] = it.copy(isBlocked = true) }
    }

    override suspend fun unblockPeer(did: String) {
        rows[did]?.let { rows[did] = it.copy(isBlocked = false) }
    }

    override suspend fun updateLastSeen(did: String, timestamp: Long) {
        rows[did]?.let { rows[did] = it.copy(lastSeenAt = timestamp) }
    }

    override suspend fun removeStaleProfiles(cutoffTimestamp: Long) {
        rows.entries.removeAll { (_, v) -> v.lastSeenAt < cutoffTimestamp && !v.isBlocked }
    }

    override suspend fun countDiscoverable(): Int = rows.values.count { !it.isBlocked }

    override suspend fun countCached(): Int = rows.values.count { it.cachedAt != null }

    override suspend fun pickOldestByLastServed(n: Int): List<String> =
        rows.values
            .filter { it.cachedAt != null }
            .sortedWith(
                compareBy(
                    { it.lastServedAt ?: Long.MIN_VALUE },
                    { it.cachedAt ?: Long.MIN_VALUE },
                )
            )
            .take(n)
            .map { it.did }

    override suspend fun sweepExpired(now: Long): Int {
        val before = rows.size
        rows.entries.removeAll { (_, v) -> v.expiresAt != null && v.expiresAt!! < now }
        return before - rows.size
    }

    override suspend fun bumpLastServed(did: String, ts: Long) {
        rows[did]?.let { rows[did] = it.copy(lastServedAt = ts) }
    }

    override suspend fun deleteByDid(did: String) {
        rows.remove(did)
    }
}
