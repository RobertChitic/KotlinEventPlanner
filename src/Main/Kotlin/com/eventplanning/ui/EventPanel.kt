package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax. swing.event.PopupMenuListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import java.awt.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util. Date
import java.util.UUID

/**
 * Panel for managing events (create, view, delete, find slots).
 */
class EventPanel(private val eventManager: EventManager) : JPanel() {

    private var displayedEvents: List<Event> = emptyList()

    private val tableModel = object : DefaultTableModel(
        arrayOf("Title", "Date", "Time", "Venue", "Occupancy"), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val eventTable = JTable(tableModel)
    private val tableSorter = TableRowSorter(tableModel)

    // Form fields
    private val searchField = JTextField(15)
    private val titleField = JTextField(20)
    private val startDateSpinner = JSpinner(SpinnerDateModel())
    private val hoursSpinner = JSpinner(SpinnerNumberModel(2, 0, 24, 1))
    private val minutesSpinner = JSpinner(SpinnerNumberModel(0, 0, 59, 15))
    private val venueCombo = JComboBox<VenueItem>()
    private val venueInfoLabel = JLabel("Select a venue...")
    private val descriptionArea = JTextArea(5, 20)
    private val maxParticipantsField = JTextField(10)

    // Buttons
    private val createButton = JButton("Create Event")
    private val clearButton = JButton("Clear")
    private val findVenueBtn = JButton("Find Slot (Scala)")
    private val deleteButton = JButton("Delete Event")

    init {
        layout = BorderLayout()
        setupComponents()

        val splitPane = JSplitPane(JSplitPane. HORIZONTAL_SPLIT).apply {
            leftComponent = createTablePanel()
            rightComponent = createFormPanel()
            dividerLocation = 600
            resizeWeight = 0.7
        }

        add(splitPane, BorderLayout.CENTER)

        setupListeners()
        refreshEventTable()
        refreshVenueCombo()
    }

    private fun setupComponents() {
        eventTable.apply {
            rowSorter = tableSorter
            setSelectionMode(ListSelectionModel. SINGLE_SELECTION)
            rowHeight = 30
            showHorizontalLines = true
            gridColor = Color.LIGHT_GRAY
            fillsViewportHeight = true
            columnModel. getColumn(4).cellRenderer = CapacityCellRenderer()
        }

        searchField.putClientProperty("JTextField. placeholderText", "Filter events...")
        venueInfoLabel.foreground = Color. GRAY

        deleteButton.apply {
            foreground = Color.WHITE
            background = Color(220, 53, 69)
            isOpaque = true
        }

        createButton.apply {
            foreground = Color. WHITE
            background = Color(40, 167, 69)
            isOpaque = true
        }

        findVenueBtn.apply {
            foreground = Color.WHITE
            background = Color(0, 123, 255)
            isOpaque = true
        }

        // Date spinner setup
        val dateEditor = JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm")
        startDateSpinner.editor = dateEditor
        startDateSpinner.value = Date()

        // Description area setup
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
    }

    /**
     * Custom cell renderer that displays capacity as a colored progress bar.
     */
    private class CapacityCellRenderer : DefaultTableCellRenderer() {
        private val progressBar = JProgressBar(0, 100). apply {
            isStringPainted = true
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                BorderFactory. createLineBorder(Color. LIGHT_GRAY)
            )
            isOpaque = true
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            progressBar.background = if (isSelected) table?. selectionBackground else Color.WHITE

            val strValue = value as? String ?: "0/0"
            try {
                val parts = strValue.split("/")
                if (parts.size == 2) {
                    val current = parts[0]. toInt()
                    val max = parts[1].toInt()

                    progressBar.maximum = max
                    progressBar.value = current
                    progressBar. string = "$current / $max"

                    val percent = if (max > 0) (current. toDouble() / max * 100).toInt() else 0
                    progressBar. foreground = when {
                        percent >= 100 -> Color(220, 53, 69)  // Red - Full
                        percent >= 75 -> Color(255, 193, 7)   // Yellow - Almost full
                        else -> Color(40, 167, 69)            // Green - Available
                    }
                }
            } catch (e: Exception) {
                progressBar.string = strValue
            }
            return progressBar
        }
    }

