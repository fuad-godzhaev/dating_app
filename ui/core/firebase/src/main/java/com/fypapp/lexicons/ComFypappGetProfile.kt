package com.fypapp.lexicons

/**
 * com.fypapp.getProfile - Query to retrieve a user's profile
 */
object ComFypappGetProfile {
    data class Input(
        val actor: String
    )

    data class Output(
        val profile: ComFypappProfile.Record? = null,
        val uri: String? = null,
        val cid: String? = null,
        val matchScore: Int? = null,
        val distance: Double? = null,
        val isLiked: Boolean? = null,
        val isMatched: Boolean? = null,
        val isBlocked: Boolean? = null
    )
}
