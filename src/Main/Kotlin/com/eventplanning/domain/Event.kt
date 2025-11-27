package com.eventplanning.domain

import java.time.LocalDateTime
import java.time.Duration

data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val venue: Venue,
    val description: String = "",
    val duration: Duration,
    val maxParticipants: Int = venue.capacity,
    private val registeredParticipants: MutableList<Participant> = mutableListOf()
)
{
    //This block of code ensures the event data is valid.
    init {
        // Ensures the ID is not blank.
        require(title.isNotBlank()) {"Title must not be blank."}
        //Ensures the ID is not blank.
        require(id.isNotBlank()) {"ID must not be blank."}
        //Ensures the event can hold at least one person.
        require(maxParticipants > 0) { "Max participants must be more than 0." }
        //Ensures the event's capacity doesn't exceed its venue's capacity.
        require(maxParticipants <= venue.capacity) {
            "Max participants ($maxParticipants) cannot exceed venue capacity (${venue.capacity})."
        }
        // Ensures the duration is a positive amount of time.
        require(!duration.isNegative && !duration.isZero) {
            "Duration must be positive."
        }
    }

    // Views the list of participants and returns a rad-only copy
     // This prevents code outside of the class from adding/removing participants

    fun getRegisteredParticipants(): List<Participant> =
        registeredParticipants.toList()

    // Tries to register a new participant for the event
    // Returns "true" if successful, and "false" if not
    fun registerParticipant(participant: Participant): Boolean {
        // Check capacity first (Part C requirement) using the helper function
        if (isFull()) {
            return false //Registration failed
        }

        // Check for duplicate registration using ID (more reliable)
        if (isParticipantRegistered(participant)) {
            return false //Registration failed
        }

        // Register participant, if the checks have passed.
        //Adds the participant to the private list.
        registeredParticipants.add(participant)
        return true //Registration successful
    }

    // Thia part of the code unregisters a participant.
    // Finds the participant by their ID.
    // Returns "true" if a participant was found and removed, "false " otherwise.
    fun unregisterParticipant(participant: Participant): Boolean {
        // Iterates through the list and checks the participant.
        // Removes any element where the lambda { } is true.
        return registeredParticipants.removeIf { it.id == participant.id }
    }


    //This function checks if the event is at maximum capacity
    fun isFull(): Boolean =
        registeredParticipants.size >= maxParticipants

    // This function gets the current number of registered participants
    fun getCurrentCapacity(): Int =
        registeredParticipants.size

    // This function finds out how many spots are left.
    fun getAvailableSpots(): Int =
        maxParticipants - registeredParticipants.size

    // Checks if the specific participant is already registered.
    // Returns "true" if any elemen in the list matches the condition.
    fun isParticipantRegistered(participant: Participant): Boolean =
        registeredParticipants.any { it.id == participant.id }

    // Calculates and returns the event's end time.
    // Crucial for conflict checking.
    fun getEndTime(): LocalDateTime =
        dateTime.plus(duration)


    //Checks if this even conflicts with another event.
    fun conflictsWith(other: Event): Boolean {
        // Different venues = no conflict
        if (this.venue.id != other.venue.id) {
            return false //No conflict
        }

        // Ge the end times for both events.
        val thisEnd = this.getEndTime()
        val otherEnd = other.getEndTime()

        // Check for time overlap:
        // Events conflict if one starts before the other ends
        return this.dateTime < otherEnd && thisEnd > other.dateTime
    }

    // Overrides the default .toString() to prove a more descriptive and readable string
    override fun toString(): String {
        // Makes a format hat is easy to read for the date and time
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        // Returns the custom string
        return "$title @ ${venue.name} on ${dateTime.format(formatter)} " +
                "(${getCurrentCapacity()}/$maxParticipants registered)"
    }

    //This part of the code overrides the default .equals() method.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true // It means they have the same object in memory.
        if (other !is Event) return false // It means it is not an Event object.
        return id == other.id // Checks if the IDs are the same.
    }

    // Overrides he default .hashCode() method.
    override fun hashCode(): Int {
        return id.hashCode()
    }
}