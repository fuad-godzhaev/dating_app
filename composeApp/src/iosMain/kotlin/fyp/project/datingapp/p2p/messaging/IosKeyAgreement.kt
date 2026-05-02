package fyp.project.datingapp.p2p.messaging

/** iOS stub - real P-256 ECDH lands with the iOS crypto bring-up. */
class IosKeyAgreement : KeyAgreement {
    override fun ephemeralAgree(recipientPublicKey: ByteArray): EcdhResult =
        throw UnsupportedOperationException("key agreement is not implemented on iOS yet")
    override suspend fun staticAgree(ephemeralPublicKey: ByteArray): ByteArray =
        throw UnsupportedOperationException("key agreement is not implemented on iOS yet")
}
