package fyp.project.datingapp.domain.auth

/**
 * P-256 key re-split rollout (ADR-0003): wipes any legacy identity whose DID is
 * not a P-256 did:key ("did:key:zDn..."), i.e. the old Ed25519 ("z6Mk") or
 * pre-B.3 accounts, forcing a re-onboard under P-256 signing. The PoC has no
 * deployed users, so dropping the legacy key is acceptable. Run once at app start.
 */
class KeyMigration(
    private val authRepository: AuthRepository,
) {
    suspend fun runIfNeeded() {
        val did = authRepository.getDid() ?: return
        if (!did.startsWith(P256_DID_PREFIX)) {
            authRepository.deleteAccount()
        }
    }

    private companion object {
        const val P256_DID_PREFIX = "did:key:zDn"
    }
}
