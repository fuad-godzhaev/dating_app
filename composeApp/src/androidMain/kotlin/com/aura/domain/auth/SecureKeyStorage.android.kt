package com.aura.domain.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.math.BigInteger
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android actual (ADR-0003, amended for recovery). The signing key is a P-256
 * key **derived deterministically from the recovery phrase** so it can be
 * restored on a fresh install — a non-exportable Keystore key cannot be
 * reconstructed from a mnemonic. The private scalar is sealed at rest with a
 * Keystore-wrapped AES-GCM key; the compressed public point (for the did:key) is
 * stored in the clear. **Weaker than B1's non-exportable key** — the scalar is in
 * app memory at onboarding/recovery and is software-protected (see ARCHITECTURE.md
 * §key custody). Transport key = raw 32-byte Ed25519 seed (random, also sealed).
 * No Bouncy Castle (EC math in [P256]).
 */
actual class SecureKeyStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("secure_key_storage", Context.MODE_PRIVATE)

    actual fun hasKeyPair(): Boolean = prefs.contains(SIGN_PUB)

    actual suspend fun generateKeyPair(): ByteArray =
        deriveAndStore(ByteArray(32).also { SecureRandom().nextBytes(it) })

    /** Deterministic from [seed] → recovery: the same phrase yields the same DID. */
    actual suspend fun generateKeyPairFromSeed(seed: ByteArray): ByteArray = deriveAndStore(seed)

    actual fun getPublicKey(): ByteArray? {
        val pub = prefs.getString(SIGN_PUB, null) ?: return null
        return Base64.decode(pub, Base64.DEFAULT)
    }

    actual suspend fun sign(data: ByteArray): ByteArray {
        val d = loadScalar() ?: error("No keypair")
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(P256.privateKey(d))
            update(data)
            sign()
        }
    }

    actual suspend fun ecdh(peerPublicKey: ByteArray): ByteArray {
        val d = loadScalar() ?: error("No keypair")
        val peer = P256.decompress(peerPublicKey) ?: error("invalid peer public key")
        return P256.to32(P256.scalarMult(d, peer).affineX)
    }

    actual suspend fun deleteKeyPair() {
        keyStore().apply { if (containsAlias(WRAP_ALIAS)) deleteEntry(WRAP_ALIAS) }
        prefs.edit().clear().apply()
    }

    actual fun getOrCreateTransportSeed(): ByteArray {
        loadSealed(TRANSPORT_SEED, TRANSPORT_NONCE, TRANSPORT_AAD)?.let { return it }
        val seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
        storeSealed(seed, TRANSPORT_SEED, TRANSPORT_NONCE, TRANSPORT_AAD)
        return seed
    }

    // ---- signing key (derived, sealed) ----

    private fun deriveAndStore(seed: ByteArray): ByteArray {
        val d = P256.scalarFromSeed(seed)
        val q = P256.compress(P256.scalarBaseMult(d))
        storeSealed(P256.to32(d), SIGN_D, SIGN_D_NONCE, SIGN_AAD)
        prefs.edit().putString(SIGN_PUB, Base64.encodeToString(q, Base64.DEFAULT)).apply()
        return q
    }

    private fun loadScalar(): BigInteger? =
        loadSealed(SIGN_D, SIGN_D_NONCE, SIGN_AAD)?.let { BigInteger(1, it) }

    // ---- AES-GCM at rest (Keystore-wrapped key) ----

    private fun storeSealed(plaintext: ByteArray, ctKey: String, nonceKey: String, aad: ByteArray) {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, wrapKey())
            updateAAD(aad)
        }
        val sealed = cipher.doFinal(plaintext)
        prefs.edit()
            .putString(ctKey, Base64.encodeToString(sealed, Base64.DEFAULT))
            .putString(nonceKey, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
            .apply()
    }

    private fun loadSealed(ctKey: String, nonceKey: String, aad: ByteArray): ByteArray? {
        val ct = prefs.getString(ctKey, null) ?: return null
        val iv = prefs.getString(nonceKey, null) ?: return null
        return try {
            Cipher.getInstance(AES_TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, wrapKey(), GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.DEFAULT)))
                updateAAD(aad)
                doFinal(Base64.decode(ct, Base64.DEFAULT))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(WRAP_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    WRAP_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_BITS)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val WRAP_ALIAS = "com.aura.wrap"
        const val SIGN_D = "sign_d"
        const val SIGN_D_NONCE = "sign_d_nonce"
        const val SIGN_PUB = "sign_pub"
        const val TRANSPORT_SEED = "transport_seed"
        const val TRANSPORT_NONCE = "transport_nonce"
        const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_BITS = 256
        const val GCM_TAG_BITS = 128
        val SIGN_AAD = "sign".encodeToByteArray()
        val TRANSPORT_AAD = "transport".encodeToByteArray()
    }
}
