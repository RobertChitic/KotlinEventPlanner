package com.eventplanning.persistance

import com.eventplanning.domain.Event
import com.eventplanning.domain.Participant
import com.eventplanning.domain.Venue

/**
 * Repository interface defining all persistence operations.
 * Allows for easy swapping of persistence implementations (SQLite, PostgreSQL, File, etc.)
 */
interface Repository {
    /**
     * Establishes connection to the data store.
     */
    fun connect()

    /**
     * Closes connection to the data store.
     */
    fun disconnect()

    /**
     * Initializes the storage schema (creates tables, etc.)
     */
    fun initializeStorage()

    // ==================== VENUE OPERATIONS ====================
    fun saveVenue(venue: Venue): Boolean
    fun deleteVenue(id: String): Boolean
    fun loadAllVenues(): List<Venue>

    // ==================== PARTICIPANT OPERATIONS ====================
    fun saveParticipant(participant: Participant): Boolean
    fun deleteParticipant(id: String): Boolean
    fun loadAllParticipants(): List<Participant>

    // ==================== EVENT OPERATIONS ====================
    fun saveEvent(event: Event): Boolean
    fun deleteEvent(id: String): Boolean
    fun loadAllEvents(
        venueLookup: (String) -> Venue?,
        participantLookup: (String) -> Participant?
    ): List<Event>
}