package com.eventplanning.persistance

import com.eventplanning.domain.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter


private object SqlQueries {
    // venues table
    const val CREATE_VENUES = """
        CREATE TABLE IF NOT EXISTS Venues (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, capacity INTEGER NOT NULL,
            location TEXT NOT NULL, address TEXT NOT NULL, facilities TEXT
        )"""
    // participants table
    const val CREATE_PARTICIPANTS = """
        CREATE TABLE IF NOT EXISTS Participants (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE,
            phone TEXT, organization TEXT
        )"""
    // Events Table
    const val CREATE_EVENTS = """
        CREATE TABLE IF NOT EXISTS Events (
            id TEXT PRIMARY KEY, title TEXT NOT NULL, dateTime TEXT NOT NULL,
            venue_id TEXT NOT NULL, description TEXT, duration_minutes INTEGER NOT NULL,
            max_participants INTEGER NOT NULL, FOREIGN KEY (venue_id) REFERENCES Venues(id)
        )"""
    // Registration Table
    const val CREATE_REGISTRATIONS = """
        CREATE TABLE IF NOT EXISTS Event_Registrations (
            event_id TEXT, participant_id TEXT, registration_date TEXT NOT NULL,
            PRIMARY KEY (event_id, participant_id),
            FOREIGN KEY (event_id) REFERENCES Events(id) ON DELETE CASCADE,
            FOREIGN KEY (participant_id) REFERENCES Participants(id) ON DELETE CASCADE
        )"""

    const val INSERT_VENUE = "INSERT OR REPLACE INTO Venues (id, name, capacity, location, address, facilities) VALUES (?, ?, ?, ?, ?, ?)"
    const val SELECT_VENUES = "SELECT * FROM Venues"

    const val INSERT_PARTICIPANT = "INSERT OR REPLACE INTO Participants (id, name, email, phone, organization) VALUES (?, ?, ?, ?, ?)"
    const val SELECT_PARTICIPANTS = "SELECT * FROM Participants"

    const val INSERT_EVENT = "INSERT OR REPLACE INTO Events (id, title, dateTime, venue_id, description, duration_minutes, max_participants) VALUES (?, ?, ?, ?, ?, ?, ?)"
    const val DELETE_REGISTRATIONS = "DELETE FROM Event_Registrations WHERE event_id = ?"
    const val INSERT_REGISTRATION = "INSERT INTO Event_Registrations (event_id, participant_id, registration_date) VALUES (?, ?, ?)"
    const val SELECT_EVENTS = "SELECT * FROM Events"
    const val SELECT_REGISTRATIONS = "SELECT participant_id FROM Event_Registrations WHERE event_id = ?"
}

