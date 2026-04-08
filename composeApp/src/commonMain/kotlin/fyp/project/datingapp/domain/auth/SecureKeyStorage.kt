package fyp.project.datingapp.domain.auth

/**
 * Two-key custody (ADR-0003, amended for recovery):
 *  - Signing / identity key = P-256, **derived from the recovery phrase** and
 *    encrypted at rest (so it can be restored on a new install — a non-exportable
 *    key can't). [getPublicKey] returns the 33-byte compressed point (the form
 *    did:key P-256 encodes); [sign] produces a DER ECDSA (`SHA256withECDSA`) sig.
 *  - Transport key = raw 32-byte Ed25519 seed, encrypted at rest, handed to the
 *    go-libp2p host (ADR-0004) to derive its PeerId. See [getOrCreateTransportSeed].
 */
expect class SecureKeyStorage {
    fun hasKeyPair(): Boolean
    /** Generate the P-256 signing key; returns the 33-byte compressed point. */
    suspend fun generateKeyPair(): ByteArray
    /**
     * Derive the P-256 signing key deterministically from [seed]; returns the
     * 33-byte compressed point. Same seed -> same key/DID, so re-entering the
     * recovery phrase on a fresh install restores the identity (ADR-0003 amended).
     */
    suspend fun generateKeyPairFromSeed(seed: ByteArray): ByteArray
    /** 33-byte compressed P-256 point of the signing key (did:key / verify need this). */
    fun getPublicKey(): ByteArray?
    suspend fun sign(data: ByteArray): ByteArray
    suspend fun deleteKeyPair()
    /** Raw 32-byte Ed25519 transport seed (libp2p identity); created on first use. */
    fun getOrCreateTransportSeed(): ByteArray
}
