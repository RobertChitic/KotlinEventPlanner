package com.eventplanning.domain

import java.time.LocalDateTime
import java.time.Duration

data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val venue: Venue,
    val description: String = "",
    val duration: Duration = Duration.ofHours(2),
    val maxParticipants: Int = venue.capacity,
    private val registeredParticipants: MutableList<Participant> = mutableListOf()
)
{
    init {
        require(title.isNotBlank()) {"Title must not be blank."}
        require(id.isNotBlank()) {"ID must not be blank."}
        require(maxParticipants > 0) { "Max participants must be more than 0." }
        require(maxParticipants <= venue.capacity) {
            "Max participants ($maxParticipants) cannot exceed venue capacity (${venue.capacity})."
        }
        require(!duration.isNegative && !duration.isZero) {
            "Duration must be positive."
        }
    }

    fun getRegisteredParticipants(): List<Participant> =
        registeredParticipants.toList()

    fun registerParticipant(participant: Participant): Boolean {
        // Check capacity first (Part C requirement)
        if (isFull()) {
            return false
        }

        // Check for duplicate registration using ID (more reliable)
        if (isParticipantRegistered(participant)) {
            return false
        }

        // Register participant
        registeredParticipants.add(participant)
        return true
    }

    fun unregisterParticipant(participant: Participant): Boolean {
        return registeredParticipants.removeIf { it.id == participant.id }
    }


    fun isFull(): Boolean =
        registeredParticipants.size >= maxParticipants


    fun getCurrentCapacity(): Int =
        registeredParticipants.size


    fun getAvailableSpots(): Int =
        maxParticipants - registeredParticipants.size


    fun isParticipantRegistered(participant: Participant): Boolean =
        registeredParticipants.any { it.id == participant.id }


    fun getEndTime(): LocalDateTime =
        dateTime.plus(duration)


    fun conflictsWith(other: Event): Boolean {
        // Different venues = no conflict
        if (this.venue.id != other.venue.id) {
            return false
        }

        val thisEnd = this.getEndTime()
        val otherEnd = other.getEndTime()

        // Check for time overlap:
        // Events conflict if one starts before the other ends
        return this.dateTime < otherEnd && thisEnd > other.dateTime
    }

    override fun toString(): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return "$title @ ${venue.name} on ${dateTime.format(formatter)} " +
                "(${getCurrentCapacity()}/$maxParticipants registered)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}