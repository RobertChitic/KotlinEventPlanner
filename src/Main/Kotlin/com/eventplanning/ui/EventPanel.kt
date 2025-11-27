package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
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

    private var displayedEvents: List<Event> = emptyList()

    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val eventTable = JTable(tableModel)
    private val tableSorter = TableRowSorter(tableModel)

    // Form Components
    private val searchField = JTextField(15)
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val venueInfoLabel = JLabel("Select a venue...")
    private val descriptionArea = JTextArea(4, 20)
    private val maxParticipantsField = JTextField(10)

    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear")
    private val findVenueBtn = JButton("Find Slot")
    private val deleteButton = JButton("❌ Delete Event")

    init {
        layout = BorderLayout()

        // Table Setup
        eventTable.rowSorter = tableSorter
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        eventTable.rowHeight = 28
        eventTable.showHorizontalLines = true
        eventTable.gridColor = Color.LIGHT_GRAY
        eventTable.fillsViewportHeight = true
        eventTable.columnModel.getColumn(4).cellRenderer = CapacityCellRenderer()

        venueInfoLabel.foreground = Color.GRAY

        deleteButton.foreground = Color.WHITE
        deleteButton.background = Color(220, 53, 69)
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        // Create Split Pane
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 650
        splitPane.resizeWeight = 0.7

        add(splitPane, BorderLayout.CENTER)

        setupListeners()
        refreshEventTable()
        refreshVenueCombo()
    }

    private class CapacityCellRenderer : DefaultTableCellRenderer() {
        private val progressBar = JProgressBar(0, 100)
        init {
            progressBar.isStringPainted = true
            progressBar.border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createLineBorder(Color.LIGHT_GRAY)
            )
            progressBar.isOpaque = true
        }
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            if (isSelected) progressBar.background = table?.selectionBackground else progressBar.background = Color.WHITE
            val strValue = value as? String ?: "0/0"
            try {
                val parts = strValue.split("/")
                if (parts.size == 2) {
                    val current = parts[0].toInt(); val max = parts[1].toInt()
                    progressBar.maximum = max; progressBar.value = current; progressBar.string = "$current / $max"
                    val percent = if (max > 0) (current.toDouble() / max * 100).toInt() else 0
                    progressBar.foreground = when {
                        percent >= 100 -> Color(220, 53, 69)
                        percent >= 75 -> Color(255, 193, 7)
                        else -> Color(40, 167, 69)
                    }
                }
            } catch (e: Exception) { progressBar.string = strValue }
            return progressBar
        }
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Event Schedule")

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.add(JLabel("Filter:"))
        topPanel.add(searchField)
        panel.add(topPanel, BorderLayout.NORTH)

        panel.add(JScrollPane(eventTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(deleteButton)
        panel.add(btnPanel, BorderLayout.SOUTH)
        return panel
    }

    // FIXED: Simplified Layout (Standard GridBag is safer here)
    private fun createFormPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = BorderFactory.createTitledBorder("Event Details")

        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        // Helper to reset GBC
        fun resetGBC(x: Int, y: Int) {
            gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL
        }

        // Row 0: Title
        resetGBC(0, 0); formPanel.add(JLabel("Title:"), gbc)
        resetGBC(1, 0); gbc.weightx = 1.0; formPanel.add(titleField, gbc)

        // Row 1: Start Time
        resetGBC(0, 1); formPanel.add(JLabel("Start:"), gbc)
        val startEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm")
        startDateSpinner.editor = startEditor
        startDateSpinner.value = Date()
        resetGBC(1, 1); gbc.weightx = 1.0; formPanel.add(startDateSpinner, gbc)

        // Row 2: Duration
        resetGBC(0, 2); formPanel.add(JLabel("Duration:"), gbc)
        val durPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        durPanel.add(hoursSpinner); durPanel.add(JLabel(" hrs  ")); durPanel.add(minutesSpinner); durPanel.add(JLabel(" mins"))
        resetGBC(1, 2); gbc.weightx = 1.0; formPanel.add(durPanel, gbc)

        // Row 3: Venue
        resetGBC(0, 3); formPanel.add(JLabel("Venue:"), gbc)
        resetGBC(1, 3); gbc.weightx = 1.0; formPanel.add(venueCombo, gbc)

        // Row 4: Venue Info Label (small text under combo)
        resetGBC(1, 4); formPanel.add(venueInfoLabel, gbc)

        // Row 5: Capacity
        resetGBC(0, 5); formPanel.add(JLabel("Capacity:"), gbc)
        resetGBC(1, 5); gbc.weightx = 1.0; formPanel.add(maxParticipantsField, gbc)

        // Row 6: Description (Scrollpane needs specific constraints)
        resetGBC(0, 6); gbc.anchor = GridBagConstraints.NORTHWEST; formPanel.add(JLabel("Desc:"), gbc)
        resetGBC(1, 6); gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        formPanel.add(JScrollPane(descriptionArea), gbc)

        mainPanel.add(formPanel, BorderLayout.CENTER)

        // Buttons at bottom
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(findVenueBtn)
        buttonPanel.add(createButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun setupListeners() {
        createButton.addActionListener { createEvent() }
        findVenueBtn.addActionListener { findAvailableVenue() }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener { deleteSelectedEvent() }

        venueCombo.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) { refreshVenueCombo() }
            override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {}
        })
        venueCombo.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val item = e.item as? VenueItem
                if (item != null) venueInfoLabel.text = "✓ ${item.venue.name} (Cap: ${item.venue.capacity})"
            }
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { filter() }
            override fun removeUpdate(e: DocumentEvent?) { filter() }
            override fun changedUpdate(e: DocumentEvent?) { filter() }
            fun filter() {
                val text = searchField.text
                if (text.trim().isEmpty()) tableSorter.setRowFilter(null)
                else tableSorter.setRowFilter(RowFilter.regexFilter("(?i)$text"))
            }
        })
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        displayedEvents = eventManager.getAllEvents().sortedBy { it.dateTime }
        displayedEvents.forEach { event ->
            val endTime = event.getEndTime()
            tableModel.addRow(arrayOf(
                event.title, event.dateTime.format(dateFormatter),
                "${event.dateTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                event.venue.name, "${event.getCurrentCapacity()}/${event.maxParticipants}"
            ))
        }
    }

    private fun deleteSelectedEvent() {
        val viewRow = eventTable.selectedRow
        if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Select an event.", "Error", JOptionPane.WARNING_MESSAGE); return }
        val modelRow = eventTable.convertRowIndexToModel(viewRow)
        val eventToDelete = displayedEvents.getOrNull(modelRow) ?: return
        if (JOptionPane.showConfirmDialog(this, "Delete '${eventToDelete.title}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.deleteEvent(eventToDelete)
                override fun done() { if (get()) { refreshEventTable(); JOptionPane.showMessageDialog(this@EventPanel, "Deleted.", "Success", JOptionPane.INFORMATION_MESSAGE) } }
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
        val title = titleField.text.trim(); val venueItem = venueCombo.selectedItem as? VenueItem; val description = descriptionArea.text.trim(); val maxParticipants = maxParticipantsField.text.toIntOrNull()
        if (title.isBlank() || venueItem == null || maxParticipants == null || maxParticipants <= 0 || maxParticipants > venueItem.venue.capacity) { JOptionPane.showMessageDialog(this, "Invalid Input/Capacity", "Error", JOptionPane.ERROR_MESSAGE); return }
        val duration = Duration.ofHours(hoursSpinner.value as Long).plusMinutes(minutesSpinner.value as Long)
        val startDateTime = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())
        val event = Event(UUID.randomUUID().toString(), title, startDateTime, venueItem.venue, description, duration, maxParticipants)
        if (eventManager.getAllEvents().any { it.conflictsWith(event) }) { JOptionPane.showMessageDialog(this, "Conflict!", "Error", JOptionPane.ERROR_MESSAGE); return }

        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground() = eventManager.addEvent(event)
            override fun done() { if (get()) { refreshEventTable(); clearFields(); JOptionPane.showMessageDialog(this@EventPanel, "Saved!", "Success", JOptionPane.INFORMATION_MESSAGE) } else JOptionPane.showMessageDialog(this@EventPanel, "DB Error", "Error", JOptionPane.ERROR_MESSAGE) }
        }
        worker.execute()
    }

    private fun findAvailableVenue() {
        try {
            val cap = maxParticipantsField.text.toIntOrNull() ?: return
            val dur = Duration.ofHours(hoursSpinner.value as Long).plusMinutes(minutesSpinner.value as Long)
            val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())
            val res = ScalaBridge.findAvailableVenues(eventManager.getAllVenues(), eventManager.getAllEvents(), cap, dt, dur)
            if(res.isEmpty()) JOptionPane.showMessageDialog(this, "No venues.", "Result", JOptionPane.INFORMATION_MESSAGE)
            else { val sb = StringBuilder("Found:\n"); res.forEach { sb.append("- ${it.name}\n") }; JOptionPane.showMessageDialog(this, sb.toString(), "Result", JOptionPane.INFORMATION_MESSAGE) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun clearFields() { titleField.text=""; descriptionArea.text=""; maxParticipantsField.text=""; venueInfoLabel.text="Select a venue..."; eventTable.clearSelection() }
    private data class VenueItem(val venue: Venue) { override fun toString() = "${venue.name} (Cap: ${venue.capacity})" }
}