package com.eventplanning.domain

class EventManager {
    private val venues = mutableListOf<Venue>()
    private val events = mutableListOf<Event>()
    private val participants = mutableListOf<Participant>()

    fun addVenue(venue: Venue): Boolean {
        if (venues.any { it.id == venue.id }) return false
        venues.add(venue)
        return true
    }

    fun getAllVenues(): List<Venue> = venues.toList()

    fun getVenueById(id: String): Venue? = venues.find { it.id == id }

    fun addEvent(event: Event): Boolean {
        if (events.any { it.id == event.id }) return false
        events.add(event)
        return true
    }

    fun getAllEvents(): List<Event> = events.toList()

    fun getEventById(id: String): Event? = events.find { it.id == id }

    fun addParticipant(participant: Participant): Boolean {
        if (participants.any { it.id == participant.id }) return false

        if (participants.any { it.email.equals(participant.email, ignoreCase = true) }) {
            throw IllegalArgumentException("A participant with email ${participant.email} already exists")
        }

        participants.add(participant)
        return true
    }

    fun getAllParticipants(): List<Participant> = participants.toList()

    fun getParticipantById(id: String): Participant? = participants.find { it.id == id }

    fun registerParticipantToEvent(participantId: String, eventId: String): Boolean {
        val participant = getParticipantById(participantId) ?: return false
        val event = getEventById(eventId) ?: return false

        return event.registerParticipant(participant)
    }

    fun unregisterParticipantFromEvent(participantId: String, eventId: String): Boolean {
        val participant = getParticipantById(participantId) ?: return false
        val event = getEventById(eventId) ?: return false

        return event.unregisterParticipant(participant)
    }

    fun isParticipantRegistered(participantId: String, eventId: String): Boolean {
        val participant = getParticipantById(participantId) ?: return false
        val event = getEventById(eventId) ?: return false

        return event.isParticipantRegistered(participant)
    }

    fun getParticipantsForEvent(eventId: String): List<Participant> {
        val event = getEventById(eventId) ?: return emptyList()
        return event.getRegisteredParticipants()
    }

    fun getEventsForParticipant(participantId: String): List<Event> {
        val participant = getParticipantById(participantId) ?: return emptyList()

        return events.filter { event ->
            event.isParticipantRegistered(participant)
        }
    }

    fun getAvailableSpots(eventId: String): Int {
        val event = getEventById(eventId) ?: return 0
        return event.getAvailableSpots()
    }

    fun getRegistrationCount(eventId: String): Int {
        val event = getEventById(eventId) ?: return 0
        return event.getCurrentCapacity()
    }

    fun clearAll() {
        events.clear()
        participants.clear()
        venues.clear()
    }

    fun getStatistics(): Statistics {
        return Statistics(
            totalVenues = venues.size,
            totalEvents = events.size,
            totalParticipants = participants.size,
            totalRegistrations = events.sumOf { it.getCurrentCapacity() },
            eventsAtCapacity = events.count { it.isFull() }
        )
    }

    data class Statistics(
        val totalVenues: Int,
        val totalEvents: Int,
        val totalParticipants: Int,
        val totalRegistrations: Int,
        val eventsAtCapacity: Int
    )
}