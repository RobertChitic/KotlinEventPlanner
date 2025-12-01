package com.eventplanning.domain

import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Represents an event in the event planning system.
 * Handles participant registration with capacity enforcement.
 */
data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val venue: Venue,
    val description: String = "",
    val duration: Duration,
    val maxParticipants: Int = venue.capacity,
    private val registeredParticipants: MutableList<Participant> = mutableListOf()
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    init {
        require(id.isNotBlank()) { "ID must not be blank." }
        require(title.isNotBlank()) { "Title must not be blank." }
        require(maxParticipants > 0) { "Max participants must be more than 0." }
        require(maxParticipants <= venue.capacity) {
            "Max participants ($maxParticipants) cannot exceed venue capacity (${venue.capacity})."
        }
        require(!duration.isNegative && !duration.isZero) {
            "Duration must be positive."
        }
    }

    /**
     * Returns a read-only copy of the registered participants list.
     * This prevents external modification of the internal state.
     */
    fun getRegisteredParticipants(): List<Participant> = registeredParticipants.toList()

    /**
     * Attempts to register a new participant for the event.
     * @param participant The participant to register
     * @return RegistrationResult indicating success or failure reason
     */
    fun registerParticipant(participant: Participant): RegistrationResult {
        if (isFull()) {
            return RegistrationResult.EventFull
        }

        if (isParticipantRegistered(participant)) {
            return RegistrationResult.AlreadyRegistered
        }

        registeredParticipants.add(participant)
        return RegistrationResult.Success
    }

    /**
     * Unregisters a participant from the event.
     * @param participant The participant to unregister
     * @return true if participant was found and removed, false otherwise
     */
    fun unregisterParticipant(participant: Participant): Boolean {
        return registeredParticipants.removeIf { it.id == participant.id }
    }

    /**
     * Checks if the event has reached maximum capacity.
     */
    fun isFull(): Boolean = registeredParticipants.size >= maxParticipants

    /**
     * Returns the current number of registered participants.
     */
    fun getCurrentCapacity(): Int = registeredParticipants.size

    /**
     * Returns the number of available spots remaining.
     */
    fun getAvailableSpots(): Int = maxParticipants - registeredParticipants.size

    /**
     * Checks if a specific participant is already registered.
     */
    fun isParticipantRegistered(participant: Participant): Boolean =
        registeredParticipants.any { it.id == participant.id }

    /**
     * Calculates and returns the event's end time.
     */
    fun getEndTime(): LocalDateTime = dateTime.plus(duration)

    /**
     * Checks if this event conflicts with another event (same venue, overlapping time).
     */
    fun conflictsWith(other: Event): Boolean {
        // 1. Identity Check: An event does not conflict with itself
        if (this.id == other.id) {
            return false
        }

        // 2. Venue Check: Different venues cannot conflict
        if (this.venue.id != other.venue.id) {
            return false
        }

        // 3. Time Overlap Calculation
        val thisEnd = this.getEndTime()
        val otherEnd = other.getEndTime()

        // Logic: (StartA < EndB) and (EndA > StartB)
        // This allows back-to-back events (e.g. A ends 10:00, B starts 10:00)
        return this.dateTime < otherEnd && thisEnd > other.dateTime
    }

    override fun toString(): String {
        return "$title @ ${venue.name} on ${dateTime.format(DATE_TIME_FORMATTER)} " +
                "(${getCurrentCapacity()}/$maxParticipants registered)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Sealed class representing the result of a registration attempt.
 * Provides type-safe error handling.
 */
sealed class RegistrationResult {
    object Success : RegistrationResult()
    object EventFull : RegistrationResult()
    object AlreadyRegistered : RegistrationResult()
}