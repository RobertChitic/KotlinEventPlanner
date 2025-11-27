package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge // NEW IMPORT
import javax.swing.*
import java.awt.*
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
    private val dateSpinner = JSpinner(SpinnerDateModel())
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

        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Event Title:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(titleField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Date/Time:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        val spinnerEditor = JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd HH:mm")
        dateSpinner.editor = spinnerEditor
        dateSpinner.value = Date()
        panel.add(dateSpinner, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Venue:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(venueCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Max Participants:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(maxParticipantsField, gbc)

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Description:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        panel.add(JScrollPane(descriptionArea), gbc)

        gbc.gridx = 1; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE
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

        val selectedDate = dateSpinner.value as Date
        val dateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault())

        try {
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTime = dateTime,
                venue = venueItem.venue,
                description = description,
                maxParticipants = maxParticipants ?: venueItem.venue.capacity
            )

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
                            JOptionPane.showMessageDialog(this@EventPanel, "Failed to save event", "Error", JOptionPane.ERROR_MESSAGE)
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
        eventManager.getAllEvents().forEach { event ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            eventListModel.addElement("${event.title} - ${event.dateTime.format(formatter)} @ ${event.venue.name}")
        }
    }

    private fun refreshVenueCombo() {
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venue -> venueCombo.addItem(VenueItem(venue)) }
    }

    private fun clearFields() {
        titleField.text = ""
        dateSpinner.value = Date()
        descriptionArea.text = ""
        maxParticipantsField.text = ""
    }

    private data class VenueItem(val venue: Venue) {
        override fun toString(): String = "${venue.name} (Capacity: ${venue.capacity})"
    }

    // === UPDATED: Using ScalaBridge ===
    private fun findAvailableVenue() {
        try {
            val requiredCapacity = maxParticipantsField.text.toIntOrNull() ?: run {
                JOptionPane.showMessageDialog(this, "Enter required capacity first", "Info", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            val selectedDate = dateSpinner.value as Date
            val proposedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault())

            // Use local variables for thread safety inside SwingWorker (though not strictly required for read)
            val venues = eventManager.getAllVenues()
            val events = eventManager.getAllEvents()

            // Note: You could wrap this in a SwingWorker too if it's slow,
            // but here we are just bridging the call.

            val availableVenues = ScalaBridge.findAvailableVenues(
                venues,
                events,
                requiredCapacity,
                proposedDateTime
            )

            if (availableVenues.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No venues found for this date/capacity.", "Info", JOptionPane.INFORMATION_MESSAGE)
            } else {
                val message = buildString {
                    appendLine("Found ${availableVenues.size} available venue(s):")
                    availableVenues.forEach { appendLine("- ${it.name} (Cap: ${it.capacity})") }
                }
                JOptionPane.showMessageDialog(this, message, "Results", JOptionPane.INFORMATION_MESSAGE)

                // Auto-select first match
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