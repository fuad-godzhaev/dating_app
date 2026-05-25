package com.aura

import com.aura.records.BlobRef
import com.aura.records.Message
import com.aura.records.UserProfile

sealed class DataValidatorResult {
    data object Valid : DataValidatorResult()
    data class Invalid(val errors: List<String>) : DataValidatorResult()
}

object DataValidator {
    fun validate(record: Any): DataValidatorResult {
        return when (record) {
            is UserProfile -> validateProfile(record)
            //is GraphLike -> validateLike(record)
            //is GraphMatch -> validateMatch(record)
            is Message -> validateMessage(record)
            else -> DataValidatorResult.Invalid(
                listOf("Unknown record type: ${record::class.simpleName}")
            )
        }
    }

    // ------Profile Validation-------
    //Validates against: datingapp/lexicons/profile.json
    private fun validateProfile(profile: UserProfile): DataValidatorResult {
        val errors = mutableListOf<String>()

        // Required fields
        if (profile.displayName.isBlank()) {
            errors.add("displayName is required and cannot be blank")
        }
        if (profile.createdAt.isBlank()) {
            errors.add("createdAt is required")
        }

        // displayName constraints
        if (profile.displayName.length > 640) {
            errors.add("displayName exceeds maxLength of 640 bytes")
        }
        //if (graphemeCount(profile.displayName) > 64) {
        //    errors.add("displayName exceeds maxGraphemes of 64")
        //}

        // bio constraints
        profile.bio?.let { bio ->
            if (bio.length > 5000) {
                errors.add("bio exceeds maxLength of 5000 bytes")
            }
            //if (graphemeCount(bio) > 500) {
            //  errors.add("bio exceeds maxGraphemes of 500")
            //}
        }

        // age constraints
        profile.age?.let { age ->
            if (age < 18) {
                errors.add("age must be at least 18 (got $age)")
            }
            if (age > 120) {
                errors.add("age must be at most 120 (got $age)")
            }
        }

        // interests constraints
        profile.interests?.let { interests ->
            if (interests.size > 20) {
                errors.add("interests exceeds max of 20 items (got ${interests.size})")
            }
            interests.forEachIndexed { i, interest ->
                if (interest.length > 100) {
                    errors.add("interests[$i] exceeds maxLength of 100")
                }
            }
        }

        // avatar blob constraints
        profile.avatar?.let { avatar ->
            validateBlob(avatar, "avatar", errors)
        }

        // photos array + blob constraints
        profile.photos?.let { photos ->
            if (photos.size > 6) {
                errors.add("photos exceeds max of 6 items (got ${photos.size})")
            }
            photos.forEachIndexed { i, photo ->
                validateBlob(photo, "photos[$i]", errors)
            }
        }

        // createdAt format
        if (profile.createdAt.isNotBlank() && !isValidDatetime(profile.createdAt)) {
            errors.add("createdAt is not a valid ISO 8601 datetime")
        }

        // $type field
        if (profile.type != "com.aura.records.profile") {
            errors.add("\$type must be 'com.aura.records.profile' (got '${profile.type}')")
        }

        return if (errors.isEmpty()) DataValidatorResult.Valid
        else DataValidatorResult.Invalid(errors)
    }

    // ------Message Validation-------
    //Validates against: datingapp/lexicons/message.json
    private fun validateMessage(message: Message): DataValidatorResult {
        val errors = mutableListOf<String>()

        if (message.recipient.isBlank()) {
            errors.add("recipient is required")
        }
        if (message.recipient.isNotBlank() && !isValidDid(message.recipient)) {
            errors.add("recipient must be a valid DID")
        }

        if (message.cipherText.isEmpty()) {
            errors.add("ciphertext is required and cannot be empty")
        }
        if (message.cipherText.size > 65536) {
            errors.add("ciphertext exceeds maxLength of 65536 bytes (got ${message.cipherText.size})")
        }

        if (message.createdAt.isBlank()) {
            errors.add("createdAt is required")
        }
        if (message.createdAt.isNotBlank() && !isValidDatetime(message.createdAt)) {
            errors.add("createdAt is not a valid ISO 8601 datetime")
        }

        if (message.type != "com.aura.records.message") {
            errors.add("\$type must be 'com.aura.records.message'")
        }

        return if (errors.isEmpty()) DataValidatorResult.Valid
        else DataValidatorResult.Invalid(errors)
    }

    //---Shared helpers---
    //Validate blob metadata
    private fun validateBlob(blob: BlobRef, fieldName: String, errors: MutableList<String>) {
        val allowedMimes = listOf("image/png", "image/jpeg")
        if (blob.mimeType !in allowedMimes) {
            errors.add("$fieldName has invalid mimeType '${blob.mimeType}' (allowed: $allowedMimes)")
        }
        if (blob.size > 2_000_000) {
            errors.add("$fieldName exceeds maxSize of 2MB (got ${blob.size} bytes)")
        }
        if (blob.size <= 0) {
            errors.add("$fieldName has invalid size: ${blob.size}")
        }
        if (blob.ref.isBlank()) {
            errors.add("$fieldName has empty CID ref")
        }
    }

    // TODO: Count Unicode grapheme clusters
   /* private fun graphemeCount(text: String): Int {
        return text.codePointCount(0, text.length)
    } */

    //Validate DID format
    private fun isValidDid(did: String): Boolean {
        // Pattern: did:<method>:<at-least-one-character>
        val didRegex = Regex("^did:[a-z]+:[a-zA-Z0-9._:%-]+$")
        return didRegex.matches(did)
    }

    //ISO 8601 datetime validation
    private fun isValidDatetime(datetime: String): Boolean {
        return try {
            // TODO: On Android/JVM, use java.time for proper parsing
            // Simplified check:
            datetime.matches(
                Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$")
            )
        } catch (e: Exception) {
            false
        }
    }
}