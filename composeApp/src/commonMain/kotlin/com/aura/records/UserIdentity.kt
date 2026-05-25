package com.aura.records

data class UserIdentity(val did: String, val publicKey: ByteArray, val keyAlgorithm: String = "P-256")
{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserIdentity) return false
        return did == other.did
        }
    override fun hashCode(): Int = did.hashCode()
}