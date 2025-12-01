package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.util.UUID

class VenuePanel(private val eventManager: EventManager) : JPanel() {

    private var displayedVenues: List<Venue> = emptyList()
    private val tableModel = object : DefaultTableModel(
        arrayOf("Name", "Capacity", "Location", "Address", "Facilities"), 0
    ) { override fun isCellEditable(row: Int, column: Int) = false }
    private val venueTable = JTable(tableModel)

    // Inputs
    private val nameField = UIStyles.createTextField()
    private val capacityField = UIStyles.createTextField(10)
    private val locationField = UIStyles.createTextField()
    private val addressField = UIStyles.createTextField()
    private val facilitiesField = UIStyles.createTextField()

    // Buttons
    private val addButton = UIStyles.createPrimaryButton("Add Venue")
    private val clearButton = UIStyles.createSecondaryButton("Clear")
    private val deleteButton = UIStyles.createDangerButton("Delete Selected")

    init {
        layout = BorderLayout(0, 20)
        background = UIStyles.background
        isOpaque = false

        add(UIStyles.createHeaderLabel("Venues"), BorderLayout.NORTH)

        val contentPanel = JPanel(GridBagLayout())
        contentPanel.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.BOTH; insets = Insets(0, 0, 0, 20) }

        // Left: Table Card
        gbc.gridx = 0; gbc.weightx = 0.65; gbc.weighty = 1.0
        contentPanel.add(createTableCard(), gbc)

        // Right: Form Card
        gbc.gridx = 1; gbc.weightx = 0.35; gbc.insets = Insets(0, 0, 0, 0)
        contentPanel.add(createFormCard(), gbc)

        add(contentPanel, BorderLayout.CENTER)
        setupListeners()
        refreshVenueTable()
    }

    private fun createTableCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(UIStyles.createSectionLabel("ALL VENUES"), BorderLayout.NORTH)

        UIStyles.styleTable(venueTable)
        card.add(UIStyles.createScrollPane(venueTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); btnPanel.isOpaque = false
        btnPanel.add(deleteButton)
        card.add(btnPanel, BorderLayout.SOUTH)
        return card
    }

    private fun createFormCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(UIStyles.createSectionLabel("ADD NEW VENUE"), BorderLayout.NORTH)

        val form = JPanel(GridBagLayout()); form.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; insets = Insets(0, 0, 15, 0); weightx = 1.0; gridx = 0 }
        var y = 0

        fun addIn(label: String, comp: JComponent) {
            gbc.gridy = y++
            form.add(UIStyles.createLabel(label), gbc)
            gbc.gridy = y++
            form.add(comp, gbc)
        }

        addIn("Venue Name", nameField)
        addIn("Capacity", capacityField)
        addIn("Location", locationField)
        addIn("Address", addressField)
        addIn("Facilities", facilitiesField)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); btnPanel.isOpaque = false
        btnPanel.add(clearButton)
        btnPanel.add(addButton)

        gbc.gridy = y++; gbc.weighty = 1.0
        form.add(JPanel().apply { isOpaque = false }, gbc) // Spacer
        gbc.gridy = y++; gbc.weighty = 0.0
        form.add(btnPanel, gbc)

        card.add(form, BorderLayout.CENTER)
        return card
    }

    private fun setupListeners() {
        addButton.addActionListener {
            try {
                val cap = capacityField.text.trim().toIntOrNull() ?: throw IllegalArgumentException("Invalid Capacity")
                val venue = Venue(UUID.randomUUID().toString(), nameField.text.trim(), cap, locationField.text.trim(), addressField.text.trim(), facilitiesField.text.split(",").map { it.trim() })

                val worker = object : SwingWorker<Boolean, Void>() {
                    override fun doInBackground() = eventManager.addVenue(venue)
                    override fun done() {
                        if(get()) { refreshVenueTable(); clearFields() }
                        else JOptionPane.showMessageDialog(this@VenuePanel, "Failed to add.", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
                worker.execute()
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, e.message, "Validation", JOptionPane.WARNING_MESSAGE)
            }
        }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener {
            val row = venueTable.selectedRow
            if (row != -1) {
                val v = displayedVenues[venueTable.convertRowIndexToModel(row)]
                val worker = object : SwingWorker<Boolean, Void>() {
                    override fun doInBackground() = eventManager.deleteVenue(v)
                    override fun done() { if(get()) refreshVenueTable() }
                }
                worker.execute()
            }
        }
    }

    private fun refreshVenueTable() {
        tableModel.rowCount = 0
        displayedVenues = eventManager.getAllVenues()
        displayedVenues.forEach { tableModel.addRow(arrayOf(it.name, it.capacity, it.location, it.address, it.getFacilitiesDisplay())) }
    }

    private fun clearFields() { nameField.text=""; capacityField.text=""; locationField.text=""; addressField.text=""; facilitiesField.text="" }
}