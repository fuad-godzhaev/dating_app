package fyp.project.datingapp.domain.auth

import fyp.project.datingapp.database.AuthSettingsDao
import fyp.project.datingapp.database.AuthSettingsEntity
import fyp.project.datingapp.database.sha256Digest
import fyp.project.datingapp.p2p.transport.PeerIdentity
import fyp.project.datingapp.records.UserIdentity
import kotlin.jvm.JvmInline
import kotlin.time.Clock

//TODO: reorganize/split the file
@JvmInline
value class SeedPhrase(val words: List<String>) {
    init {
        require(words.size == 12 || words.size == 24) {
            "Seed phrase must be 12 or 24 words (got ${words.size})"
        }
    }
    fun toDisplayString(): String =
        words.mapIndexed { i, word -> "${i + 1}. $word" }.joinToString("  ")

    override fun toString(): String = "[REDACTED]"
}

data class AuthState(
    val hasIdentity: Boolean,
    val hasPin: Boolean,
    val hasProfile: Boolean
)

interface AuthRepository {
    suspend fun hasIdentity(): Boolean
    suspend fun hasPin(): Boolean
    suspend fun getDid(): String?
    suspend fun getAuthState(hasProfile: Boolean): AuthState
    suspend fun generateIdentity(): Result<SeedPhrase>
    suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity>
    suspend fun getIdentity(): UserIdentity?
    suspend fun sign(data: ByteArray): ByteArray
    suspend fun setPin(pin: String): Result<Unit>
    suspend fun verifyPin(pin: String): Boolean
    suspend fun deleteAccount()
}

class DefaultAuthRepository(
    private val authDao: AuthSettingsDao,
    private val keyStorage: SecureKeyStorage
) : AuthRepository {

    override suspend fun hasIdentity(): Boolean {
        return authDao.get()?.hasIdentity == true && keyStorage.hasKeyPair()
    }

    override suspend fun hasPin(): Boolean {
        val settings = authDao.get() ?: return false
        return settings.pinHash != null && settings.pinSalt != null
    }

    override suspend fun getDid(): String? {
        return authDao.get()?.didKey
    }

    override suspend fun getAuthState(hasProfile: Boolean): AuthState {
        return AuthState(
            hasIdentity = hasIdentity(),
            hasPin = hasPin(),
            hasProfile = hasProfile
        )
    }

    override suspend fun generateIdentity(): Result<SeedPhrase> = runCatching {
        val mnemonic = generateMnemonic()
        val seed = mnemonicToSeed(mnemonic)
        val publicKey = keyStorage.generateKeyPairFromSeed(seed)
        val did = deriveDid(publicKey)
        val existing = authDao.get()
        authDao.save(
            (existing ?: AuthSettingsEntity()).copy(
                hasIdentity = true,
                didKey = did,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        SeedPhrase(mnemonic)
    }

    // Recovery (ADR-0003 amended): the signing key is derived deterministically
    // from the phrase, so re-entering it on a fresh install restores the same DID.
    // (Concurrent multi-device is deferred — see ARCHITECTURE.md.)
    override suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity> = runCatching {
        val seed = mnemonicToSeed(seedPhrase.words)
        val publicKey = keyStorage.generateKeyPairFromSeed(seed)
        val did = deriveDid(publicKey)
        val existing = authDao.get()
        authDao.save(
            (existing ?: AuthSettingsEntity()).copy(
                hasIdentity = true,
                didKey = did,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        UserIdentity(did = did, publicKey = publicKey)
    }

    override suspend fun getIdentity(): UserIdentity? {
        val did = authDao.get()?.didKey ?: return null
        val publicKey = keyStorage.getPublicKey() ?: return null
        return UserIdentity(did = did, publicKey = publicKey)
    }

    override suspend fun sign(data: ByteArray): ByteArray {
        return keyStorage.sign(data)
    }

    override suspend fun setPin(pin: String): Result<Unit> = runCatching {
        require(pin.length == 4 && pin.all { it.isDigit() }) {
            "PIN must be exactly 4 digits"
        }
        val salt = generateRandomSalt()
        val hash = hashPin(pin, salt)
        val existing = authDao.get() ?: AuthSettingsEntity()
        authDao.save(existing.copy(pinHash = hash, pinSalt = salt))
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val settings = authDao.get() ?: return false
        val storedHash = settings.pinHash ?: return false
        val salt = settings.pinSalt ?: return false
        return hashPin(pin, salt) == storedHash
    }

    override suspend fun deleteAccount() {
        keyStorage.deleteKeyPair()
        authDao.clear()
    }

    private fun hashPin(pin: String, salt: String): String {
        val data = (salt + pin).encodeToByteArray()
        val hash = sha256Digest(data)
        return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    private fun generateRandomSalt(): String {
        val bytes = ByteArray(16)
        kotlin.random.Random.nextBytes(bytes)
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    private fun deriveDid(publicKey: ByteArray): String =
        PeerIdentity.didKeyFromP256(publicKey)

    private fun generateMnemonic(): List<String> {
        return List(12) { bip39WordlistSubset.random() }
    }

    private fun mnemonicToSeed(words: List<String>): ByteArray {
        val joined = words.joinToString(" ").encodeToByteArray()
        return sha256Digest(sha256Digest(joined))
    }
}

private val bip39WordlistSubset = listOf(
    "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
    "acid", "acoustic", "acquire", "across", "act", "action", "actor", "adapt",
    "address", "adjust", "admit", "adult", "advance", "advice", "aerobic", "affair",
    "afford", "afraid", "again", "agree", "album", "alert", "alien", "almost",
    "alone", "alpha", "already", "also", "alter", "always", "amazing", "among",
    "amount", "anchor", "ancient", "anger", "angle", "animal", "announce", "annual",
    "answer", "antenna", "antique", "anxiety", "apart", "apology", "appear", "apple",
    "approve", "arena", "army", "arrow", "artist", "assume", "atom", "auction",
    "august", "aunt", "author", "auto", "autumn", "average", "avocado", "avoid",
    "awake", "aware", "awesome", "awful", "awkward", "axis", "baby", "badge",
    "balance", "banana", "banner", "barely", "basic", "basket", "battle", "beach",
    "beauty", "because", "become", "begin", "behind", "believe", "benefit", "best",
    "betray", "blanket", "blast", "bleak", "blood", "blue", "board", "bomb",
    "bonus", "border", "bounce", "brain", "bread", "bridge", "broken", "brush",
    "buddy", "budget", "build", "burger", "burst", "cabin", "camera", "canal",
    "cancel", "canyon", "carbon", "carpet", "castle", "catalog", "catch", "cattle",
    "cave", "celery", "census", "cereal", "chain", "change", "chapter", "cheese",
    "chief", "child", "choice", "circle", "citizen", "civil", "claim", "clap",
    "clean", "clerk", "clever", "click", "client", "cliff", "climb", "clock",
    "cloud", "clown", "cluster", "coach", "coconut", "coffee", "coin", "collect",
    "color", "column", "combine", "come", "comedy", "comfort", "common", "company",
    "concert", "conduct", "confirm", "connect", "consider", "control", "convince", "cook",
    "coral", "core", "corn", "correct", "cotton", "couch", "country", "couple",
    "course", "cousin", "cover", "craft", "crash", "crater", "cream", "credit",
    "crew", "crisis", "crisp", "cross", "crowd", "crucial", "cruel", "cruise",
    "crystal", "cube", "culture", "cup", "curtain", "curve", "cycle", "damage",
    "dance", "danger", "daring", "dawn", "debate", "decade", "december", "decide"
)
