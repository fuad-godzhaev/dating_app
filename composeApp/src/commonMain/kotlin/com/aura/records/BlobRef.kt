package com.aura.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlobRef(
    @SerialName("\$type")
    val type: String = "blob",

    val ref: String,            // Content Identifier (CID) of the blob
    val mimeType: String,
    val size: Long
)