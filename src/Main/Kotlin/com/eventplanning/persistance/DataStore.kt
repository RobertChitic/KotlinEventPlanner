package com.eventplanning.persistance

import com.eventplanning.domain.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter


class DataStore {
    private val dbUrl = "jdbc:sqlite:events.db"
    private var connection: Connection? = null
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun connectToDatabase() {
        try {
            // 1. Explicitly load the driver class
            Class.forName("org.sqlite.JDBC")

            // 2. Establish connection
            connection = DriverManager.getConnection(dbUrl)
            println("Connected to the database successfully.")

        } catch (e: ClassNotFoundException) {
            println("Error: SQLite Driver not found. Please add the JAR to your library.")
            e.printStackTrace()
        } catch (e: SQLException) {
            println("Error connecting to the database: ${e.message}")
        }
    }
    fun createTables() {
        val conn = connection
        if (conn == null || conn.isClosed) {
            println("Connection is not established. Please connect to the database first.")
            return
        }

        conn.createStatement().use { statement ->
            try {
                // Venues table
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Venues (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        capacity INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        address TEXT NOT NULL,
                        facilities TEXT
                    )
                """.trimIndent())

                // Participants table
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Participants (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        phone TEXT,
                        organization TEXT
                    )
                """.trimIndent())

                // Events table
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Events (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        dateTime TEXT NOT NULL,
                        venue_id TEXT NOT NULL,
                        description TEXT,
                        duration_minutes INTEGER NOT NULL,
                        max_participants INTEGER NOT NULL,
                        FOREIGN KEY (venue_id) REFERENCES Venues(id)
                    )
                """.trimIndent())

                // Event registrations table
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Event_Registrations (
                        event_id TEXT,
                        participant_id TEXT,
                        registration_date TEXT NOT NULL,
                        PRIMARY KEY (event_id, participant_id),
                        FOREIGN KEY (event_id) REFERENCES Events(id) ON DELETE CASCADE,
                        FOREIGN KEY (participant_id) REFERENCES Participants(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                println("Tables created successfully!")
            } catch (e: SQLException) {
                println("Error creating tables: ${e.message}")
            }
        }
    }

    // ============== SAVE OPERATIONS ==============

    fun saveVenue(venue: Venue): Boolean {
        val conn = connection ?: return false

        return try {
            val sql = """
                INSERT OR REPLACE INTO Venues (id, name, capacity, location, address, facilities)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
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
            println("Error saving venue: ${e.message}")
            false
        }
    }

