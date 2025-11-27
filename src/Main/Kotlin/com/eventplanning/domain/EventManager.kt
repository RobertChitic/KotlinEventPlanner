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
            // We pass functions to look up venues/participants to keep DataStore decoupled from Manager
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
        // In a real app, we'd save incrementally, but for this assignment:
        var success = true
        venues.forEach { if (!repository.saveVenue(it)) success = false }
        participants.forEach { if (!repository.saveParticipant(it)) success = false }
        events.forEach { if (!repository.saveEvent(it)) success = false }
        return success
    }

    //Transactional Methods: DB First, then Memory

    fun addVenue(venue: Venue): Boolean {
        if (venues.any { it.id == venue.id }) return false

        // 1. Try to save to DB
        if (!repository.saveVenue(venue)) return false

        // 2. If successful, add to memory
        venues.add(venue)
        return true
    }

    fun addParticipant(participant: Participant): Boolean {
        if (participants.any { it.id == participant.id }) return false

        if (!repository.saveParticipant(participant)) return false

        participants.add(participant)
        return true
    }

    fun addEvent(event: Event): Boolean {
        if (events.any { it.id == event.id }) return false

        if (!repository.saveEvent(event)) return false

        events.add(event)
        return true
    }

    fun updateEvent(event: Event): Boolean {
        // Used for registrations/modifications
        if (!repository.saveEvent(event)) return false
        // Memory reference is likely already updated if object was modified directly,
        // but this ensures DB is in sync
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