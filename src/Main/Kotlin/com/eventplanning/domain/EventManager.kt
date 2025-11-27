package com.eventplanning.domain

import com.eventplanning.persistance.Repository

/**
 * Central manager for all domain operations.
 * Coordinates between the repository and in-memory state.
 * Implements a "Database First, then Memory" transactional pattern.
 */
class EventManager(private val repository: Repository) {
    private val venues = mutableListOf<Venue>()
    private val events = mutableListOf<Event>()
    private val participants = mutableListOf<Participant>()

    /**
     * Loads all data from the repository into memory.
     * @return true if successful, false otherwise
     */
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

    /**
     * Saves all in-memory data to the repository.
     * @return true if all saves successful, false if any failed
     */
    fun saveAllData(): Boolean {
        var success = true
        venues.forEach { if (! repository.saveVenue(it)) success = false }
        participants.forEach { if (!repository.saveParticipant(it)) success = false }
        events.forEach { if (!repository. saveEvent(it)) success = false }
        return success
    }

    // ==================== VENUE OPERATIONS ====================

    /**
     * Adds a new venue to the system.
     * @return true if successful, false if ID already exists or save failed
     */
    fun addVenue(venue: Venue): Boolean {
        if (venues.any { it. id == venue.id }) return false
        if (! repository.saveVenue(venue)) return false
        venues.add(venue)
        return true
    }

    /**
     * Deletes a venue from the system.
     * @return true if successful, false if venue is used by events or delete failed
     */
    fun deleteVenue(venue: Venue): Boolean {
        if (events.any { it. venue.id == venue.id }) {
            return false // Cannot delete venue if events depend on it
        }

        if (repository.deleteVenue(venue.id)) {
            venues. removeIf { it. id == venue.id }
            return true
        }
        return false
    }

    // ==================== PARTICIPANT OPERATIONS ====================

    /**
     * Adds a new participant to the system.
     * @return true if successful, false if ID already exists or save failed
     */
    fun addParticipant(participant: Participant): Boolean {
        if (participants.any { it.id == participant. id }) return false
        if (!repository. saveParticipant(participant)) return false
        participants.add(participant)
        return true
    }

    /**
     * Deletes a participant from the system.
     * Also removes them from any events they were registered to.
     * @return true if successful, false if delete failed
     */
    fun deleteParticipant(participant: Participant): Boolean {
        if (repository.deleteParticipant(participant.id)) {
            participants.removeIf { it.id == participant.id }
            events.forEach { it.unregisterParticipant(participant) }
            return true
        }
        return false
    }

    // ==================== EVENT OPERATIONS ====================

    /**
     * Adds a new event to the system.
     * Validates for conflicts before saving.
     * @return true if successful, false if ID exists, conflicts detected, or save failed
     */
    fun addEvent(event: Event): Boolean {
        if (events. any { it.id == event.id }) return false

        val hasConflict = events.any { existingEvent ->
            event.conflictsWith(existingEvent)
        }

        if (hasConflict) return false

        if (! repository.saveEvent(event)) return false

        events.add(event)
        return true
    }

    /**
     * Updates an existing event in the repository.
     * Used primarily for updating registrations.
     * @return true if save successful, false otherwise
     */
    fun updateEvent(event: Event): Boolean {
        return repository.saveEvent(event)
    }

    /**
     * Deletes an event from the system.
     * @return true if successful, false if delete failed
     */
    fun deleteEvent(event: Event): Boolean {
        if (repository.deleteEvent(event.id)) {
            events.removeIf { it. id == event.id }
            return true
        }
        return false
    }

    // ==================== READ OPERATIONS ====================

    fun getAllVenues(): List<Venue> = venues. toList()
    fun getVenueById(id: String): Venue?  = venues.find { it.id == id }
    fun getAllEvents(): List<Event> = events.toList()
    fun getEventById(id: String): Event?  = events.find { it.id == id }
    fun getAllParticipants(): List<Participant> = participants.toList()
    fun getParticipantById(id: String): Participant? = participants.find { it. id == id }

    /**
     * Returns events sorted by date/time.
     */
    fun getEventsSortedByDate(): List<Event> = events.sortedBy { it.dateTime }

    /**
     * Returns events at a specific venue.
     */
    fun getEventsByVenue(venueId: String): List<Event> =
        events.filter { it.venue. id == venueId }
}