package com.eventplanning.domain

    class EventManger {
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
            participants.add(participant)
            return true
        }
        fun getAllParticipants(): List<Participant> = participants.toList()
        fun getParticipantById(id: String): Participant? = participants.find { it.id == id }

        fun clearAll() {
            events.clear()
            participants.clear()
            venues.clear()
        }
    }
