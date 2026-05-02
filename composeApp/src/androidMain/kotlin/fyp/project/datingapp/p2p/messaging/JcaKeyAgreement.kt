package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.domain.auth.P256
import fyp.project.datingapp.domain.auth.SecureKeyStorage
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Android [KeyAgreement] over the project's pure-JCA P-256 math ([P256]). The
 * recipient side reuses the stored signing key via [SecureKeyStorage.ecdh]; the
 * sender side uses a fresh ephemeral keypair (forward secrecy of the sender key).
 */
class JcaKeyAgreement(private val keyStorage: SecureKeyStorage) : KeyAgreement {

    override fun ephemeralAgree(recipientPublicKey: ByteArray): EcdhResult {
        val recipient = P256.decompress(recipientPublicKey) ?: error("invalid recipient public key")
        val ephScalar = randomScalar()
        val ephemeralPublic = P256.compress(P256.scalarBaseMult(ephScalar))
        val shared = P256.to32(P256.scalarMult(ephScalar, recipient).affineX)
        return EcdhResult(ephemeralPublic, shared)
    }

    override suspend fun staticAgree(ephemeralPublicKey: ByteArray): ByteArray =
        keyStorage.ecdh(ephemeralPublicKey)

    private fun randomScalar(): BigInteger {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return P256.scalarFromSeed(bytes)
    }
}
