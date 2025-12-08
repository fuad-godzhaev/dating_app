package com.fypapp.lexicons

/**
 * com.fypapp.updateProfile - Procedure to update user profile
 */
object ComFypappUpdateProfile {
    data class Input(
        val profile: ComFypappProfile.Record
    )

    data class Output(
        val uri: String,
        val cid: String
    )
}
