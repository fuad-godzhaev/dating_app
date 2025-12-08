package com.fypapp.lexicons

/**
 * com.fypapp.pass - Record a pass/swipe-left action
 */
object ComFypappPass {
    data class Record(
        val subject: String,
        val createdAt: String
    )
}