    private fun createTablePanel(): JPanel {
        return JPanel(BorderLayout()). apply {
            border = BorderFactory.createTitledBorder("Event Schedule")

            val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            topPanel. add(JLabel("Filter:"))
            topPanel.add(searchField)
            add(topPanel, BorderLayout.NORTH)

            add(JScrollPane(eventTable), BorderLayout.CENTER)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            buttonPanel.add(deleteButton)
            add(buttonPanel, BorderLayout.SOUTH)
        }
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")

        val gbc = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints. WEST
            fill = GridBagConstraints.HORIZONTAL
        }

        var gridY = 0

        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0
            gbc.gridy = gridY
            gbc.weightx = 0.0
            gbc.gridwidth = 1
            panel.add(JLabel(label), gbc)

            gbc.gridx = 1
            gbc.weightx = 1. 0
            panel.add(component, gbc)
            gridY++
        }

        addRow("Title:*", titleField)
        addRow("Start Time:*", startDateSpinner)

        // Duration panel
        val durationPanel = JPanel(FlowLayout(FlowLayout. LEFT, 0, 0)). apply {
            add(hoursSpinner)
            add(JLabel(" hours "))
            add(minutesSpinner)
            add(JLabel(" minutes"))
        }
        addRow("Duration:*", durationPanel)

        addRow("Venue:*", venueCombo)

        // Venue info label
        gbc.gridx = 1
        gbc. gridy = gridY
        panel.add(venueInfoLabel, gbc)
        gridY++

        addRow("Max Capacity:*", maxParticipantsField)

        // Description area
        gbc.gridx = 0
        gbc. gridy = gridY
        gbc.gridwidth = 2
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints. BOTH
        panel.add(JScrollPane(descriptionArea). apply {
            border = BorderFactory.createTitledBorder("Description")
        }, gbc)

        // Button panel
        gridY++
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)). apply {
            add(clearButton)
            add(findVenueBtn)
            add(createButton)
        }

        gbc.gridy = gridY
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        createButton.addActionListener { createEvent() }
        findVenueBtn. addActionListener { findAvailableVenue() }
        clearButton.addActionListener { clearFields() }
        deleteButton. addActionListener { deleteSelectedEvent() }

        // Update venue info when selection changes
        venueCombo.addActionListener {
            val venue = (venueCombo. selectedItem as? VenueItem)?. venue
            if (venue != null) {
                venueInfoLabel.text = "Capacity: ${venue.capacity}"
                venueInfoLabel. foreground = Color(40, 167, 69)
            } else {
                venueInfoLabel.text = "Select a venue..."
                venueInfoLabel.foreground = Color.GRAY
            }
        }

        // Refresh venue combo when dropdown opens
        venueCombo.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) { refreshVenueCombo() }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })

        // Search filter
        searchField. document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = applyFilter()
            override fun removeUpdate(e: DocumentEvent?) = applyFilter()
            override fun changedUpdate(e: DocumentEvent?) = applyFilter()

            private fun applyFilter() {
                val text = searchField.text
                if (text.trim().isEmpty()) {
                    tableSorter.rowFilter = null
                } else {
                    tableSorter. rowFilter = RowFilter.regexFilter("(? i)$text")
                }
            }
        })
    }

    private fun refreshEventTable() {
        tableModel.rowCount = 0
        val dateFormatter = DateTimeFormatter. ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        displayedEvents = eventManager.getAllEvents().sortedBy { it. dateTime }
        displayedEvents.forEach { event ->
            val endTime = event.getEndTime()
            tableModel.addRow(arrayOf(
                event.title,
                event.dateTime. format(dateFormatter),
                "${event.dateTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                event.venue. name,
                "${event.getCurrentCapacity()}/${event.maxParticipants}"
            ))
        }
    }

    private fun refreshVenueCombo() {
        val selected = venueCombo.selectedItem
        venueCombo.removeAllItems()
        eventManager.getAllVenues().forEach { venueCombo.addItem(VenueItem(it)) }
        if (selected != null) venueCombo.selectedItem = selected
    }

    private fun createEvent() {
        val title = titleField.text.trim()
        val venueItem = venueCombo.selectedItem as?  VenueItem
        val description = descriptionArea. text. trim()
        val capacity = maxParticipantsField.text.toIntOrNull()

        // Validation
        if (title.isBlank()) {
            showError("Please enter an event title.")
            return
        }

        if (venueItem == null) {
            showError("Please select a venue.")
            return
        }

        if (capacity == null || capacity <= 0) {
            showError("Please enter a valid capacity (positive number).")
            return
        }

        if (capacity > venueItem.venue.capacity) {
            showError("Capacity cannot exceed venue capacity (${venueItem. venue.capacity}).")
            return
        }

        val hours = (hoursSpinner.value as Number).toLong()
        val minutes = (minutesSpinner.value as Number).toLong()
        val duration = Duration. ofHours(hours). plusMinutes(minutes)

        if (duration.isZero || duration.isNegative) {
            showError("Please enter a valid duration.")
            return
        }

        val dateTime = LocalDateTime. ofInstant(
            (startDateSpinner. value as Date).toInstant(),
            ZoneId.systemDefault()
        )

        try {
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTime = dateTime,
                venue = venueItem.venue,
                description = description,
                duration = duration,
                maxParticipants = capacity
            )

            // Check for conflicts
            if (eventManager.getAllEvents().any { it.conflictsWith(event) }) {
                showError("This event conflicts with an existing event at the same venue.")
                return
            }

            createButton.isEnabled = false

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.addEvent(event)

                override fun done() {
                    createButton. isEnabled = true
                    try {
                        if (get()) {
                            refreshEventTable()
                            clearFields()
                            JOptionPane.showMessageDialog(
                                this@EventPanel,
                                "Event created successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        } else {
                            showError("Failed to create event.")
                        }
                    } catch (e: Exception) {
                        showError("Error: ${e.message}")
                    }
                }
            }
            worker.execute()
        } catch (e: IllegalArgumentException) {
            showError("Validation Error: ${e.message}")
        }
    }

    private fun deleteSelectedEvent() {
        val viewRow = eventTable.selectedRow
        if (viewRow == -1) {
            showError("Please select an event to delete.")
            return
        }

        val modelRow = eventTable.convertRowIndexToModel(viewRow)
        val eventToDelete = displayedEvents.getOrNull(modelRow) ?: return

        val confirm = JOptionPane. showConfirmDialog(
            this,
            "Are you sure you want to delete '${eventToDelete. title}'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane. YES_OPTION) {
            deleteButton.isEnabled = false

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.deleteEvent(eventToDelete)

                override fun done() {
                    deleteButton.isEnabled = true
                    if (get()) {
                        refreshEventTable()
                        JOptionPane.showMessageDialog(
                            this@EventPanel,
                            "Event deleted successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        showError("Failed to delete event.")
                    }
                }
            }
            worker. execute()
        }
    }

    private fun findAvailableVenue() {
        val capacity = maxParticipantsField.text.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            showError("Please enter a valid capacity to find available venues.")
            return
        }

        val hours = (hoursSpinner.value as Number).toLong()
        val minutes = (minutesSpinner. value as Number).toLong()
        val duration = Duration.ofHours(hours). plusMinutes(minutes)

        val dateTime = LocalDateTime. ofInstant(
            (startDateSpinner.value as Date).toInstant(),
            ZoneId.systemDefault()
        )

        findVenueBtn. isEnabled = false

        val worker = object : SwingWorker<ScalaBridge. SlotFinderResult, Void>() {
            override fun doInBackground(): ScalaBridge.SlotFinderResult {
                return ScalaBridge.findAvailableVenues(
                    eventManager.