class DataStore : Repository {
    private val dbUrl = "jdbc:sqlite:events.db"
    private var connection: Connection? = null
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun connect() {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection(dbUrl)
            println("Connected to the database successfully.")
        } catch (e: Exception) {
            println("Error connecting to database: ${e.message}")
        }
    }

    override fun initializeStorage() {
        val conn = connection ?: return
        try {
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(SqlQueries.CREATE_VENUES)
                stmt.executeUpdate(SqlQueries.CREATE_PARTICIPANTS)
                stmt.executeUpdate(SqlQueries.CREATE_EVENTS)
                stmt.executeUpdate(SqlQueries.CREATE_REGISTRATIONS)
            }
            println("Tables created/verified successfully.")
        } catch (e: SQLException) {
            println("Error creating tables: ${e.message}")
        }
    }

    override fun saveVenue(venue: Venue): Boolean {
        val conn = connection ?: return false
        return try {
            conn.prepareStatement(SqlQueries.INSERT_VENUE).use { stmt ->
                stmt.setString(1, venue.id)
                stmt.setString(2, venue.name)
                stmt.setInt(3, venue.capacity)
                stmt.setString(4, venue.location)
                stmt.setString(5, venue.address)
                stmt.setString(6, venue.facilities.joinToString(","))
                stmt.executeUpdate()
                true
            }
        } catch (e: SQLException) {
            println("Error saving venue: ${e.message}"); false
        }
    }

    override fun loadAllVenues(): List<Venue> {
        val conn = connection ?: return emptyList()
        val venues = mutableListOf<Venue>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_VENUES)
                while (rs.next()) {
                    venues.add(Venue(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        capacity = rs.getInt("capacity"),
                        location = rs.getString("location"),
                        address = rs.getString("address"),
                        facilities = rs.getString("facilities")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    ))
                }
            }
        } catch (e: SQLException) { println("Error loading venues: ${e.message}") }
        return venues
    }

    override fun saveParticipant(participant: Participant): Boolean {
        val conn = connection ?: return false
        return try {
            conn.prepareStatement(SqlQueries.INSERT_PARTICIPANT).use { stmt ->
                stmt.setString(1, participant.id)
                stmt.setString(2, participant.name)
                stmt.setString(3, participant.email)
                stmt.setString(4, participant.phone)
                stmt.setString(5, participant.organization)
                stmt.executeUpdate()
                true
            }
        } catch (e: SQLException) { println("Error saving participant: ${e.message}"); false }
    }

    override fun loadAllParticipants(): List<Participant> {
        val conn = connection ?: return emptyList()
        val participants = mutableListOf<Participant>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_PARTICIPANTS)
                while (rs.next()) {
                    participants.add(Participant(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        email = rs.getString("email"),
                        phone = rs.getString("phone") ?: "",
                        organization = rs.getString("organization") ?: ""
                    ))
                }
            }
        } catch (e: SQLException) { println("Error loading participants: ${e.message}") }
        return participants
    }

    override fun saveEvent(event: Event): Boolean {
        val conn = connection ?: return false
        return try {
            conn.autoCommit = false // Transaction start

            // Save Event Details
            conn.prepareStatement(SqlQueries.INSERT_EVENT).use { stmt ->
                stmt.setString(1, event.id)
                stmt.setString(2, event.title)
                stmt.setString(3, event.dateTime.format(dateTimeFormatter))
                stmt.setString(4, event.venue.id)
                stmt.setString(5, event.description)
                stmt.setLong(6, event.duration.toMinutes())
                stmt.setInt(7, event.maxParticipants)
                stmt.executeUpdate()
            }

            // Clear old registrations
            conn.prepareStatement(SqlQueries.DELETE_REGISTRATIONS).use { stmt ->
                stmt.setString(1, event.id)
                stmt.executeUpdate()
            }

            //  Add current registrations
            conn.prepareStatement(SqlQueries.INSERT_REGISTRATION).use { stmt ->
                for (p in event.getRegisteredParticipants()) {
                    stmt.setString(1, event.id)
                    stmt.setString(2, p.id)
                    stmt.setString(3, LocalDateTime.now().format(dateTimeFormatter))
                    stmt.executeUpdate()
                }
            }

            conn.commit() // Transaction success
            conn.autoCommit = true
            true
        } catch (e: SQLException) {
            try { conn.rollback() } catch (ex: SQLException) { ex.printStackTrace() }
            println("Error saving event: ${e.message}")
            false
        }
    }

    override fun loadAllEvents(
        venueLookup: (String) -> Venue?,
        participantLookup: (String) -> Participant?
    ): List<Event> {
        val conn = connection ?: return emptyList()
        val events = mutableListOf<Event>()

        // 1. Load all Registrations into a Map in Memory first
        // Map<EventID, List<ParticipantID>>
        val registrationMap = HashMap<String, MutableList<String>>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT event_id, participant_id FROM Event_Registrations")
                while(rs.next()) {
                    val eId = rs.getString("event_id")
                    val pId = rs.getString("participant_id")
                    registrationMap.computeIfAbsent(eId) { mutableListOf() }.add(pId)
                }
            }
        } catch (e: SQLException) { println("Error pre-loading regs: ${e.message}") }

        // 2. Load Events and attach participants from map (No extra DB queries inside loop!)
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_EVENTS)
                while (rs.next()) {
                    val eventId = rs.getString("id")
                    val venueId = rs.getString("venue_id")
                    val venue = venueLookup(venueId)

                    if (venue != null) {
                        val event = Event(
                            id = eventId,
                            title = rs.getString("title"),
                            dateTime = LocalDateTime.parse(rs.getString("dateTime"), dateTimeFormatter),
                            venue = venue,
                            description = rs.getString("description") ?: "",
                            duration = Duration.ofMinutes(rs.getLong("duration_minutes")),
                            maxParticipants = rs.getInt("max_participants")
                        )

                        // Attach participants from our pre-loaded map
                        registrationMap[eventId]?.forEach { pId ->
                            participantLookup(pId)?.let { event.registerParticipant(it) }
                        }

                        events.add(event)
                    }
                }
            }
        } catch (e: SQLException) { println("Error loading events: ${e.message}") }
        return events

    }
    private fun loadEventRegistrations(event: Event, participantLookup: (String) -> Participant?) {
        val conn = connection ?: return
        try {
            conn.prepareStatement(SqlQueries.SELECT_REGISTRATIONS).use { stmt ->
                stmt.setString(1, event.id)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val pId = rs.getString("participant_id")
                    val participant = participantLookup(pId)
                    if (participant != null) event.registerParticipant(participant)
                }
            }
        } catch (e: SQLException) { println("Error loading regs: ${e.message}") }
    }

    override fun disconnect() {
        try { connection?.close() } catch (e: SQLException) { e.printStackTrace() }
    }
}