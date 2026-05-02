package fyp.project.datingapp.domain.auth

import fyp.project.datingapp.database.sha256Digest
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithBytes
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class SecureKeyStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun hasKeyPair(): Boolean = defaults.dataForKey(KEY_PUBLIC) != null

    actual suspend fun generateKeyPair(): ByteArray {
        val privateKey = ByteArray(32)
        privateKey.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, 32UL, pinned.addressOf(0))
        }
        return storeAndReturnPublic(privateKey)
    }

    actual suspend fun generateKeyPairFromSeed(seed: ByteArray): ByteArray {
        return storeAndReturnPublic(sha256Digest(sha256Digest(seed)))
    }

    actual fun getPublicKey(): ByteArray? {
        return defaults.dataForKey(KEY_PUBLIC)?.toKotlinByteArray()
    }

    actual suspend fun sign(data: ByteArray): ByteArray {
        val privateKey = defaults.dataForKey(KEY_PRIVATE)?.toKotlinByteArray()
            ?: error("No keypair")
        return sha256Digest(privateKey + data)
    }

    // TODO(iOS): real P-256 ECDH (Secure Enclave / Security framework). Stubbed.
    actual suspend fun ecdh(peerPublicKey: ByteArray): ByteArray =
        throw UnsupportedOperationException("ECDH is not implemented on iOS yet")

    actual suspend fun deleteKeyPair() {
        defaults.removeObjectForKey(KEY_PRIVATE)
        defaults.removeObjectForKey(KEY_PUBLIC)
        defaults.removeObjectForKey(KEY_TRANSPORT_SEED)
    }

    // TODO(iOS): real P-256 signing via Secure Enclave + Ed25519 transport key;
    // this stub just persists a random 32-byte seed unencrypted.
    actual fun getOrCreateTransportSeed(): ByteArray {
        defaults.dataForKey(KEY_TRANSPORT_SEED)?.toKotlinByteArray()?.let { return it }
        val seed = ByteArray(32)
        seed.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, 32UL, pinned.addressOf(0))
        }
        defaults.setObject(seed.toNSData(), forKey = KEY_TRANSPORT_SEED)
        return seed
    }

    private fun storeAndReturnPublic(privateKey: ByteArray): ByteArray {
        val publicKey = sha256Digest(privateKey)
        defaults.setObject(privateKey.toNSData(), forKey = KEY_PRIVATE)
        defaults.setObject(publicKey.toNSData(), forKey = KEY_PUBLIC)
        return publicKey
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }

    private fun NSData.toKotlinByteArray(): ByteArray {
        val result = ByteArray(length.toInt())
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
        return result
    }

    companion object {
        private const val KEY_PRIVATE = "private_key"
        private const val KEY_PUBLIC = "public_key"
        private const val KEY_TRANSPORT_SEED = "transport_seed"
    }
}
