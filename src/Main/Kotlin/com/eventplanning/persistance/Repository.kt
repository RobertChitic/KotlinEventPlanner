package com.eventplanning.persistance

import com.eventplanning.domain.Event
import com.eventplanning.domain.Participant
import com.eventplanning.domain.Venue

/**
 * Interface defining the contract for data persistence.
 * This allows the underlying storage mechanism to be swapped
 * without affecting the rest of the application.
 */
interface Repository {
    fun connect()
    fun disconnect()
    fun initializeStorage() // e.g. create tables

    // Venues
    fun saveVenue(venue: Venue): Boolean
    fun loadAllVenues(): List<Venue>

    // Participants
    fun saveParticipant(participant: Participant): Boolean
    fun loadAllParticipants(): List<Participant>

    // Events
    fun saveEvent(event: Event): Boolean
    fun loadAllEvents(venueLookup: (String) -> Venue?, participantLookup: (String) -> Participant?): List<Event>
}