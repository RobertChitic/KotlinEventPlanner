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
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Capacity", "Location", "Address"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val venueTable = JTable(tableModel)

    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(20)

    private val addButton = JButton("Add Venue")
    private val clearButton = JButton("Clear")
    private val deleteButton = JButton("❌ Delete Venue")

    init {
        layout = BorderLayout()
        venueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        venueTable.rowHeight = 25
        venueTable.showHorizontalLines = true
        venueTable.gridColor = Color.LIGHT_GRAY
        venueTable.fillsViewportHeight = true

        deleteButton.foreground = Color.BLACK
        deleteButton.background = Color(220, 53, 69)
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 500
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

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("New Venue Details")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10, 10, 10, 10); gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL
        var gridY = 0
        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.0; panel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0; panel.add(component, gbc); gridY++
        }
        addRow("Name:", nameField); addRow("Capacity:", capacityField); addRow("Location:", locationField); addRow("Address:", addressField)
        gbc.gridy = gridY; gbc.weighty = 1.0; panel.add(JPanel(), gbc) // Spacer
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); buttonPanel.add(clearButton); buttonPanel.add(addButton)
        gbc.gridy = gridY + 1; gbc.weighty = 0.0; panel.add(buttonPanel, gbc)
        return panel
    }

    private fun setupListeners() {
        addButton.addActionListener { addVenue() }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener { deleteSelectedVenue() }
    }

    private fun addVenue() {
        val name = nameField.text.trim()
        val capStr = capacityField.text.trim()
        if (name.isBlank() || capStr.isBlank()) { JOptionPane.showMessageDialog(this, "Missing fields", "Error", JOptionPane.WARNING_MESSAGE); return }
        try {
            val venue = Venue(UUID.randomUUID().toString(), name, capStr.toInt(), locationField.text.trim(), addressField.text.trim())
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.addVenue(venue)
                override fun done() { if(get()) { refreshVenueTable(); clearFields(); JOptionPane.showMessageDialog(this@VenuePanel, "Added!", "Success", JOptionPane.INFORMATION_MESSAGE) } }
            }
            worker.execute()
        } catch (e: Exception) { JOptionPane.showMessageDialog(this, e.message, "Error", JOptionPane.ERROR_MESSAGE) }
    }

    private fun deleteSelectedVenue() {
        val row = venueTable.selectedRow
        if (row == -1) return
        val venue = displayedVenues.getOrNull(row) ?: return
        if (JOptionPane.showConfirmDialog(this, "Delete '${venue.name}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.deleteVenue(venue)
                override fun done() { if(get()) refreshVenueTable() else JOptionPane.showMessageDialog(this@VenuePanel, "Cannot delete (Used in Event)", "Error", JOptionPane.ERROR_MESSAGE) }
            }
            worker.execute()
        }
    }

    private fun refreshVenueTable() {
        tableModel.rowCount = 0
        displayedVenues = eventManager.getAllVenues()
        displayedVenues.forEach { tableModel.addRow(arrayOf(it.name, it.capacity, it.location, it.address)) }
    }

    private fun clearFields() { nameField.text=""; capacityField.text=""; locationField.text=""; addressField.text="" }
}