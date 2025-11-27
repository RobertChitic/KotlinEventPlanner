package com.eventplanning.domain

import com.eventplanning.persistance.Repository

class EventManager(private val repository: Repository) {
    private val venues = mutableListOf<Venue>()
    private val events = mutableListOf<Event>()
    private val participants = mutableListOf<Participant>()

    // Load all data on startup
    fun initializeData(): Boolean {
        try {
            venues.clear()
            venues.addAll(repository.loadAllVenues())

            participants.clear()
            participants.addAll(repository.loadAllParticipants())

            events.clear()
            events.addAll(repository.loadAllEvents(
                venueLookup = { id -> getVenueById(id) },
                participantLookup = { id -> getParticipantById(id) }
            ))
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun saveAllData(): Boolean {
        var success = true
        venues.forEach { if (!repository.saveVenue(it)) success = false }
        participants.forEach { if (!repository.saveParticipant(it)) success = false }
        events.forEach { if (!repository.saveEvent(it)) success = false }
        return success
    }

    //Transactional Methods: DB First, then Memory

    fun addVenue(venue: Venue): Boolean {
        if (venues.any { it.id == venue.id }) return false
        if (!repository.saveVenue(venue)) return false
        venues.add(venue)
        return true
    }

    fun addParticipant(participant: Participant): Boolean {
        if (participants.any { it.id == participant.id }) return false
        if (!repository.saveParticipant(participant)) return false
        participants.add(participant)
        return true
    }

    // === UPDATED METHOD WITH CONFLICT CHECK ===
    fun addEvent(event: Event): Boolean {
        // 1. Check if ID exists
        if (events.any { it.id == event.id }) return false

        // 2. CRITICAL FIX: Check for conflicts with existing events
        // If ANY existing event conflicts with the new one, reject it.
        val hasConflict = events.any { existingEvent ->
            event.conflictsWith(existingEvent)
        }

        if (hasConflict) {
            // Ideally, you would throw an exception here to tell the UI *why* it failed,
            // but returning false is the minimum requirement.
            return false
        }

        // 3. Try to save to DB
        if (!repository.saveEvent(event)) return false

        // 4. If successful, add to memory
        events.add(event)
        return true
    }

    fun updateEvent(event: Event): Boolean {
        if (!repository.saveEvent(event)) return false
        return true
    }

    // Read-only methods remain the same
    fun getAllVenues(): List<Venue> = venues.toList()
    fun getVenueById(id: String): Venue? = venues.find { it.id == id }
    fun getAllEvents(): List<Event> = events.toList()
    fun getEventById(id: String): Event? = events.find { it.id == id }
    fun getAllParticipants(): List<Participant> = participants.toList()
    fun getParticipantById(id: String): Participant? = participants.find { it.id == id }
}