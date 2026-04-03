package fyp.project.datingapp.domain.auth

/**
 * iOS actual — Phase A stub.
 *
 * The real implementation will bridge to `SecKeyVerifySignature` alongside
 * the iOS libp2p work (see `p2p-subsystem-design.md` §13.2). Until then the
 * stub returns `false` for every call so any accidental iOS-side verify
 * path fails closed rather than open.
 */
private class StubSignatureVerifier : SignatureVerifier {
    override fun verify(
        publicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: String,
    ): Boolean = false
}

actual fun defaultSignatureVerifier(): SignatureVerifier = StubSignatureVerifier()
