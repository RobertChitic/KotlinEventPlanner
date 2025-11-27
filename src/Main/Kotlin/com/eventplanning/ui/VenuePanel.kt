package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.util.UUID

/**
 * Panel for managing venues (create, view, delete).
 */
class VenuePanel(private val eventManager: EventManager) : JPanel() {

    private var displayedVenues: List<Venue> = emptyList()

    private val tableModel = object : DefaultTableModel(
        arrayOf("Name", "Capacity", "Location", "Address", "Facilities"), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val venueTable = JTable(tableModel)

    // Form fields
    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(20)
    private val facilitiesField = JTextField(20)

    // Buttons
    private val addButton = JButton("Add Venue")
    private val clearButton = JButton("Clear")
    private val deleteButton = JButton("Delete Venue")

    init {
        layout = BorderLayout()
        setupTable()
        setupButtons()

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = createTablePanel()
            rightComponent = createFormPanel()
            dividerLocation = 550
            resizeWeight = 0.6
        }

        add(splitPane, BorderLayout.CENTER)

        refreshVenueTable()
        setupListeners()
    }

    private fun setupTable() {
        venueTable.apply {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            rowHeight = 28
            showHorizontalLines = true
            gridColor = Color.LIGHT_GRAY
            fillsViewportHeight = true
        }
    }

    private fun setupButtons() {
        deleteButton.apply {
            foreground = Color.WHITE
            background = Color(220, 53, 69)
            isOpaque = true
        }

        addButton.apply {
            foreground = Color.WHITE
            background = Color(40, 167, 69)
            isOpaque = true
        }
    }

    private fun createTablePanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Venue List")
            add(JScrollPane(venueTable), BorderLayout.CENTER)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            buttonPanel.add(deleteButton)
            add(buttonPanel, BorderLayout.SOUTH)
        }
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("New Venue Details")

        val gbc = GridBagConstraints().apply {
            insets = Insets(8, 10, 8, 10)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }

        var gridY = 0

        fun addRow(label: String, component: JComponent, tooltip: String? = null) {
            gbc.gridx = 0
            gbc.gridy = gridY
            gbc.weightx = 0.0
            panel.add(JLabel(label), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            component.toolTipText = tooltip
            panel.add(component, gbc)
            gridY++
        }

        addRow("Name:*", nameField, "Enter venue name")
        addRow("Capacity:*", capacityField, "Maximum number of people")
        addRow("Location:*", locationField, "City or area")
        addRow("Address:*", addressField, "Full street address")
        addRow("Facilities:", facilitiesField, "Comma-separated (e.g., WiFi, Projector)")

        // Spacer
        gbc.gridy = gridY
        gbc.weighty = 1.0
        panel.add(JPanel(), gbc)

        // Buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(addButton)

        gbc.gridy = gridY + 1
        gbc.weighty = 0.0
        gbc.gridx = 0
        gbc.gridwidth = 2
        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        addButton.addActionListener { addVenue() }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener { deleteSelectedVenue() }
    }

    private fun addVenue() {
        val name = nameField.text.trim()
        val capacityStr = capacityField.text.trim()
        val location = locationField.text.trim()
        val address = addressField.text.trim()
        val facilitiesStr = facilitiesField.text.trim()

        // Validation
        if (name.isBlank() || capacityStr.isBlank() || location.isBlank() || address.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "Please fill in all required fields (marked with *).",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val capacity = capacityStr.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            JOptionPane.showMessageDialog(
                this,
                "Capacity must be a positive number.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val facilities = if (facilitiesStr.isBlank()) {
            emptyList()
        } else {
            facilitiesStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

        try {
            val venue = Venue(
                id = UUID.randomUUID().toString(),
                name = name,
                capacity = capacity,
                location = location,
                address = address,
                facilities = facilities
            )

            addButton.isEnabled = false

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.addVenue(venue)

                override fun done() {
                    addButton.isEnabled = true
                    try {
                        if (get()) {
                            refreshVenueTable()
                            clearFields()
                            JOptionPane.showMessageDialog(
                                this@VenuePanel,
                                "Venue '${venue.name}' added successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        } else {
                            JOptionPane.showMessageDialog(
                                this@VenuePanel,
                                "Failed to add venue.  It may already exist.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            this@VenuePanel,
                            "Error: ${e.message}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            worker.execute()
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(
                this,
                "Validation Error: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun deleteSelectedVenue() {
        val selectedRow = venueTable.selectedRow
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a venue to delete.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val venue = displayedVenues.getOrNull(selectedRow) ?: return

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete '${venue.name}'?\n\nThis action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            deleteButton.isEnabled = false

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.deleteVenue(venue)

                override fun done() {
                    deleteButton.isEnabled = true
                    try {
                        if (get()) {
                            refreshVenueTable()
                            JOptionPane.showMessageDialog(
                                this@VenuePanel,
                                "Venue deleted successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        } else {
                            JOptionPane.showMessageDialog(
                                this@VenuePanel,
                                "Cannot delete venue.\nIt may be in use by existing events.",
                                "Delete Failed",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            this@VenuePanel,
                            "Error: ${e.message}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            worker.execute()
        }
    }

    private fun refreshVenueTable() {
        tableModel.rowCount = 0
        displayedVenues = eventManager.getAllVenues()
        displayedVenues.forEach { venue ->
            tableModel.addRow(arrayOf(
                venue.name,
                venue.capacity,
                venue.location,
                venue.address,
                venue.getFacilitiesDisplay()
            ))
        }
    }

    private fun clearFields() {
        nameField.text = ""
        capacityField.text = ""
        locationField.text = ""
        addressField.text = ""
        facilitiesField.text = ""
        nameField.requestFocus()
    }
}