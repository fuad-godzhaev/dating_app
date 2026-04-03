package fyp.project.datingapp.p2p.relay

/**
 * iOS actual — **Phase A stub**.
 *
 * A Secure-Enclave-backed implementation arrives with the iOS libp2p work
 * described in `p2p-subsystem-design.md` §13.2. Until then iOS targets can
 * compile against this class but must not wire it into [RelayPolicy]; the
 * policy object is currently only instantiated on Android/JVM.
 *
 * Throwing here (rather than silently returning empty bytes) is deliberate:
 * if something on iOS ever constructs a [RelayPolicy] by accident, we want a
 * loud failure rather than a cache that silently admits everything.
 */
actual class CacheEncryption actual constructor() : CacheCipher {
    actual override fun seal(plaintext: ByteArray, aad: ByteArray): Sealed =
        throw UnsupportedOperationException(
            "iOS CacheEncryption is stubbed in Phase A; see p2p-subsystem-design.md §13.2"
        )

    actual override fun open(ciphertext: ByteArray, nonce: ByteArray, aad: ByteArray): ByteArray? =
        throw UnsupportedOperationException(
            "iOS CacheEncryption is stubbed in Phase A; see p2p-subsystem-design.md §13.2"
        )
}
