package com.eventplanning.domain

import com.eventplanning.persistance.Repository

class EventManager(private val repository: Repository) {
    private val venues = mutableListOf<Venue>()
    private val events = mutableListOf<Event>()
    private val participants = mutableListOf<Participant>()

    fun initializeData(): Boolean {
        return try {
            venues.clear()
            venues.addAll(repository.loadAllVenues())

            participants.clear()
            participants.addAll(repository.loadAllParticipants())

            events.clear()
            events.addAll(repository.loadAllEvents(
                venueLookup = { id -> getVenueById(id) },
                participantLookup = { id -> getParticipantById(id) }
            ))
            true
        } catch (e: Exception) {
            System.err.println("Error initializing data: ${e.message}")
            false
        }
    }

    fun saveAllData(): Boolean {
        var success = true
        venues.forEach { if (!repository.saveVenue(it)) success = false }
        participants.forEach { if (!repository.saveParticipant(it)) success = false }
        events.forEach { if (!repository.saveEvent(it)) success = false }
        return success
    }

    // --- VENUES ---
    fun addVenue(venue: Venue): Boolean {
        if (venues.any { it.id == venue.id }) return false
        if (!repository.saveVenue(venue)) return false
        venues.add(venue)
        return true
    }

    fun deleteVenue(venue: Venue): Boolean {
        if (events.any { it.venue.id == venue.id }) return false
        if (repository.deleteVenue(venue.id)) {
            venues.removeIf { it.id == venue.id }
            return true
        }
        return false
    }

    // --- PARTICIPANTS ---
    fun addParticipant(participant: Participant): Boolean {
        if (participants.any { it.id == participant.id }) return false
        if (!repository.saveParticipant(participant)) return false
        participants.add(participant)
        return true
    }

    fun deleteParticipant(participant: Participant): Boolean {
        if (repository.deleteParticipant(participant.id)) {
            participants.removeIf { it.id == participant.id }
            events.forEach { it.unregisterParticipant(participant) }
            return true
        }
        return false
    }

    // --- EVENTS ---
    fun addEvent(event: Event): Boolean {
        if (events.any { it.id == event.id }) return false

        // Check for conflicts with ANY event
        if (events.any { it.conflictsWith(event) }) return false

        if (!repository.saveEvent(event)) return false
        events.add(event)
        return true
    }

    /**
     * NEW: Modifies an existing event.
     * Checks for conflicts with OTHER events (excluding itself).
     */
    fun modifyEvent(updatedEvent: Event): Boolean {
        // 1. Validation: Ensure capacity isn't lower than current registrations
        if (updatedEvent.maxParticipants < updatedEvent.getCurrentCapacity()) {
            return false // Cannot reduce capacity below current participant count
        }

        // 2. Conflict Check: Check against everything EXCEPT the event we are updating
        val hasConflict = events
            .filter { it.id != updatedEvent.id } // Ignore self
            .any { it.conflictsWith(updatedEvent) }

        if (hasConflict) return false

        // 3. Save to DB
        if (!repository.saveEvent(updatedEvent)) return false

        // 4. Update Memory (Replace old object with new one)
        val index = events.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            events[index] = updatedEvent
        } else {
            events.add(updatedEvent) // Should not happen, but safe fallback
        }
        return true
    }

    fun updateEvent(event: Event): Boolean = repository.saveEvent(event)

    fun deleteEvent(event: Event): Boolean {
        if (repository.deleteEvent(event.id)) {
            events.removeIf { it.id == event.id }
            return true
        }
        return false
    }

    // --- READ ---
    fun getAllVenues(): List<Venue> = venues.toList()
    fun getVenueById(id: String): Venue? = venues.find { it.id == id }
    fun getAllEvents(): List<Event> = events.toList()
    fun getEventById(id: String): Event? = events.find { it.id == id }
    fun getAllParticipants(): List<Participant> = participants.toList()
    fun getParticipantById(id: String): Participant? = participants.find { it.id == id }
}