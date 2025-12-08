package com.fypapp.lexicons

/**
 * com.fypapp.discover - Get profiles to swipe on
 */
object ComFypappDiscover {
    data class Input(
        val limit: Int = 10,
        val cursor: String? = null
    )

    data class Output(
        val profiles: List<ProfileCard>? = null,
        val cursor: String? = null
    )

    data class ProfileCard(
        val did: String,
        val profile: ComFypappProfile.Record? = null,
        val distance: Double? = null,
        val matchScore: Int? = null,
        val commonInterests: List<String>? = null,
        val recentlyActive: Boolean? = null
    )
}
