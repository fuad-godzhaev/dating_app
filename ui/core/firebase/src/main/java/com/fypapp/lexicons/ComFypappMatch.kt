package com.fypapp.lexicons

/**
 * com.fypapp.match - A mutual match between two users
 */
object ComFypappMatch {
    data class Record(
        val user1: String,
        val user2: String,
        val createdAt: String,
        val isActive: Boolean = true,
        val lastMessageAt: String? = null
    )
}
