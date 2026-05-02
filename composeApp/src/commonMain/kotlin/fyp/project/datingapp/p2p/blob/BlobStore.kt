package fyp.project.datingapp.p2p.blob

/**
 * Platform file IO for blob bytes (profile photos / avatars). Blobs are
 * public-by-design images, so v1 stores them as plaintext files under the app's
 * private storage (no at-rest encryption; see MASTER-PLAN-EXECUTION-NOTES). An
 * interface (not expect/actual) so it wires through Koin per-platform like
 * `LocationProvider` / `LanBootstrap`, and commonTest can fake it.
 */
interface BlobStore {
    /** Persist [bytes] under [cid]; returns the absolute file path written. */
    suspend fun write(cid: String, bytes: ByteArray): String

    /** Read the bytes at [path], or null if missing / unreadable. */
    suspend fun read(path: String): ByteArray?

    /** Cheap existence check (avoids reading a multi-MB file just to probe). */
    suspend fun exists(path: String): Boolean
}
