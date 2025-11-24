package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.persistence.DataStore
/* import com.eventplanning.scheduling.SlotFinder */
import javax.swing.*
import java.awt.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class EventPanel(
    private val eventManager: EventManager,
    private val dataStore: DataStore
) : JPanel() {
    private val eventListModel = DefaultListModel<String>()
    private val eventList = JList(eventListModel)

    private val titleField = JTextField(25)
    private val dateField = JTextField(16)
    private val venueCombo = JComboBox<VenueItem>()
    private val descriptionArea = JTextArea(3, 25)
    private val maxParticipantsField = JTextField(10)

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

        // Title
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Event Title:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(titleField, gbc)

        // Date/Time
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Date/Time:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        dateField.toolTipText = "Format: yyyy-MM-dd HH:mm (e.g., 2025-12-25 14:30)"
        panel.add(dateField, gbc)

        // Venue
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Venue:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(venueCombo, gbc)

        // Max Participants
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Max Participants:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(maxParticipantsField, gbc)

        // Description
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Description:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        panel.add(JScrollPane(descriptionArea), gbc)

        // Buttons
        gbc.gridx = 1; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
/**
// Add "Find Available Venue" button (Part E - Scala)
        val findVenueBtn = JButton("🔍 Find Available Venue")
        findVenueBtn.toolTipText = "Use Scala algorithm to find available venues"
        findVenueBtn.addActionListener { findAvailableVenue() }
        buttonPanel.add(findVenueBtn)
*/

        val refreshVenuesBtn = JButton("Refresh Venues")
        refreshVenuesBtn.addActionListener { refreshVenueCombo() }
        buttonPanel.add(refreshVenuesBtn)

        val addButton = JButton("Create Event")
        addButton.addActionListener { createEvent() }
        buttonPanel.add(addButton)

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
        try {
            val title = titleField.text.trim()
            val dateTimeStr = dateField.text.trim()
            val venueItem = venueCombo.selectedItem as? VenueItem
            val description = descriptionArea.text.trim()
            val maxParticipants = maxParticipantsField.text.toIntOrNull()

            if (venueItem == null) {
                JOptionPane.showMessageDialog(this,
                    "Please select a venue",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE)
                return
            }

            val dateTime = LocalDateTime.parse(dateTimeStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTime = dateTime,
                venue = venueItem.venue,
                description = description,
                maxParticipants = maxParticipants ?: venueItem.venue.capacity
            )

            if (eventManager.addEvent(event)) {
                // Save to database immediately
                if (dataStore.saveEvent(event)) {
                    JOptionPane.showMessageDialog(this,
                        "Event created and saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Event created but failed to save to database",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE)
                }
                clearFields()
                refreshEventList()
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to create event",
                    "Error",
                    JOptionPane.ERROR_MESSAGE)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this,
                "Error: ${e.message}",
                "Input Error",
                JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshEventList() {
        eventListModel.clear()
        eventManager.getAllEvents().forEach { event ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            eventListModel.addElement(
                "${event.title} - ${event.dateTime.format(formatter)} @ ${event.venue.name}"
            )
        }
    }

    private fun refreshVenueCombo() {
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venue ->
            venueCombo.addItem(VenueItem(venue))
        }
    }

    private fun clearFields() {
        titleField.text = ""
        dateField.text = ""
        descriptionArea.text = ""
        maxParticipantsField.text = ""
    }

    private data class VenueItem(val venue: Venue) {
        override fun toString(): String =
            "${venue.name} (Capacity: ${venue.capacity})"
    }
/**
    /**
     * Part E - Slot Finder using Scala
     * Finds available venues using functional Scala algorithm
     */
    private fun findAvailableVenue() {
        try {
            // Get required capacity from max participants field
            val requiredCapacity = maxParticipantsField.text.toIntOrNull() ?: run {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter the required capacity in 'Max Participants' field first",
                    "Input Required",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return
            }

            // Get proposed date/time
            val dateTimeStr = dateField.text.trim()
            if (dateTimeStr.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a date/time first",
                    "Input Required",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return
            }

            val proposedDateTime = LocalDateTime.parse(
                dateTimeStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )

            // Call Scala SlotFinder algorithm (Part E)
            val venues = eventManager.getAllVenues()
            val events = eventManager.getAllEvents()

            val availableVenues = SlotFinder.findAllAvailableSlots(
                venues,
                events,
                requiredCapacity,
                proposedDateTime
            )

            // Display results
            if (availableVenues.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "No available venues found for:\n" +
                            "  Capacity: $requiredCapacity\n" +
                            "  Date/Time: ${proposedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n\n" +
                            "All venues are either too small or already booked.",
                    "No Venues Available",
                    JOptionPane.WARNING_MESSAGE
                )
            } else {
                // Create dialog to show results
                val message = buildString {
                    appendLine("Found ${availableVenues.size} available venue(s):\n")
                    availableVenues.forEachIndexed { index, venue ->
                        appendLine("${index + 1}. ${venue.name}")
                        appendLine("   Capacity: ${venue.capacity}")
                        appendLine("   Location: ${venue.location}\n")
                    }
                    appendLine("\nSelect a venue from the dropdown to use it.")
                }

                JOptionPane.showMessageDialog(
                    this,
                    message,
                    "Available Venues (Scala Algorithm)",
                    JOptionPane.INFORMATION_MESSAGE
                )

                // Auto-select the first available venue
                if (availableVenues.size > 0) {
                    val firstVenue = availableVenues[0]
                    for (i in 0 until venueCombo.itemCount) {
                        val item = venueCombo.getItemAt(i)
                        if (item.venue.id == firstVenue.id) {
                            venueCombo.selectedIndex = i
                            break
                        }
                    }
                }
            }

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Error finding available venues: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }

    }
*/
}