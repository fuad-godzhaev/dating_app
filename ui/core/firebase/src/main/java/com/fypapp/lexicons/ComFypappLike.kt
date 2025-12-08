package com.fypapp.lexicons

/**
 * com.fypapp.like - Record a like/swipe-right action
 */
object ComFypappLike {
    data class Record(
        val subject: String,
        val message: String? = null,
        val createdAt: String,
        val superLike: Boolean = false
    )
}
