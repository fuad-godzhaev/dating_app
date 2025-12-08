package com.fypapp.lexicons.repo

/**
 * AT Protocol URI representation
 * Format: at://AUTHORITY/COLLECTION/RKEY
 */
data class AtUri(val uri: String) {
    override fun toString(): String = uri
}
