package fyp.project.datingapp.p2p.relay

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android actual — AES-256-GCM backed by a non-exportable key in the
 * Android Keystore. The key is generated on first use and retrieved by
 * alias on every subsequent call.
 *
 * Threat model:
 *  - Filesystem extraction sees only [Sealed.ciphertext] + [Sealed.nonce];
 *    the key never leaves the Keystore / TEE boundary.
 *  - A modified app running on the device can still decrypt, because the
 *    key is bound to the app package and not to any integrity check. That
 *    residual risk is acknowledged in `relay-architecture.md` §9.2 (1).
 *  - `setUserAuthenticationRequired(false)` — the cache must work on the
 *    main scope (including background sweeps) without a biometric prompt.
 */
actual class CacheEncryption actual constructor() : CacheCipher {

    actual override fun seal(plaintext: ByteArray, aad: ByteArray): Sealed {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        return Sealed(ciphertext = ciphertext, nonce = cipher.iv)
    }

    actual override fun open(ciphertext: ByteArray, nonce: ByteArray, aad: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(aad)
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            // GCM tag mismatch, tampered AAD, truncated ciphertext — all
            // collapse to "cache miss" at the caller. Never leak which
            // check failed; the distinction isn't actionable at the policy
            // layer and inviting callers to branch on it invites bugs.
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "fyp.project.datingapp.cacheWrap"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_BITS = 256
        const val GCM_TAG_BITS = 128
    }
}
