package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import java.awt.event.ItemEvent

class EventPanel(
    private val eventManager: EventManager
) : JPanel() {

    // Store the currently displayed events to match table rows to objects
    private var displayedEvents: List<Event> = emptyList()

    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val eventTable = JTable(tableModel)

    // --- FORM COMPONENTS ---
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val venueInfoLabel = JLabel("Select a venue to see details")
    private val descriptionArea = JTextArea(5, 20)
    private val maxParticipantsField = JTextField(10)

    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear Form")
    private val findVenueBtn = JButton("Find Slot (Scala)")

    // NEW: Delete Button
    private val deleteButton = JButton("❌ Delete Event")

    init {
        layout = BorderLayout()

        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        eventTable.rowHeight = 25
        eventTable.showHorizontalLines = true
        eventTable.gridColor = Color.LIGHT_GRAY
        eventTable.fillsViewportHeight = true

        venueInfoLabel.foreground = Color.GRAY
        venueInfoLabel.font = venueInfoLabel.font.deriveFont(Font.ITALIC)

        // Style the Delete Button
        deleteButton.foreground = Color.BLACK
        deleteButton.background = Color(220, 53, 69) // Red
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 600
        splitPane.resizeWeight = 0.7

        add(splitPane, BorderLayout.CENTER)

        setupListeners()
        refreshEventTable()
        refreshVenueCombo()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Event Schedule")
        panel.add(JScrollPane(eventTable), BorderLayout.CENTER)

        // Add Delete Button to bottom of list
        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(deleteButton)
        panel.add(btnPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        var gridY = 0
        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.0
            panel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            panel.add(component, gbc)
            gridY++
        }

        addRow("Title:", titleField)

        val startEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm")
        startDateSpinner.editor = startEditor
        startDateSpinner.value = Date()
        addRow("Start Time:", startDateSpinner)

        val durationPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        durationPanel.add(hoursSpinner); durationPanel.add(JLabel(" h "))
        durationPanel.add(minutesSpinner); durationPanel.add(JLabel(" m"))
        addRow("Duration:", durationPanel)

        addRow("Venue:", venueCombo)
        gbc.gridx = 1; gbc.gridy = gridY
        panel.add(venueInfoLabel, gbc)
        gridY++

        addRow("Max Capacity:", maxParticipantsField)

        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        val scrollDesc = JScrollPane(descriptionArea)
        gbc.gridx = 0; gbc.gridy = gridY; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(scrollDesc, gbc)

        gridY++
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(findVenueBtn)
        buttonPanel.add(createButton)

        gbc.gridy = gridY; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        createButton.addActionListener { createEvent() }
        findVenueBtn.addActionListener { findAvailableVenue() }
        clearButton.addActionListener { clearFields() }

        // NEW: Delete Logic
        deleteButton.addActionListener { deleteSelectedEvent() }

        venueCombo.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) { refreshVenueCombo() }
            override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {}
        })

        venueCombo.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val item = e.item as? VenueItem
                if (item != null) {
                    venueInfoLabel.text = "✓ ${item.venue.name} (Capacity: ${item.venue.capacity})"
                }
            }
        }
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // Keep track of displayed events to map selection later
        displayedEvents = eventManager.getAllEvents().sortedBy { it.dateTime }

        displayedEvents.forEach { event ->
            val endTime = event.getEndTime()
            tableModel.addRow(arrayOf(
                event.title,
                event.dateTime.format(dateFormatter),
                "${event.dateTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                event.venue.name,
                "${event.getCurrentCapacity()}/${event.maxParticipants}"
            ))
        }
    }

    private fun deleteSelectedEvent() {
        val selectedRow = eventTable.selectedRow
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an event to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        // Safely get the event object using the parallel list
        val eventToDelete = displayedEvents.getOrNull(selectedRow) ?: return

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete '${eventToDelete.title}'?\nThis cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean {
                    return eventManager.deleteEvent(eventToDelete)
                }
                override fun done() {
                    if (get()) {
                        JOptionPane.showMessageDialog(this@EventPanel, "Event deleted.", "Success", JOptionPane.INFORMATION_MESSAGE)
                        refreshEventTable()
                    } else {
                        JOptionPane.showMessageDialog(this@EventPanel, "Failed to delete event.", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }
            worker.execute()
        }
    }

    private fun refreshVenueCombo() {
        val selected = venueCombo.selectedItem
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venue -> venueCombo.addItem(VenueItem(venue)) }
        if (selected != null) venueCombo.selectedItem = selected
    }

    private fun createEvent() {
        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as? VenueItem
        val description = descriptionArea.text.trim()
        val maxParticipants = maxParticipantsField.text.toIntOrNull()

        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter title", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (venueItem == null) {
            JOptionPane.showMessageDialog(this, "Please select a venue", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (maxParticipants == null || maxParticipants <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Max Participants.", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (maxParticipants > venueItem.venue.capacity) {
            JOptionPane.showMessageDialog(this, "Max Participants cannot exceed venue capacity (${venueItem.venue.capacity}).", "Capacity Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        val hours = hoursSpinner.value as Int
        val minutes = minutesSpinner.value as Int
        if (hours == 0 && minutes == 0) {
            JOptionPane.showMessageDialog(this, "Duration must be at least 1 minute.", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        val duration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())
        val startObj = startDateSpinner.value as Date
        val startDateTime = LocalDateTime.ofInstant(startObj.toInstant(), ZoneId.systemDefault())

        val event = Event(
            id = UUID.randomUUID().toString(),
            title = title,
            dateTime = startDateTime,
            venue = venueItem.venue,
            description = description,
            duration = duration,
            maxParticipants = maxParticipants
        )

        if (eventManager.getAllEvents().any { it.conflictsWith(event) }) {
            JOptionPane.showMessageDialog(this, "Conflict Detected!", "Error", JOptionPane.ERROR_MESSAGE)
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
                if (get()) {
                    JOptionPane.showMessageDialog(this@EventPanel, "Event Created!", "Success", JOptionPane.INFORMATION_MESSAGE)
                    refreshEventTable()
                    clearFields()
                } else {
                    JOptionPane.showMessageDialog(this@EventPanel, "Failed to save event.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        worker.execute()
    }

    private fun findAvailableVenue() {
        try {
            val requiredCapacity = maxParticipantsField.text.toIntOrNull()
            if (requiredCapacity == null) {
                JOptionPane.showMessageDialog(this, "Enter required capacity first", "Info", JOptionPane.INFORMATION_MESSAGE)
                return
            }

            val hours = hoursSpinner.value as Int
            val minutes = minutesSpinner.value as Int
            if (hours == 0 && minutes == 0) {
                JOptionPane.showMessageDialog(this, "Set a duration first", "Info", JOptionPane.INFORMATION_MESSAGE)
                return
            }
            val duration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())

            val selectedDate = startDateSpinner.value as Date
            val proposedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault())

            val venues = eventManager.getAllVenues()
            val events = eventManager.getAllEvents()

            val availableVenues = ScalaBridge.findAvailableVenues(
                venues, events, requiredCapacity, proposedDateTime, duration
            )

            if (availableVenues.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No venues available for these criteria.", "Result", JOptionPane.INFORMATION_MESSAGE)
            } else {
                val sb = StringBuilder("Found ${availableVenues.size} venues:\n")
                availableVenues.forEach { sb.append("- ${it.name} (Cap: ${it.capacity})\n") }
                JOptionPane.showMessageDialog(this, sb.toString(), "Result", JOptionPane.INFORMATION_MESSAGE)

                // Auto-select first match
                val firstId = availableVenues[0].id
                for (i in 0 until venueCombo.itemCount) {
                    if (venueCombo.getItemAt(i).venue.id == firstId) {
                        venueCombo.selectedIndex = i
                        break
                    }
                }
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Bridge Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun clearFields() {
        titleField.text = ""
        descriptionArea.text = ""
        maxParticipantsField.text = ""
        hoursSpinner.value = 2
        minutesSpinner.value = 0
        venueInfoLabel.text = "Select a venue to see details"
        startDateSpinner.value = Date()
        eventTable.clearSelection()
    }

    private data class VenueItem(val venue: Venue) {
        override fun toString(): String = "${venue.name} (Cap: ${venue.capacity})"
    }
}