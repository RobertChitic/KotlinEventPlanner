package com.eventplanning. domain

/**
 * Represents a participant who can register for events.
 */
data class Participant(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val organization: String = ""
) {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    init {
        require(id.isNotBlank()) { "ID must not be blank." }
        require(name.isNotBlank()) { "Name must not be blank." }
        require(email.matches(EMAIL_REGEX)) {
            "Invalid email format. Expected format: example@domain.com"
        }
    }

    /**
     * Returns a display-friendly string for UI elements.
     */
    fun getDisplayName(): String = "$name ($email)"

    override fun toString(): String = getDisplayName()
}