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
import java.util.Calendar
import java.util.UUID

class EventPanel(private val eventManager: EventManager) : JPanel() {

    // State
    private var displayedEvents: List<Event> = emptyList()
    private var editingEventId: String? = null

    // Table
    private val tableModel = object : DefaultTableModel(arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val eventTable = JTable(tableModel)
    private val tableSorter = TableRowSorter(tableModel)

    // Inputs
    private val searchField = UIStyles.createTextField(15).apply {
        putClientProperty("JTextField.placeholderText", "Search...")
    }
    private val titleField = UIStyles.createTextField()

    // Styled Components
    private val venueCombo = UIStyles.createComboBox()

    // --- DARK THEMED SPINNERS ---

    // Date Spinner
    private val dateSpinner = JSpinner(SpinnerDateModel().apply {
        calendarField = Calendar.DAY_OF_MONTH
    }).apply {
        editor = JSpinner.DateEditor(this, "dd/MM/yyyy")
        applyDarkSpinnerStyle(this)
    }

    // Time Spinner
    private val timeSpinner = JSpinner(SpinnerDateModel().apply {
        calendarField = Calendar.MINUTE
    }).apply {
        editor = JSpinner.DateEditor(this, "HH:mm")
        applyDarkSpinnerStyle(this)
    }

    // Duration Spinners
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1)).apply { applyDarkSpinnerStyle(this) }
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15)).apply { applyDarkSpinnerStyle(this) }

    private val descriptionArea = UIStyles.createTextArea(3, 20)
    private val maxParticipantsField = UIStyles.createTextField(10)

    // Buttons
    private val saveButton = UIStyles.createPrimaryButton("Save Event")
    private val clearButton = UIStyles.createSecondaryButton("Cancel")
    private val editButton = UIStyles.createAccentButton("Edit Selected")
    private val deleteButton = UIStyles.createDangerButton("Delete")
    private val findVenueBtn = UIStyles.createSecondaryButton("Find Slot")

    init {
        layout = BorderLayout(0, 20)
        background = UIStyles.background
        isOpaque = false

        // Header
        add(UIStyles.createHeaderLabel("Events Management"), BorderLayout.NORTH)

        // Main Layout
        val content = JPanel(GridBagLayout())
        content.isOpaque = false
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            insets = Insets(0, 0, 0, 20)
        }

        // Left: Table
        gbc.gridx = 0; gbc.weightx = 0.65; gbc.weighty = 1.0
        content.add(createTableCard(), gbc)

        // Right: Form
        gbc.gridx = 1; gbc.weightx = 0.35; gbc.insets = Insets(0, 0, 0, 0)
        content.add(createFormCard(), gbc)

        add(content, BorderLayout.CENTER)

        refreshEventTable()
        refreshVenueCombo()
        setupListeners()
    }

    // --- STYLE HELPER FOR DARK SPINNERS ---
    private fun applyDarkSpinnerStyle(spinner: JSpinner) {
        val editor = spinner.editor

        // Helper to style the inner text field
        fun styleField(tf: JFormattedTextField) {
            tf.background = UIStyles.inputBackground
            tf.foreground = UIStyles.textPrimary
            tf.caretColor = UIStyles.accentGreen
            tf.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
        }

        if (editor is JSpinner.DefaultEditor) {
            styleField(editor.textField)
        } else if (editor is JSpinner.DateEditor) {
            styleField(editor.textField)
        }

        // Style the outer border
        spinner.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyles.tableBorder, 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        )
        spinner.background = UIStyles.inputBackground
    }

    private fun createTableCard(): JPanel {
        val card = UIStyles.createCardPanel()

        val header = JPanel(BorderLayout())
        header.isOpaque = false
        header.add(UIStyles.createSectionLabel("SCHEDULE"), BorderLayout.WEST)

        val tools = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        tools.isOpaque = false
        tools.add(searchField)
        tools.add(editButton)
        tools.add(deleteButton)
        header.add(tools, BorderLayout.EAST)

        card.add(header, BorderLayout.NORTH)

        UIStyles.styleTable(eventTable)
        eventTable.rowSorter = tableSorter
        eventTable.columnModel.getColumn(4).cellRenderer = CapacityCellRenderer()

        card.add(UIStyles.createScrollPane(eventTable), BorderLayout.CENTER)
        return card
    }

    private fun createFormCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(UIStyles.createSectionLabel("EVENT DETAILS"), BorderLayout.NORTH)

        val form = JPanel(GridBagLayout())
        form.isOpaque = false
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 12, 0)
            weightx = 1.0
            gridx = 0
        }
        var y = 0

        fun addIn(label: String, comp: JComponent) {
            gbc.gridy = y++
            form.add(UIStyles.createLabel(label), gbc)
            gbc.gridy = y++
            form.add(comp, gbc)
        }

        addIn("Event Title", titleField)

        // Split Date and Time Row
        val dateTimePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        dateTimePanel.isOpaque = false

        // Date
        dateSpinner.preferredSize = Dimension(130, 36)
        dateTimePanel.add(dateSpinner)
        dateTimePanel.add(Box.createHorizontalStrut(10))

        // Time
        timeSpinner.preferredSize = Dimension(90, 36)
        dateTimePanel.add(timeSpinner)

        addIn("Start Date & Time", dateTimePanel)

        // Duration Row
        val durPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        durPanel.isOpaque = false

        hoursSpinner.preferredSize = Dimension(70, 36)
        minutesSpinner.preferredSize = Dimension(70, 36)

        durPanel.add(hoursSpinner)
        durPanel.add(UIStyles.createLabel(" h   "))
        durPanel.add(minutesSpinner)
        durPanel.add(UIStyles.createLabel(" m"))
        addIn("Duration", durPanel)

        // Venue Row
        val venPanel = JPanel(BorderLayout(5, 0))
        venPanel.isOpaque = false
        venPanel.add(venueCombo, BorderLayout.CENTER)
        venPanel.add(findVenueBtn, BorderLayout.EAST)
        addIn("Venue", venPanel)

        addIn("Max Capacity", maxParticipantsField)
        addIn("Description", UIStyles.createScrollPane(descriptionArea))

        gbc.gridy = y++
        gbc.weighty = 1.0
        form.add(JPanel().apply { isOpaque = false }, gbc)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        btnPanel.isOpaque = false
        btnPanel.add(clearButton)
        btnPanel.add(saveButton)

        gbc.gridy = y++
        gbc.weighty = 0.0
        form.add(btnPanel, gbc)

        card.add(form, BorderLayout.CENTER)
        return card
    }

    // --- LOGIC & LISTENERS ---

    private fun setupListeners() {
        saveButton.addActionListener {
            if (editingEventId == null) createEvent() else updateEvent()
        }
        clearButton.addActionListener { clearFields() }
        editButton.addActionListener { loadEventForEditing() }
        deleteButton.addActionListener { deleteSelectedEvent() }
        findVenueBtn.addActionListener { findAvailableVenue() }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filter()
            override fun removeUpdate(e: DocumentEvent?) = filter()
            override fun changedUpdate(e: DocumentEvent?) = filter()
            fun filter() {
                val text = searchField.text
                if (text.trim().isEmpty()) tableSorter.rowFilter = null
                else tableSorter.rowFilter = RowFilter.regexFilter("(?i)$text")
            }
        })
    }

    private fun getCombinedDateTime(): LocalDateTime {
        val datePart = (dateSpinner.value as Date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val timePart = (timeSpinner.value as Date).toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
        return LocalDateTime.of(datePart, timePart)
    }

    private fun setCombinedDateTime(dt: LocalDateTime) {
        val date = Date.from(dt.atZone(ZoneId.systemDefault()).toInstant())
        dateSpinner.value = date
        timeSpinner.value = date
    }

    private fun createEvent() {
        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as? VenueItem
        val description = descriptionArea.text.trim()
        val maxParticipants = maxParticipantsField.text.toIntOrNull()

        if (title.isBlank() || venueItem == null || maxParticipants == null) {
            JOptionPane.showMessageDialog(this, "Title, Venue, and Capacity are required.", "Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        val hours = (hoursSpinner.value as Number).toLong()
        val minutes = (minutesSpinner.value as Number).toLong()
        val duration = Duration.ofHours(hours).plusMinutes(minutes)
        val dateTime = getCombinedDateTime()

        val event = Event(
            id = UUID.randomUUID().toString(),
            title = title,
            dateTime = dateTime,
            venue = venueItem.venue,
            description = description,
            duration = duration,
            maxParticipants = maxParticipants
        )

        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.addEvent(event)
            override fun done() {
                if (get()) {
                    refreshEventTable()
                    clearFields()
                    JOptionPane.showMessageDialog(this@EventPanel, "Event created!", "Success", JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this@EventPanel, "Failed to create event (Conflict or Capacity).", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        worker.execute()
    }

    private fun updateEvent() {
        val id = editingEventId ?: return
        val oldEvent = eventManager.getEventById(id) ?: return

        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as? VenueItem ?: return
        val description = descriptionArea.text.trim()
        val maxParticipants = maxParticipantsField.text.toIntOrNull() ?: oldEvent.maxParticipants

        val hours = (hoursSpinner.value as Number).toLong()
        val minutes = (minutesSpinner.value as Number).toLong()
        val duration = Duration.ofHours(hours).plusMinutes(minutes)
        val dateTime = getCombinedDateTime()

        val updatedEvent = Event(
            id = id,
            title = title,
            dateTime = dateTime,
            venue = venueItem.venue,
            description = description,
            duration = duration,
            maxParticipants = maxParticipants
        )

        // Keep participants
        oldEvent.getRegisteredParticipants().forEach { updatedEvent.registerParticipant(it) }

        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.modifyEvent(updatedEvent)
            override fun done() {
                if (get()) {
                    refreshEventTable()
                    clearFields()
                    JOptionPane.showMessageDialog(this@EventPanel, "Event updated!", "Success", JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this@EventPanel, "Update failed.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        worker.execute()
    }

    private fun loadEventForEditing() {
        val row = eventTable.selectedRow
        if (row == -1) return
        val event = displayedEvents.getOrNull(eventTable.convertRowIndexToModel(row)) ?: return

        editingEventId = event.id
        saveButton.text = "Update Event"
        titleField.text = event.title
        descriptionArea.text = event.description
        maxParticipantsField.text = event.maxParticipants.toString()

        setCombinedDateTime(event.dateTime)

        hoursSpinner.value = event.duration.toHours().toInt()
        minutesSpinner.value = event.duration.toMinutesPart()

        for (i in 0 until venueCombo.itemCount) {
            val item = venueCombo.getItemAt(i) as VenueItem
            if (item.venue.id == event.venue.id) {
                venueCombo.selectedIndex = i
                break
            }
        }
    }

    private fun deleteSelectedEvent() {
        val row = eventTable.selectedRow
        if (row == -1) return
        val event = displayedEvents.getOrNull(eventTable.convertRowIndexToModel(row)) ?: return

        if (JOptionPane.showConfirmDialog(this, "Delete '${event.title}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.deleteEvent(event)
                override fun done() {
                    if (get()) refreshEventTable() else JOptionPane.showMessageDialog(this@EventPanel, "Delete failed.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            worker.execute()
        }
    }

    private fun findAvailableVenue() {
        try {
            val cap = maxParticipantsField.text.toIntOrNull()
            if (cap == null) {
                JOptionPane.showMessageDialog(this, "Enter capacity first.", "Info", JOptionPane.WARNING_MESSAGE)
                return
            }
            val hours = (hoursSpinner.value as Number).toLong()
            val minutes = (minutesSpinner.value as Number).toLong()
            val dur = Duration.ofHours(hours).plusMinutes(minutes)
            val dt = getCombinedDateTime()

            findVenueBtn.isEnabled = false
            findVenueBtn.text = "..."

            val worker = object : SwingWorker<ScalaBridge.SlotFinderResult, Void>() {
                override fun doInBackground() = ScalaBridge.findAvailableVenues(eventManager.getAllVenues(), eventManager.getAllEvents(), cap, dt, dur)
                override fun done() {
                    findVenueBtn.isEnabled = true
                    findVenueBtn.text = "Find Slot"
                    try {
                        when (val result = get()) {
                            is ScalaBridge.SlotFinderResult.Success -> {
                                if (result.slots.isEmpty()) JOptionPane.showMessageDialog(this@EventPanel, "No slots found.", "Info", JOptionPane.INFORMATION_MESSAGE)
                                else {
                                    val sb = StringBuilder("Available Slots:\n\n")
                                    val fmt = DateTimeFormatter.ofPattern("HH:mm")
                                    result.slots.forEach {
                                        sb.append("• ${it.venue.name} (${it.venue.capacity})\n   ${it.startTime.format(fmt)} - ${it.freeUntil.format(fmt)}\n")
                                    }
                                    val area = UIStyles.createTextArea(10, 30).apply { text = sb.toString(); isEditable = false }
                                    JOptionPane.showMessageDialog(this@EventPanel, UIStyles.createScrollPane(area), "Slots", JOptionPane.PLAIN_MESSAGE)
                                }
                            }
                            is ScalaBridge.SlotFinderResult.Error -> JOptionPane.showMessageDialog(this@EventPanel, result.message, "Error", JOptionPane.ERROR_MESSAGE)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            worker.execute()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0
        val dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val tF = DateTimeFormatter.ofPattern("HH:mm")
        displayedEvents = eventManager.getAllEvents()
        displayedEvents.forEach { e ->
            tableModel.addRow(arrayOf(e.title, e.dateTime.format(dF), e.dateTime.format(tF), e.venue.name, "${e.getCurrentCapacity()}/${e.maxParticipants}"))
        }
    }

    private fun refreshVenueCombo() {
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venueCombo.addItem(VenueItem(it)) }
    }

    private fun clearFields() {
        titleField.text = ""
        descriptionArea.text = ""
        maxParticipantsField.text = ""
        venueCombo.selectedIndex = -1
        editingEventId = null
        saveButton.text = "Save Event"
        eventTable.clearSelection()

        // Reset spinners
        val now = Date()
        dateSpinner.value = now
        timeSpinner.value = now
        hoursSpinner.value = 2
        minutesSpinner.value = 0
    }

    private data class VenueItem(val venue: Venue) { override fun toString() = venue.name }

    private class CapacityCellRenderer : DefaultTableCellRenderer() {
        private val progressBar = JProgressBar(0, 100)
        init {
            progressBar.isStringPainted = true
            progressBar.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            progressBar.background = UIStyles.cardBackground
            progressBar.foreground = UIStyles.accentGreen // Bar Color
        }
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            if (isSelected) progressBar.background = UIStyles.tableSelection else progressBar.background = UIStyles.cardBackground
            val strValue = value as? String ?: "0/0"
            try {
                val parts = strValue.split("/")
                if (parts.size == 2) {
                    val c = parts[0].trim().toInt(); val m = parts[1].trim().toInt()
                    progressBar.maximum = m; progressBar.value = c
                    progressBar.string = "$c / $m"

                    // Bar Color Logic
                    progressBar.foreground = if(c >= m) UIStyles.accentRed else UIStyles.accentGreen

                    // --- FORCE WHITE TEXT ---
                    // This forces text to render cleanly on dark background
                    progressBar.ui.installUI(progressBar)
                }
            } catch(e:Exception){}
            return progressBar
        }
    }
}