    fun saveParticipant(participant: Participant): Boolean {
        val conn = connection ?: return false

        return try {
            val sql = """
                INSERT OR REPLACE INTO Participants (id, name, email, phone, organization)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, participant.id)
                stmt.setString(2, participant.name)
                stmt.setString(3, participant.email)
                stmt.setString(4, participant.phone)
                stmt.setString(5, participant.organization)
                stmt.executeUpdate()
                true
            }
        } catch (e: SQLException) {
            println("Error saving participant: ${e.message}")
            false
        }
    }

    fun saveEvent(event: Event): Boolean {
        val conn = connection ?: return false

        return try {
            // Save the event
            val eventSql = """
                INSERT OR REPLACE INTO Events (id, title, dateTime, venue_id, description, duration_minutes, max_participants)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(eventSql).use { stmt ->
                stmt.setString(1, event.id)
                stmt.setString(2, event.title)
                stmt.setString(3, event.dateTime.format(dateTimeFormatter))
                stmt.setString(4, event.venue.id)
                stmt.setString(5, event.description)
                stmt.setLong(6, event.duration.toMinutes())
                stmt.setInt(7, event.maxParticipants)
                stmt.executeUpdate()
            }

            // Delete existing registrations
            val deleteRegSql = "DELETE FROM Event_Registrations WHERE event_id = ?"
            conn.prepareStatement(deleteRegSql).use { stmt ->
                stmt.setString(1, event.id)
                stmt.executeUpdate()
            }

            // Save current registrations
            val regSql = """
                INSERT INTO Event_Registrations (event_id, participant_id, registration_date)
                VALUES (?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(regSql).use { stmt ->
                for (participant in event.getRegisteredParticipants()) {
                    stmt.setString(1, event.id)
                    stmt.setString(2, participant.id)
                    stmt.setString(3, LocalDateTime.now().format(dateTimeFormatter))
                    stmt.executeUpdate()
                }
            }

            true
        } catch (e: SQLException) {
            println("Error saving event: ${e.message}")
            false
        }
    }

    // ============== LOAD OPERATIONS ==============

    fun loadAllVenues(): List<Venue> {
        val conn = connection ?: return emptyList()
        val venues = mutableListOf<Venue>()

        try {
            val sql = "SELECT * FROM Venues"
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                while (rs.next()) {
                    venues.add(
                        Venue(
                            id = rs.getString("id"),
                            name = rs.getString("name"),
                            capacity = rs.getInt("capacity"),
                            location = rs.getString("location"),
                            address = rs.getString("address"),
                            facilities = rs.getString("facilities")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        )
                    )
                }
            }
        } catch (e: SQLException) {
            println("Error loading venues: ${e.message}")
        }

        return venues
    }

    fun loadAllParticipants(): List<Participant> {
        val conn = connection ?: return emptyList()
        val participants = mutableListOf<Participant>()

        try {
            val sql = "SELECT * FROM Participants"
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
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
            println("Error loading participants: ${e.message}")
        }

        return participants
    }

    fun loadAllEvents(eventManager: EventManager): List<Event> {
        val conn = connection ?: return emptyList()
        val events = mutableListOf<Event>()

        try {
            val sql = "SELECT * FROM Events"
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                while (rs.next()) {
                    val venueId = rs.getString("venue_id")
                    val venue = eventManager.getVenueById(venueId)

                    if (venue != null) {
                        val event = Event(
                            id = rs.getString("id"),
                            title = rs.getString("title"),
                            dateTime = LocalDateTime.parse(rs.getString("dateTime"), dateTimeFormatter),
                            venue = venue,
                            description = rs.getString("description") ?: "",
                            duration = Duration.ofMinutes(rs.getLong("duration_minutes")),
                            maxParticipants = rs.getInt("max_participants")
                        )

                        // Load registrations for this event
                        loadEventRegistrations(event, eventManager)

                        events.add(event)
                    }
                }
            }
        } catch (e: SQLException) {
            println("Error loading events: ${e.message}")
        }

        return events
    }

    private fun loadEventRegistrations(event: Event, eventManager: EventManager) {
        val conn = connection ?: return

        try {
            val sql = "SELECT participant_id FROM Event_Registrations WHERE event_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.id)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val participantId = rs.getString("participant_id")
                    val participant = eventManager.getParticipantById(participantId)
                    if (participant != null) {
                        event.registerParticipant(participant)
                    }
                }
            }
        } catch (e: SQLException) {
            println("Error loading event registrations: ${e.message}")
        }
    }

    // ============== BATCH OPERATIONS ==============

    /**
     * Save all data from EventManager to database
     */
    fun saveAll(eventManager: EventManager): Boolean {
        var success = true
        var savedCount = 0

        // Save all venues
        eventManager.getAllVenues().forEach { venue ->
            if (saveVenue(venue)) savedCount++ else success = false
        }

        // Save all participants
        eventManager.getAllParticipants().forEach { participant ->
            if (saveParticipant(participant)) savedCount++ else success = false
        }

        // Save all events (includes registrations)
        eventManager.getAllEvents().forEach { event ->
            if (saveEvent(event)) savedCount++ else success = false
        }

        if (success) {
            println("Successfully saved all data ($savedCount items)")
        } else {
            println("Some items failed to save")
        }

        return success
    }

    /**
     * Load all data from database into EventManager
     */
    fun loadAll(eventManager: EventManager): Boolean {
        try {
            println("Loading data from database...")

            // Clear existing data
            eventManager.clearAll()

            // Load in dependency order
            val venues = loadAllVenues()
            venues.forEach { eventManager.addVenue(it) }
            println("Loaded ${venues.size} venues")

            val participants = loadAllParticipants()
            participants.forEach { eventManager.addParticipant(it) }
            println("Loaded ${participants.size} participants")

            val events = loadAllEvents(eventManager)
            events.forEach { eventManager.addEvent(it) }
            println("Loaded ${events.size} events")

            println("Data loaded successfully!")
            return true
        } catch (e: Exception) {
            println("Error loading data: ${e.message}")
            return false
        }
    }

    fun isConnected(): Boolean {
        return connection?.isClosed == false
    }

    fun closeConnection() {
        try {
            connection?.close()
            println("Connection closed successfully.")
        } catch (e: SQLException) {
            println("Error closing the connection: ${e.message}")
        }
    }
}