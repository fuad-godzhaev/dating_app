package com.aura.database

import java.security.MessageDigest

actual fun sha256Digest(data: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(data)
}
