package com.e3hi.geodrop.data

import java.util.Locale

object ExplorerUsername {
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 20
    private val VALID_CHARACTERS = Regex("^[a-z0-9._]+$")

    fun sanitize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length < MIN_LENGTH) {
            throw InvalidUsernameException(ValidationError.TOO_SHORT)
        }
        if (trimmed.length > MAX_LENGTH) {
            throw InvalidUsernameException(ValidationError.TOO_LONG)
        }
        val normalized = trimmed.lowercase(Locale.US)
        if (!VALID_CHARACTERS.matches(normalized)) {
            throw InvalidUsernameException(ValidationError.INVALID_CHARACTERS)
        }
        return normalized
    }

    class InvalidUsernameException(val reason: ValidationError) : IllegalArgumentException()

    enum class ValidationError {
        TOO_SHORT,
        TOO_LONG,
        INVALID_CHARACTERS
    }
}