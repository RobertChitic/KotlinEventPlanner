package com.eventplanning.persistance

import com.eventplanning.domain.Event
import com.eventplanning.domain.Participant
import com.eventplanning.domain.Venue

interface Repository {
    fun connect()
    fun disconnect()
    fun initializeStorage()

    // Venues
    fun saveVenue(venue: Venue): Boolean
    fun deleteVenue(id: String): Boolean
    fun loadAllVenues(): List<Venue>

    // Participants
    fun saveParticipant(participant: Participant): Boolean
    fun deleteParticipant(id: String): Boolean
    fun loadAllParticipants(): List<Participant>

    // Events
    fun saveEvent(event: Event): Boolean
    fun deleteEvent(id: String): Boolean
    fun loadAllEvents(venueLookup: (String) -> Venue?, participantLookup: (String) -> Participant?): List<Event>
}