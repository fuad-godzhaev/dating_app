package com.aura.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match (
    @SerialName("\$type")
    val type: String = "com.aura.records.match",

    val subject: String,
    val createdAt: String
)