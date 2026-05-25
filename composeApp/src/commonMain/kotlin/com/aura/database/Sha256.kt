package com.aura.database

/**
 * Platform-specific SHA-256 hash function.
 * Each platform provides its own implementation:
 * - Android/JVM: java.security.MessageDigest
 * - iOS: CommonCrypto (CC_SHA256)
 */
expect fun sha256Digest(data: ByteArray): ByteArray
