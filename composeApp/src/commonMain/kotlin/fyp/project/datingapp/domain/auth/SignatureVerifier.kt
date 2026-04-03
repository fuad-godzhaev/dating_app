package fyp.project.datingapp.domain.auth

/**
 * Platform-agnostic signature verification primitive.
 *
 * Used by the relay layer to check that a peer-signed envelope's
 * [SignedEnvelope.signature][fyp.project.datingapp.p2p.transport.wire.SignedEnvelope.signature]
 * matches its [canonicalBytes][fyp.project.datingapp.p2p.transport.wire.SignedEnvelope.canonicalBytes]
 * under the owner's advertised public key — before admitting it to the
 * cache. A forged envelope fails this check and is dropped.
 *
 * The public-key encoding is the **33-byte compressed P-256 point** that a peer
 * carries in its did:key (ADR-0003) and that [SecureKeyStorage.getPublicKey]
 * returns; the verifier decompresses it to a P-256 point. The signature is the
 * ASN.1-DER output of `SHA256withECDSA` — matching what [SecureKeyStorage.sign]
 * produces.
 *
 * The [algorithm] parameter is reserved for forward-compat. Phase B exercises
 * only `"SHA256withECDSA"` (ECDSA P-256).
 */
interface SignatureVerifier {
    fun verify(
        publicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: String = DEFAULT_ALGORITHM,
    ): Boolean

    companion object {
        const val DEFAULT_ALGORITHM: String = "SHA256withECDSA"
    }
}

/**
 * Platform actuals. Expect-factory rather than `expect class` so that
 * Android-side instances can accept a `Context` if a future implementation
 * needs one without churning call-sites.
 */
expect fun defaultSignatureVerifier(): SignatureVerifier
