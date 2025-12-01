package com.eventplanning.persistance

import com.eventplanning.domain.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * SQLite implementation of the Repository interface.
 * Handles all database operations with proper transaction support.
 *
 */
class DataStore(private val dbPath: String = "events.db") : Repository {

    private object SqlQueries {
        const val CREATE_VENUES = """
            CREATE TABLE IF NOT EXISTS Venues (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                capacity INTEGER NOT NULL,
                location TEXT NOT NULL,
                address TEXT NOT NULL,
                facilities TEXT
            )"""

        const val CREATE_PARTICIPANTS = """
            CREATE TABLE IF NOT EXISTS Participants (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                phone TEXT,
                organization TEXT
            )"""

        const val CREATE_EVENTS = """
            CREATE TABLE IF NOT EXISTS Events (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                dateTime TEXT NOT NULL,
                venue_id TEXT NOT NULL,
                description TEXT,
                duration_minutes INTEGER NOT NULL,
                max_participants INTEGER NOT NULL,
                FOREIGN KEY (venue_id) REFERENCES Venues(id)
            )"""

        const val CREATE_REGISTRATIONS = """
            CREATE TABLE IF NOT EXISTS Event_Registrations (
                event_id TEXT,
                participant_id TEXT,
                registration_date TEXT NOT NULL,
                PRIMARY KEY (event_id, participant_id),
                FOREIGN KEY (event_id) REFERENCES Events(id) ON DELETE CASCADE,
                FOREIGN KEY (participant_id) REFERENCES Participants(id) ON DELETE CASCADE
            )"""

        // Venue Queries
        const val INSERT_VENUE =
            "INSERT OR REPLACE INTO Venues (id, name, capacity, location, address, facilities) VALUES (?, ?, ?, ?, ?, ?)"
        const val SELECT_VENUES = "SELECT * FROM Venues"
        const val DELETE_VENUE = "DELETE FROM Venues WHERE id = ?"

        // Participant Queries
        const val INSERT_PARTICIPANT =
            "INSERT OR REPLACE INTO Participants (id, name, email, phone, organization) VALUES (?, ?, ?, ?, ?)"
        const val SELECT_PARTICIPANTS = "SELECT * FROM Participants"
        const val DELETE_PARTICIPANT = "DELETE FROM Participants WHERE id = ?"

        // Event Queries
        const val INSERT_EVENT =
            "INSERT OR REPLACE INTO Events (id, title, dateTime, venue_id, description, duration_minutes, max_participants) VALUES (?, ?, ?, ?, ?, ?, ? )"
        const val SELECT_EVENTS = "SELECT * FROM Events"
        const val DELETE_EVENT = "DELETE FROM Events WHERE id = ?"

        // Registration Queries
        const val DELETE_REGISTRATIONS = "DELETE FROM Event_Registrations WHERE event_id = ?"
        const val INSERT_REGISTRATION =
            "INSERT INTO Event_Registrations (event_id, participant_id, registration_date) VALUES (?, ?, ?)"
        const val SELECT_ALL_REGISTRATIONS = "SELECT event_id, participant_id FROM Event_Registrations"
    }

