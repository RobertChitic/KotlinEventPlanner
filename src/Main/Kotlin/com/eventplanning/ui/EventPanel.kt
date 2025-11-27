package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import java.awt.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class EventPanel(
    private val eventManager: EventManager
) : JPanel() {

    private val eventListModel = DefaultListModel<String>()
    private val eventList = JList(eventListModel)
    private val titleField = JTextField(25)

    // Start Time
    private val startDateSpinner = JSpinner(SpinnerDateModel())

    // Duration Spinners
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))

    private val venueCombo = JComboBox<VenueItem>()
    private val descriptionArea = JTextArea(3, 25)
    private val maxParticipantsField = JTextField(10)
    private val createButton = JButton("Create Event")

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(createFormPanel(), BorderLayout.NORTH)
        add(createListPanel(), BorderLayout.CENTER)
        refreshEventList()
        refreshVenueCombo()
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Create New Event")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST

        // 1. Title
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Event Title:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(titleField, gbc)

        // 2. Start Time
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Start Time:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        val startEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm")
        startDateSpinner.editor = startEditor
        startDateSpinner.value = Date()
        panel.add(startDateSpinner, gbc)

        // 3. Duration
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Duration:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL

        val durationPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        durationPanel.add(hoursSpinner)
        durationPanel.add(JLabel("hrs"))
        durationPanel.add(minutesSpinner)
        durationPanel.add(JLabel("mins"))

        panel.add(durationPanel, gbc)

        // 4. Venue
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Venue:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(venueCombo, gbc)

        // 5. Max Participants
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Max Participants:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(maxParticipantsField, gbc)

        // 6. Description
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Description:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        panel.add(JScrollPane(descriptionArea), gbc)

        // 7. Buttons
        gbc.gridx = 1; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val findVenueBtn = JButton("🔍 Find Available Venue")
        findVenueBtn.addActionListener { findAvailableVenue() }
        buttonPanel.add(findVenueBtn)

        val refreshVenuesBtn = JButton("Refresh Venues")
        refreshVenuesBtn.addActionListener { refreshVenueCombo() }
        buttonPanel.add(refreshVenuesBtn)

        createButton.addActionListener { createEvent() }
        buttonPanel.add(createButton)

        panel.add(buttonPanel, gbc)
        return panel
    }

    private fun createListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Scheduled Events")
        eventList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scrollPane = JScrollPane(eventList)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    private fun createEvent() {
        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as? VenueItem
        val description = descriptionArea.text.trim()
        val maxParticipants = maxParticipantsField.text.toIntOrNull()

        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter title", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (venueItem == null) {
            JOptionPane.showMessageDialog(this, "Please select venue", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        // === CHANGED: Strict Validation for Max Participants ===
        if (maxParticipants == null || maxParticipants <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Max Participants.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        // Check logical constraint immediately (User experience)
        if (maxParticipants > venueItem.venue.capacity) {
            JOptionPane.showMessageDialog(this, "Max Participants cannot exceed venue capacity (${venueItem.venue.capacity}).", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        // =======================================================

        // === CALCULATE DURATION ===
        val hours = hoursSpinner.value as Int
        val minutes = minutesSpinner.value as Int

        if (hours == 0 && minutes == 0) {
            JOptionPane.showMessageDialog(this, "Duration must be at least 1 minute.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        val duration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())

        val startObj = startDateSpinner.value as Date
        val startDateTime = LocalDateTime.ofInstant(startObj.toInstant(), ZoneId.systemDefault())

        // Calculate End Time for display
        val endDateTime = startDateTime.plus(duration)

        // === CONFIRMATION DIALOG ===
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val endsNextDay = endDateTime.toLocalDate().isAfter(startDateTime.toLocalDate())
        val endDateString = if(endsNextDay) " (Next Day!)" else ""

        val message = """
            Please confirm your Event details:
            
            Title:       $title
            Venue:       ${venueItem.venue.name}
            Start:       ${startDateTime.format(dateTimeFormatter)}
            Duration:    $hours hrs $minutes mins
            Ends at:     ${endDateTime.format(timeOnlyFormatter)}$endDateString
            Capacity:    $maxParticipants people
            
            Is this correct?
        """.trimIndent()

        val choice = JOptionPane.showConfirmDialog(
            this,
            message,
            "Confirm Event Creation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        if (choice != JOptionPane.YES_OPTION) {
            return // User cancelled
        }
        // =================================

        try {
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTime = startDateTime,
                venue = venueItem.venue,
                description = description,
                duration = duration,
                maxParticipants = maxParticipants // Now guaranteed to be non-null
            )

            // Conflict Check
            val conflict = eventManager.getAllEvents().find { it.conflictsWith(event) }
            if (conflict != null) {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                JOptionPane.showMessageDialog(
                    this,
                    "Venue '${event.venue.name}' is already booked at this time by event:\n" +
                            "'${conflict.title}' (${conflict.dateTime.format(formatter)} - ${conflict.getEndTime().format(formatter)})",
                    "Scheduling Conflict",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            createButton.isEnabled = false
            createButton.text = "Saving..."

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean {
                    return eventManager.addEvent(event)
                }

                override fun done() {
                    createButton.isEnabled = true
                    createButton.text = "Create Event"
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(this@EventPanel, "Event Created & Saved!", "Success", JOptionPane.INFORMATION_MESSAGE)
                            clearFields()
                            refreshEventList()
                        } else {
                            JOptionPane.showMessageDialog(this@EventPanel, "Failed to save event.", "Error", JOptionPane.ERROR_MESSAGE)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            worker.execute()

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshEventList() {
        eventListModel.clear()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        eventManager.getAllEvents().forEach { event ->
            val endFormatted = event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            eventListModel.addElement(
                "${event.title} | ${event.dateTime.format(formatter)} - $endFormatted | @ ${event.venue.name}"
            )
        }
    }

    private fun refreshVenueCombo() {
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venue -> venueCombo.addItem(VenueItem(venue)) }
    }

    private fun clearFields() {
        titleField.text = ""
        startDateSpinner.value = Date()
        hoursSpinner.value = 2 // Reset to default
        minutesSpinner.value = 0
        descriptionArea.text = ""
        maxParticipantsField.text = ""
    }

    private data class VenueItem(val venue: Venue) {
        override fun toString(): String = "${venue.name} (Capacity: ${venue.capacity})"
    }

    private fun findAvailableVenue() {
        try {
            // 1. Get Capacity
            val requiredCapacity = maxParticipantsField.text.toIntOrNull() ?: run {
                JOptionPane.showMessageDialog(this, "Enter required capacity first", "Info", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            // 2. Get Duration (NEW)
            val hours = hoursSpinner.value as Int
            val minutes = minutesSpinner.value as Int

            if (hours == 0 && minutes == 0) {
                JOptionPane.showMessageDialog(this, "Please set a duration (at least 1 min) to check availability.", "Info", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            val duration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())

            // 3. Get Date
            val selectedDate = startDateSpinner.value as Date
            val proposedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault())

            // 4. Get Data
            val venues = eventManager.getAllVenues()
            val events = eventManager.getAllEvents()

            // 5. Call Bridge with Duration (UPDATED)
            val availableVenues = ScalaBridge.findAvailableVenues(
                venues,
                events,
                requiredCapacity,
                proposedDateTime,
                duration
            )

            // 6. Handle Results
            if (availableVenues.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No venues found for this date, time, and duration.", "Info", JOptionPane.INFORMATION_MESSAGE)
            } else {
                val message = buildString {
                    appendLine("Found ${availableVenues.size} available venue(s):")
                    availableVenues.forEach { appendLine("- ${it.name} (Cap: ${it.capacity})") }
                }
                JOptionPane.showMessageDialog(this, message, "Results", JOptionPane.INFORMATION_MESSAGE)

                // Auto-select the first one found
                val first = availableVenues[0]
                for (i in 0 until venueCombo.itemCount) {
                    if (venueCombo.getItemAt(i).venue.id == first.id) {
                        venueCombo.selectedIndex = i
                        break
                    }
                }
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Bridge Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}