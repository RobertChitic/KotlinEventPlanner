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

class EventPanel(
    private val eventManager: EventManager
) : JPanel() {

    // --- TABLE COMPONENTS (Left Side) ---
    // Columns: Title, Date, Time, Venue, Capacity
    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false // Make table read-only
    }
    private val eventTable = JTable(tableModel)

    // --- FORM COMPONENTS (Right Side) ---
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val descriptionArea = JTextArea(5, 20)
    private val maxParticipantsField = JTextField(10)

    // Buttons
    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear Form")
    private val findVenueBtn = JButton("Find Slot (Scala)")

    init {
        layout = BorderLayout()

        // 1. Setup Table Styling
        // FIX: Used method call instead of property assignment for JTable
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        eventTable.rowHeight = 25
        eventTable.showHorizontalLines = true
        eventTable.gridColor = Color.LIGHT_GRAY
        eventTable.fillsViewportHeight = true

        // 2. Create Split Pane layout
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 600 // Give table plenty of space
        splitPane.resizeWeight = 0.7 // Table gets 70% of extra space on resize

        add(splitPane, BorderLayout.CENTER)

        // 3. Initialize Data & Listeners
        setupListeners()
        refreshEventTable() // Loads data
        refreshVenueCombo()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Event Schedule")
        panel.add(JScrollPane(eventTable), BorderLayout.CENTER)
        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        // --- Helper to add rows cleanly ---
        var gridY = 0
        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.0
            panel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            panel.add(component, gbc)
            gridY++
        }

        // Build Form
        addRow("Title:", titleField)

        val startEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm")
        startDateSpinner.editor = startEditor
        startDateSpinner.value = Date()
        addRow("Start Time:", startDateSpinner)

        // Duration Panel
        val durationPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        durationPanel.add(hoursSpinner); durationPanel.add(JLabel(" h "))
        durationPanel.add(minutesSpinner); durationPanel.add(JLabel(" m"))
        addRow("Duration:", durationPanel)

        addRow("Venue:", venueCombo)
        addRow("Max Capacity:", maxParticipantsField)

        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        val scrollDesc = JScrollPane(descriptionArea)

        gbc.gridx = 0; gbc.gridy = gridY; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        panel.add(scrollDesc, gbc)

        // Button Panel (Bottom)
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

        // Refresh venues when opening the dropdown to ensure we have latest list
        venueCombo.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) { refreshVenueCombo() }
            override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {}
        })
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0 // Clear existing rows
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        eventManager.getAllEvents()
            .sortedBy { it.dateTime } // Sort by date
            .forEach { event ->
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

    private fun refreshVenueCombo() {
        val selected = venueCombo.selectedItem
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venue -> venueCombo.addItem(VenueItem(venue)) }
        if (selected != null) {
            // Simple re-selection attempt:
            venueCombo.selectedItem = selected
        }
    }

    private fun createEvent() {
        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as? VenueItem
        val description = descriptionArea.text.trim()
        val maxParticipants = maxParticipantsField.text.toIntOrNull()

        // --- VALIDATION ---
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
        val endDateTime = startDateTime.plus(duration)

        // --- CONFIRMATION ---
        val confirmMsg = """
            Please confirm your Event details:
            
            Title:       $title
            Venue:       ${venueItem.venue.name}
            Start:       ${startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
            Ends:        ${endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}
            Capacity:    $maxParticipants
            
            Proceed?
        """.trimIndent()

        val choice = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm Event", JOptionPane.YES_NO_OPTION)
        if (choice != JOptionPane.YES_OPTION) return

        // --- CREATION LOGIC ---
        try {
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTime = startDateTime,
                venue = venueItem.venue,
                description = description,
                duration = duration,
                maxParticipants = maxParticipants
            )

            // Conflict Check (In Memory)
            val conflict = eventManager.getAllEvents().find { it.conflictsWith(event) }
            if (conflict != null) {
                JOptionPane.showMessageDialog(this,
                    "Conflict detected with event: '${conflict.title}'",
                    "Scheduling Conflict",
                    JOptionPane.WARNING_MESSAGE)
                return
            }

            // Async Save
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
                            JOptionPane.showMessageDialog(this@EventPanel, "Event Created!", "Success", JOptionPane.INFORMATION_MESSAGE)
                            clearFields()
                            refreshEventTable() // Update the table
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

    private fun findAvailableVenue() {
        try {
            // 1. Get Requirements
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

            // 2. Call Bridge
            val venues = eventManager.getAllVenues()
            val events = eventManager.getAllEvents()

            val availableVenues = ScalaBridge.findAvailableVenues(
                venues, events, requiredCapacity, proposedDateTime, duration
            )

            // 3. Handle Result
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
        startDateSpinner.value = Date()
        eventTable.clearSelection()
    }

    private data class VenueItem(val venue: Venue) {
        override fun toString(): String = venue.name
    }
}