    private var connection: Connection? = null
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun connect() {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

            // Fix 1: Use .use {} to ensure Statement is closed, preventing resource leaks.
            connection?.createStatement()?.use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON;")
            }
            println("Database connected successfully: $dbPath")
        } catch (e: SQLException) {
            // Fix 3: Handle invalid paths gracefully
            System.err.println("Database Error: Could not open file at '$dbPath'. Check permissions or path validity.")
            throw IllegalArgumentException("Invalid database path or permissions issue: $dbPath", e)
        } catch (e: Exception) {
            System.err.println("Error connecting to database: ${e.message}")
            throw e
        }
    }

    override fun initializeStorage() {
        val conn = connection ?: throw IllegalStateException("Database not connected")
        try {
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(SqlQueries.CREATE_VENUES)
                stmt.executeUpdate(SqlQueries.CREATE_PARTICIPANTS)
                stmt.executeUpdate(SqlQueries.CREATE_EVENTS)
                stmt.executeUpdate(SqlQueries.CREATE_REGISTRATIONS)
            }
            println("Database schema initialized successfully.")
        } catch (e: SQLException) {
            System.err.println("Error creating tables: ${e.message}")
            throw e
        }
    }

    // ==================== VENUE OPERATIONS ====================

    @Synchronized // Fix 6 mitigation: Prevent concurrent writes
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
            System.err.println("Error saving venue: ${e.message}")
            false
        }
    }

    @Synchronized
    override fun deleteVenue(id: String): Boolean {
        val conn = connection ?: return false
        return try {
            conn.prepareStatement(SqlQueries.DELETE_VENUE).use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            System.err.println("Error deleting venue: ${e.message}")
            false
        }
    }

    override fun loadAllVenues(): List<Venue> {
        val conn = connection ?: return emptyList()
        val venues = mutableListOf<Venue>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_VENUES)
                while (rs.next()) {
                    venues.add(
                        Venue(
                            id = rs.getString("id"),
                            name = rs.getString("name"),
                            capacity = rs.getInt("capacity"),
                            location = rs.getString("location"),
                            address = rs.getString("address"),
                            // Fix 2: Trim strings to prevent " Wifi" bug
                            facilities = rs.getString("facilities")
                                ?.split(",")
                                ?.map { it.trim() }
                                ?.filter { it.isNotBlank() }
                                ?: emptyList()
                        )
                    )
                }
            }
        } catch (e: SQLException) {
            System.err.println("Error loading venues: ${e.message}")
        }
        return venues
    }

    // ==================== PARTICIPANT OPERATIONS ====================

    @Synchronized
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
        } catch (e: SQLException) {
            System.err.println("Error saving participant: ${e.message}")
            false
        }
    }

    @Synchronized
    override fun deleteParticipant(id: String): Boolean {
        val conn = connection ?: return false
        return try {
            conn.prepareStatement(SqlQueries.DELETE_PARTICIPANT).use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            System.err.println("Error deleting participant: ${e.message}")
            false
        }
    }

    override fun loadAllParticipants(): List<Participant> {
        val conn = connection ?: return emptyList()
        val participants = mutableListOf<Participant>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_PARTICIPANTS)
                while (rs.next()) {
                    participants.add(
                        Participant(
                            id = rs.getString("id"),
                            name = rs.getString("name"),
                            email = rs.getString("email"),
                            phone = rs.getString("phone") ?: "",
                            organization = rs.getString("organization") ?: ""
                        )
                    )
                }
            }
        } catch (e: SQLException) {
            System.err.println("Error loading participants: ${e.message}")
        }
        return participants
    }

    // ==================== EVENT OPERATIONS ====================

    @Synchronized
    override fun saveEvent(event: Event): Boolean {
        val conn = connection ?: return false
        val originalAutoCommit = conn.autoCommit

        return try {
            conn.autoCommit = false // Begin Transaction

            // Save event details
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

            // Clear existing registrations
            conn.prepareStatement(SqlQueries.DELETE_REGISTRATIONS).use { stmt ->
                stmt.setString(1, event.id)
                stmt.executeUpdate()
            }

            // Add current registrations
            conn.prepareStatement(SqlQueries.INSERT_REGISTRATION).use { stmt ->
                for (participant in event.getRegisteredParticipants()) {
                    stmt.setString(1, event.id)
                    stmt.setString(2, participant.id)
                    stmt.setString(3, LocalDateTime.now().format(dateTimeFormatter))
                    stmt.executeUpdate()
                }
            }

            conn.commit() // Commit Transaction
            true
        } catch (e: SQLException) {
            try {
                conn.rollback()
            } catch (rollbackEx: SQLException) {
                System.err.println("Rollback failed: ${rollbackEx.message}")
            }
            System.err.println("Error saving event: ${e.message}")
            false
        } finally {
            conn.autoCommit = originalAutoCommit
        }
    }

    @Synchronized
    override fun deleteEvent(id: String): Boolean {
        val conn = connection ?: return false
        return try {
            conn.prepareStatement(SqlQueries.DELETE_EVENT).use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            System.err.println("Error deleting event: ${e.message}")
            false
        }
    }

    override fun loadAllEvents(
        venueLookup: (String) -> Venue?,
        participantLookup: (String) -> Participant?
    ): List<Event> {
        val conn = connection ?: return emptyList()
        val events = mutableListOf<Event>()

        // Pre-load all registrations into memory for efficiency
        val registrationMap = HashMap<String, MutableList<String>>()
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_ALL_REGISTRATIONS)
                while (rs.next()) {
                    val eventId = rs.getString("event_id")
                    val participantId = rs.getString("participant_id")
                    registrationMap.computeIfAbsent(eventId) { mutableListOf() }.add(participantId)
                }
            }
        } catch (e: SQLException) {
            System.err.println("Error pre-loading registrations: ${e.message}")
        }

        // Load events and attach participants
        try {
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(SqlQueries.SELECT_EVENTS)
                while (rs.next()) {
                    val eventId = rs.getString("id")
                    val venueId = rs.getString("venue_id")
                    val maxParticipants = rs.getInt("max_participants")
                    var venue = venueLookup(venueId)

                    // Fix 4: Handle Orphaned Events (Missing Venue)
                    // If the venue is missing (deleted directly from DB?), use a placeholder
                    // so the event still loads and can be managed/deleted by the user.
                    if (venue == null) {
                        System.err.println("Warning: Event '$eventId' references missing venue '$venueId'. Loading as placeholder.")
                        venue = Venue(
                            id = "unknown_venue",
                            name = "UNKNOWN VENUE (Missing Data)",
                            capacity = maxParticipants, // Fix: Use maxParticipants to prevent Event constructor validation failure
                            location = "Unknown",
                            address = "Unknown"
                        )
                    }

                    val event = Event(
                        id = eventId,
                        title = rs.getString("title") + (if (venue.id == "unknown_venue") " [DATA ERROR]" else ""),
                        dateTime = LocalDateTime.parse(rs.getString("dateTime"), dateTimeFormatter),
                        venue = venue,
                        description = rs.getString("description") ?: "",
                        duration = Duration.ofMinutes(rs.getLong("duration_minutes")),
                        maxParticipants = maxParticipants
                    )

                    // Attach participants from pre-loaded map
                    registrationMap[eventId]?.forEach { participantId ->
                        participantLookup(participantId)?.let { participant ->
                            event.registerParticipant(participant)
                        }
                    }

                    events.add(event)
                }
            }
        } catch (e: SQLException) {
            System.err.println("Error loading events: ${e.message}")
        }
        return events
    }

    override fun disconnect() {
        try {
            connection?.close()
            println("Database disconnected.")
        } catch (e: SQLException) {
            System.err.println("Error disconnecting: ${e.message}")
        }
    }
}