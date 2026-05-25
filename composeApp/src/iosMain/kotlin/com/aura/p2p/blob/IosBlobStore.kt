package com.aura.p2p.blob

/**
 * iOS stub — blob file storage lands with the iOS transport bring-up
 * (`p2p-subsystem-design.md` §13.2). [read]/[exists] return empty so the UI
 * falls back to a placeholder rather than crashing; [write] is unsupported.
 */
class IosBlobStore : BlobStore {
    override suspend fun write(cid: String, bytes: ByteArray): String =
        throw UnsupportedOperationException("BlobStore is not implemented on iOS yet")

    override suspend fun read(path: String): ByteArray? = null

    override suspend fun exists(path: String): Boolean = false
}
