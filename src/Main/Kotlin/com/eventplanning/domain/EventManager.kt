package com.eventplanning.domain

    // Creates a class that manages all the data for the event planning application.
    // Holds the lists of all venues, events and participants
    class EventManager {
        // A private, mutable list to store all Venue objects.
        private val venues = mutableListOf<Venue>()
        // A private, mutable list to store all Event objects
        private val events = mutableListOf<Event>()
        // A private, mutable list to store all Participant objects
        private val participants = mutableListOf<Participant>()

        // Adds a new venue to the "venues" list
        // Returns "true" if successful, "false" if a venue with that ID already exists
        fun addVenue(venue: Venue): Boolean {
            // This line prevents adding duplicate venues based on their ID.
            if (venues.any { it.id == venue.id }) return false
            // If no duplicate is found, add the venue to the list.
            venues.add(venue)
            // Return true to indicate success
            return true
        }
        // Returns a read-only copy of the "venues" list
        fun getAllVenues(): List<Venue> = venues.toList()
        // Finds a single venue by its ID
        // Returns the firs venue that matches the condition
        // Returns "null" if none match
        fun getVenueById(id: String): Venue? = venues.find { it.id == id }

        // Adds a new event to the "events" list.
        // Returns "true" if successful.
        // Returns "false" if an event with that ID already exists.
        fun addEvent(event: Event): Boolean {
            // Checks for a duplicate event ID.
            if (events.any { it.id == event.id }) return false
            // Adds the new event to the list
            events.add(event)
            return true
        }
        // Returns a read-only copy of the "events" list.
        fun getAllEvents(): List<Event> = events.toList()
        // Finds a single event by its ID or returns "null if not found.
        fun getEventById(id: String): Event? = events.find { it.id == id }

        // Adds a new participant to the "participants" list
        // Returns "true" if successful.
        // Returns "false" if a participant with that ID already exists.
        fun addParticipant(participant: Participant): Boolean {
            // Checks for a duplicate participant ID.
            if (participants.any { it.id == participant.id }) return false
            // Adds the new participant to the list.
            participants.add(participant)
            return true
        }
        // Returns a read-only copy of the "participants" list.
        fun getAllParticipants(): List<Participant> = participants.toList()
        // Finds a single participant by its ID or returns "null if not found.
        fun getParticipantById(id: String): Participant? = participants.find { it.id == id }

        // This function will clear all data from the manager.
        // Will be used for testing, in order to reset the state between tests.
        fun clearAll() {
            events.clear()
            participants.clear()
            venues.clear()
        }
    }
