package com.aura.domain.auth

import java.security.Signature

/**
 * Android actual (ADR-0003) — ECDSA P-256 over the 33-byte compressed point a
 * peer carries in its did:key. Decompression + key reconstruction live in [P256]
 * (pure JCA + BigInteger; no Bouncy Castle). Any failure collapses to `false` —
 * the policy layer must not branch on the cause (oracle risk).
 */
private class P256SignatureVerifier : SignatureVerifier {
    override fun verify(
        publicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: String,
    ): Boolean = try {
        val point = P256.decompress(publicKey)
        if (point == null) {
            false
        } else {
            Signature.getInstance(algorithm).run {
                initVerify(P256.publicKey(point))
                update(data)
                verify(signature)
            }
        }
    } catch (_: Exception) {
        false
    }
}

actual fun defaultSignatureVerifier(): SignatureVerifier = P256SignatureVerifier()
