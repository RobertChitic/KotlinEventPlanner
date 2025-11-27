package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.util.UUID

class VenuePanel(
    private val eventManager: EventManager
) : JPanel() {

    private var displayedVenues: List<Venue> = emptyList()

    // --- TABLE ---
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Capacity", "Location", "Address"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val venueTable = JTable(tableModel)

    // --- FORM ---
    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(20)

    private val addButton = JButton("Add Venue")
    private val clearButton = JButton("Clear")
    private val deleteButton = JButton("❌ Delete Venue")

    init {
        layout = BorderLayout()

        // 1. Table Styling
        venueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        venueTable.rowHeight = 28
        venueTable.showHorizontalLines = true
        venueTable.gridColor = Color.LIGHT_GRAY
        venueTable.fillsViewportHeight = true

        // 2. Placeholders
        nameField.putClientProperty("JTextField.placeholderText", "e.g. Grand Hall")
        capacityField.putClientProperty("JTextField.placeholderText", "e.g. 150")
        locationField.putClientProperty("JTextField.placeholderText", "e.g. Building A")
        addressField.putClientProperty("JTextField.placeholderText", "e.g. 123 Main St")

        // Style Delete Button
        deleteButton.foreground = Color.WHITE
        deleteButton.background = Color(220, 53, 69) // Red
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        // 3. Split Layout
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 550
        splitPane.resizeWeight = 0.6

        add(splitPane, BorderLayout.CENTER)

        refreshVenueTable()
        setupListeners()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Venue List")
        panel.add(JScrollPane(venueTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(deleteButton)
        panel.add(btnPanel, BorderLayout.SOUTH)
        return panel
    }

    // FIXED: Safer Layout Logic
    private fun createFormPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = BorderFactory.createTitledBorder("New Venue Details")

        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        fun resetGBC(x: Int, y: Int) {
            gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0.0
        }

        resetGBC(0, 0); formPanel.add(JLabel("Name:"), gbc)
        resetGBC(1, 0); gbc.weightx = 1.0; formPanel.add(nameField, gbc)

        resetGBC(0, 1); formPanel.add(JLabel("Capacity:"), gbc)
        resetGBC(1, 1); gbc.weightx = 1.0; formPanel.add(capacityField, gbc)

        resetGBC(0, 2); formPanel.add(JLabel("Location:"), gbc)
        resetGBC(1, 2); gbc.weightx = 1.0; formPanel.add(locationField, gbc)

        resetGBC(0, 3); formPanel.add(JLabel("Address:"), gbc)
        resetGBC(1, 3); gbc.weightx = 1.0; formPanel.add(addressField, gbc)

        // Spacer
        val spacer = JPanel()
        gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL
        formPanel.add(spacer, gbc)

        mainPanel.add(formPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(addButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        return mainPanel
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

        if (name.isBlank() || capacityStr.isBlank()) {
            JOptionPane.showMessageDialog(this, "Name and Capacity are required.", "Validation Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        try {
            val capacity = capacityStr.toInt()
            val venue = Venue(
                id = UUID.randomUUID().toString(),
                name = name,
                capacity = capacity,
                location = location,
                address = address
            )

            addButton.isEnabled = false
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.addVenue(venue)
                override fun done() {
                    addButton.isEnabled = true
                    if (get()) {
                        JOptionPane.showMessageDialog(this@VenuePanel, "Venue Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
                        refreshVenueTable()
                        clearFields()
                    } else {
                        JOptionPane.showMessageDialog(this@VenuePanel, "Failed to add venue.", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }
            worker.execute()

        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(this, "Capacity must be a number.", "Error", JOptionPane.ERROR_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun deleteSelectedVenue() {
        val selectedRow = venueTable.selectedRow
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a venue to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        val venueToDelete = displayedVenues.getOrNull(selectedRow) ?: return

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete '${venueToDelete.name}'?\nThis cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean {
                    return eventManager.deleteVenue(venueToDelete)
                }
                override fun done() {
                    if (get()) {
                        JOptionPane.showMessageDialog(this@VenuePanel, "Venue deleted.", "Success", JOptionPane.INFORMATION_MESSAGE)
                        refreshVenueTable()
                    } else {
                        JOptionPane.showMessageDialog(this@VenuePanel, "Cannot delete Venue.\nIt is currently assigned to an Event.", "Constraint Error", JOptionPane.ERROR_MESSAGE)
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
            tableModel.addRow(arrayOf(venue.name, venue.capacity, venue.location, venue.address))
        }
    }

    private fun clearFields() {
        nameField.text = ""
        capacityField.text = ""
        locationField.text = ""
        addressField.text = ""
        venueTable.clearSelection()
    }
}