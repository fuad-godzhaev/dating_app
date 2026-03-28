package fyp.project.datingapp.database.appView.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fyp.project.datingapp.database.appView.entities.PeerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {

    //Get all non-blocked users
    @Query(
        """
        SELECT * FROM peer_profiles 
        WHERE isBlocked = 0 
        ORDER BY lastSeenAt DESC
    """
    )
    fun getDiscoverableProfiles(): Flow<List<PeerProfileEntity>>

    // Age filter
    @Query(
        """
        SELECT * FROM peer_profiles 
        WHERE isBlocked = 0 
          AND age BETWEEN :minAge AND :maxAge
        ORDER BY lastSeenAt DESC
    """
    )
    fun getProfilesByAgeRange(minAge: Int, maxAge: Int): Flow<List<PeerProfileEntity>>

    //Filter by geohash prefix (proximity matching)
    @Query(
        """
        SELECT * FROM peer_profiles 
        WHERE isBlocked = 0 
          AND geohash LIKE :geohashPrefix || '%'
        ORDER BY lastSeenAt DESC
    """
    )
    fun getProfilesByLocation(geohashPrefix: String): Flow<List<PeerProfileEntity>>

    // Get a specific peer's profile by DID.
    @Query("SELECT * FROM peer_profiles WHERE did = :did")
    suspend fun getProfileByDid(did: String): PeerProfileEntity?

    //Check if we already have a profile cached for this peer
    @Query("SELECT COUNT(*) > 0 FROM peer_profiles WHERE did = :did")
    suspend fun hasProfile(did: String): Boolean

    //Insert or update a peer profile. Called AFTER verifying a profile received from the network
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PeerProfileEntity)

    //Block/unblock a peer locally
    @Query("UPDATE peer_profiles SET isBlocked = 1 WHERE did = :did")
    suspend fun blockPeer(did: String)

    @Query("UPDATE peer_profiles SET isBlocked = 0 WHERE did = :did")
    suspend fun unblockPeer(did: String)

    //Update last-seen timestamp when we detect a peer is online
    @Query("UPDATE peer_profiles SET lastSeenAt = :timestamp WHERE did = :did")
    suspend fun updateLastSeen(did: String, timestamp: Long)

    //Garbage collection — remove profiles not seen in N days.
    @Query("DELETE FROM peer_profiles WHERE lastSeenAt < :cutoffTimestamp AND isBlocked = 0")
    suspend fun removeStaleProfiles(cutoffTimestamp: Long)

    //Count discoverable profiles (for UI badge/metrics)
    @Query("SELECT COUNT(*) FROM peer_profiles WHERE isBlocked = 0")
    suspend fun countDiscoverable(): Int

    // ---- Relay cache queries (v3) ------------------------------------------
    // "Relay-cached" rows are those where cachedAt IS NOT NULL. These queries
    // deliberately never touch discovery-only rows (cachedAt IS NULL) so the
    // relay cache capacity math stays isolated from discovery counts.

    /** Number of rows currently held in the relay cache. */
    @Query("SELECT COUNT(*) FROM peer_profiles WHERE cachedAt IS NOT NULL")
    suspend fun countCached(): Int

    /**
     * DIDs of the N oldest-by-last-serve relay-cached rows. Callers then
     * decide whether to evict (delete) or demote them. Order is strictly by
     * `lastServedAt` ascending; ties fall back on `cachedAt` to keep results
     * deterministic under heavy write pressure.
     */
    @Query(
        """
        SELECT did FROM peer_profiles
        WHERE cachedAt IS NOT NULL
        ORDER BY lastServedAt ASC, cachedAt ASC
        LIMIT :n
        """
    )
    suspend fun pickOldestByLastServed(n: Int): List<String>

    /**
     * Nightly sweep — drops every relay-cached row past its TTL. Returns the
     * number of rows deleted so the caller can surface a metric.
     */
    @Query("DELETE FROM peer_profiles WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun sweepExpired(now: Long): Int

    /** Bump the serve-timestamp so LRU eviction keeps hot rows alive. */
    @Query("UPDATE peer_profiles SET lastServedAt = :ts WHERE did = :did")
    suspend fun bumpLastServed(did: String, ts: Long)

    /**
     * Hard-delete a peer row by DID. Used by [RelayPolicy] when evicting LRU
     * rows and when processing tombstones (future work). Discovery-only rows
     * are deleted too — after eviction they can be re-discovered via the
     * network.
     */
    @Query("DELETE FROM peer_profiles WHERE did = :did")
    suspend fun deleteByDid(did: String)
}