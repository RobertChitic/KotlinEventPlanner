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

    // --- TABLE (Left Side) ---
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Capacity", "Location", "Address"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val venueTable = JTable(tableModel)

    // --- FORM (Right Side) ---
    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(20)
    private val addButton = JButton("Add Venue")
    private val clearButton = JButton("Clear Form")

    init {
        layout = BorderLayout()

        // 1. Setup Table
        venueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        venueTable.rowHeight = 25
        venueTable.showHorizontalLines = true
        venueTable.gridColor = Color.LIGHT_GRAY
        venueTable.fillsViewportHeight = true

        // 2. Setup Form Placeholders (FlatLaf Feature)
        nameField.putClientProperty("JTextField.placeholderText", "e.g. Grand Hall")
        capacityField.putClientProperty("JTextField.placeholderText", "e.g. 150")
        locationField.putClientProperty("JTextField.placeholderText", "e.g. Building A")
        addressField.putClientProperty("JTextField.placeholderText", "e.g. 123 Main St")

        // 3. Split Layout
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 500
        splitPane.resizeWeight = 0.6

        add(splitPane, BorderLayout.CENTER)

        // 4. Data & Listeners
        refreshVenueTable()
        setupListeners()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Venue List")
        panel.add(JScrollPane(venueTable), BorderLayout.CENTER)
        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("New Venue Details")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10, 10, 10, 10)
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

        addRow("Name:", nameField)
        addRow("Capacity:", capacityField)
        addRow("Location:", locationField)
        addRow("Address:", addressField)

        // Buttons pushed to bottom
        gbc.gridy = gridY; gbc.weighty = 1.0
        panel.add(JPanel(), gbc) // Spacer

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(addButton)

        gbc.gridy = gridY + 1; gbc.weighty = 0.0
        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        addButton.addActionListener { addVenue() }
        clearButton.addActionListener { clearFields() }
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

    private fun refreshVenueTable() {
        tableModel.rowCount = 0
        eventManager.getAllVenues().forEach { venue ->
            tableModel.addRow(arrayOf(venue.name, venue.capacity, venue.location, venue.address))
        }
    }

    private fun clearFields() {
        nameField.text = ""
        capacityField.text = ""
        locationField.text = ""
        addressField.text = ""
    }
}