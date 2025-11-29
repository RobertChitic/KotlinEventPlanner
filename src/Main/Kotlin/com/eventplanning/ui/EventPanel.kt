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
import javax.swing.table.TableCellRenderer
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

    // STATE: Track if we are editing
    private var editingEventId: String? = null

    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    // Table with Conditional Formatting (Red if full)
    private val eventTable = object : JTable(tableModel) {
        override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
            val component = super.prepareRenderer(renderer, row, column)
            if (!isRowSelected(row)) {
                val modelRow = convertRowIndexToModel(row)
                val event = displayedEvents.getOrNull(modelRow)
                if (event != null && event.isFull()) {
                    component.background = Color(255, 235, 235) // Light Red
                } else {
                    component.background = Color.WHITE
                }
            }
            return component
        }
    }
    private val tableSorter = TableRowSorter(tableModel)

    // Form
    private val searchField = JTextField(15)
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val venueInfoLabel = JLabel("Select a venue...")
    private val descriptionArea = JTextArea(5, 20)
    private val maxParticipantsField = JTextField(10)

    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear")
    private val editButton = JButton("✎ Edit Event") // NEW BUTTON
    private val findVenueBtn = JButton("Find Slot")
    private val deleteButton = JButton("❌ Delete Event")

    init {
        layout = BorderLayout()

        eventTable.rowSorter = tableSorter
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        eventTable.rowHeight = 30
        eventTable.showHorizontalLines = true
        eventTable.gridColor = Color.LIGHT_GRAY
        eventTable.fillsViewportHeight = true
        eventTable.columnModel.getColumn(4).cellRenderer = CapacityCellRenderer()

        searchField.putClientProperty("JTextField.placeholderText", "🔍 Filter...")
        venueInfoLabel.foreground = Color.GRAY

        deleteButton.foreground = Color.WHITE
        deleteButton.background = Color(220, 53, 69)
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        // Style Edit Button
        editButton.foreground = Color.WHITE
        editButton.background = Color(255, 193, 7) // Orange/Yellow
        editButton.isContentAreaFilled = false
        editButton.isOpaque = true

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

    private class CapacityCellRenderer : DefaultTableCellRenderer() {
        private val progressBar = JProgressBar(0, 100)
        init {
            progressBar.isStringPainted = true
            progressBar.border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
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
        topPanel.add(JLabel("Filter:")); topPanel.add(searchField)
        panel.add(topPanel, BorderLayout.NORTH)

        panel.add(JScrollPane(eventTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(editButton) // Add Edit button here
        btnPanel.add(Box.createHorizontalStrut(10))
        btnPanel.add(deleteButton)
        panel.add(btnPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL

        var gridY = 0
        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.0; panel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0; panel.add(component, gbc); gridY++
        }
        addRow("Title:", titleField)
        val startEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm"); startDateSpinner.editor = startEditor; startDateSpinner.value = Date()
        addRow("Start Time:", startDateSpinner)
        val durPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)); durPanel.add(hoursSpinner); durPanel.add(JLabel(" h ")); durPanel.add(minutesSpinner); durPanel.add(JLabel(" m"))
        addRow("Duration:", durPanel)
        addRow("Venue:", venueCombo); gbc.gridx = 1; gbc.gridy = gridY; panel.add(venueInfoLabel, gbc); gridY++
        addRow("Capacity:", maxParticipantsField)

        descriptionArea.lineWrap = true; descriptionArea.wrapStyleWord = true; val scrollDesc = JScrollPane(descriptionArea)
        gbc.gridx = 0; gbc.gridy = gridY; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH; panel.add(scrollDesc, gbc)

        gridY++; val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); buttonPanel.add(clearButton); buttonPanel.add(findVenueBtn); buttonPanel.add(createButton)
        gbc.gridy = gridY; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        createButton.addActionListener { if (editingEventId == null) createEvent() else updateEvent() }
        findVenueBtn.addActionListener { findAvailableVenue() }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener { deleteSelectedEvent() }
        editButton.addActionListener { loadEventForEditing() } // Listener for new button

        venueCombo.addItemListener { e -> if (e.stateChange == ItemEvent.SELECTED) { val v = (e.item as? VenueItem)?.venue; if (v!=null) venueInfoLabel.text = "✓ ${v.name} (Cap: ${v.capacity})" } }
        venueCombo.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) { refreshVenueCombo() }
            override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {}
        })

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { filter() }
            override fun removeUpdate(e: DocumentEvent?) { filter() }
            override fun changedUpdate(e: DocumentEvent?) { filter() }
            fun filter() {
                val text = searchField.text
                if (text.trim().isEmpty()) tableSorter.setRowFilter(null) else tableSorter.setRowFilter(RowFilter.regexFilter("(?i)$text"))
            }
        })
    }

    // --- EDIT LOGIC ---
    private fun loadEventForEditing() {
        val viewRow = eventTable.selectedRow
        if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Select an event to edit.", "Error", JOptionPane.WARNING_MESSAGE); return }
        val modelRow = eventTable.convertRowIndexToModel(viewRow)
        val event = displayedEvents.getOrNull(modelRow) ?: return

        // Populate Fields
        titleField.text = event.title
        val date = Date.from(event.dateTime.atZone(ZoneId.systemDefault()).toInstant())
        startDateSpinner.value = date
        hoursSpinner.value = event.duration.toHours().toInt()
        minutesSpinner.value = event.duration.toMinutesPart()
        maxParticipantsField.text = event.maxParticipants.toString()
        descriptionArea.text = event.description

        // Select Venue
        for (i in 0 until venueCombo.itemCount) {
            if (venueCombo.getItemAt(i).venue.id == event.venue.id) {
                venueCombo.selectedIndex = i
                break
            }
        }

        // Set State
        editingEventId = event.id
        createButton.text = "Save Changes"
        createButton.background = Color(255, 193, 7) // Orange
        createButton.foreground = Color.BLACK
        clearButton.text = "Cancel Edit"

        // Disable table during edit to prevent confusion
        eventTable.isEnabled = false
        editButton.isEnabled = false
        deleteButton.isEnabled = false
    }

    private fun updateEvent() {
        val id = editingEventId ?: return
        val oldEvent = eventManager.getEventById(id) ?: return

        val title = titleField.text.trim(); val venueItem = venueCombo.selectedItem as? VenueItem; val desc = descriptionArea.text.trim(); val cap = maxParticipantsField.text.toIntOrNull()
        if (title.isBlank() || venueItem == null || cap == null || cap <= 0 || cap > venueItem.venue.capacity) { JOptionPane.showMessageDialog(this, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE); return }
        val dur = Duration.ofHours(hoursSpinner.value as Long).plusMinutes(minutesSpinner.value as Long)
        val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())

        // Create Updated Object using copy to keep participants!
        val updatedEvent = oldEvent.copy(
            title = title,
            dateTime = dt,
            venue = venueItem.venue,
            description = desc,
            duration = dur,
            maxParticipants = cap
        )

        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground() = eventManager.modifyEvent(updatedEvent)
            override fun done() {
                if (get()) {
                    refreshEventTable()
                    clearFields()
                    JOptionPane.showMessageDialog(this@EventPanel, "Updated!", "Success", JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this@EventPanel, "Failed (Conflict or Capacity Issue)", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        worker.execute()
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0
        val dF = DateTimeFormatter.ofPattern("yyyy-MM-dd"); val tF = DateTimeFormatter.ofPattern("HH:mm")
        displayedEvents = eventManager.getAllEvents().sortedBy { it.dateTime }
        displayedEvents.forEach { e ->
            val end = e.getEndTime()
            tableModel.addRow(arrayOf(e.title, e.dateTime.format(dF), "${e.dateTime.format(tF)} - ${end.format(tF)}", e.venue.name, "${e.getCurrentCapacity()}/${e.maxParticipants}"))
        }
    }

    private fun createEvent() {
        val title = titleField.text.trim(); val venueItem = venueCombo.selectedItem as? VenueItem; val desc = descriptionArea.text.trim(); val cap = maxParticipantsField.text.toIntOrNull()
        if (title.isBlank() || venueItem == null || cap == null || cap <= 0 || cap > venueItem.venue.capacity) { JOptionPane.showMessageDialog(this, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE); return }
        val dur = Duration.ofHours(hoursSpinner.value as Long).plusMinutes(minutesSpinner.value as Long)
        val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())
        val event = Event(UUID.randomUUID().toString(), title, dt, venueItem.venue, desc, dur, cap)
        if (eventManager.getAllEvents().any { it.conflictsWith(event) }) { JOptionPane.showMessageDialog(this, "Conflict!", "Error", JOptionPane.ERROR_MESSAGE); return }
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground() = eventManager.addEvent(event)
            override fun done() { if (get()) { refreshEventTable(); clearFields(); JOptionPane.showMessageDialog(this@EventPanel, "Saved!", "Success", JOptionPane.INFORMATION_MESSAGE) } }
        }
        worker.execute()
    }

    private fun deleteSelectedEvent() {
        val viewRow = eventTable.selectedRow
        if (viewRow == -1) return
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
        eventManager.getAllVenues().forEach { venueCombo.addItem(VenueItem(it)) }
        if (selected != null) venueCombo.selectedItem = selected
    }

    private fun findAvailableVenue() {
        try {
            val cap = maxParticipantsField.text.toIntOrNull() ?: return
            val dur = Duration.ofHours(hoursSpinner.value as Long).plusMinutes(minutesSpinner.value as Long)
            val dt = LocalDateTime.ofInstant((startDateSpinner.value as Date).toInstant(), ZoneId.systemDefault())

            // result is type SlotFinderResult (Success or Error)
            val result = ScalaBridge.findAvailableVenues(eventManager.getAllVenues(), eventManager.getAllEvents(), cap, dt, dur)

            when (result) {
                is ScalaBridge.SlotFinderResult.Success -> {
                    val venues = result.venues // Access the actual list here
                    if (venues.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No venues.", "Info", JOptionPane.INFORMATION_MESSAGE)
                    } else {
                        val sb = StringBuilder("Found:\n")
                        venues.forEach { sb.append("- ${it.name}\n") }
                        JOptionPane.showMessageDialog(this, sb.toString(), "Info", JOptionPane.INFORMATION_MESSAGE)
                    }
                }
                is ScalaBridge.SlotFinderResult.Error -> {
                    JOptionPane.showMessageDialog(this, result.message, "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun clearFields() {
        titleField.text=""; descriptionArea.text=""; maxParticipantsField.text=""; venueInfoLabel.text="Select a venue..."; eventTable.clearSelection()
        // Reset Edit State
        editingEventId = null
        createButton.text = "Create Event"
        createButton.background = null // Reset color
        createButton.foreground = null
        clearButton.text = "Clear"
        eventTable.isEnabled = true
        editButton.isEnabled = true
        deleteButton.isEnabled = true
    }

    private data class VenueItem(val venue: Venue) { override fun toString() = "${venue.name} (Cap: ${venue.capacity})" }
}