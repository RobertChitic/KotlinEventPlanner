package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import java.awt.*
import java.util.UUID

class VenuePanel(private val eventManager: EventManager) : JPanel() {

    private var displayedVenues: List<Venue> = emptyList()
    private val tableModel = object : DefaultTableModel(
        arrayOf("Name", "Capacity", "Location", "Address", "Facilities"), 0
    ) { override fun isCellEditable(row: Int, column: Int) = false }
    private val venueTable = JTable(tableModel)

    // FIXED: Added Sorter
    private val tableSorter = TableRowSorter(tableModel)

    // Inputs
    private val searchField = UIStyles.createTextField(15).apply {
        putClientProperty("JTextField.placeholderText", "Search...")
    }
    private val nameField = UIStyles.createTextField()
    private val capacityField = UIStyles.createTextField(10)
    private val locationField = UIStyles.createTextField()
    private val addressField = UIStyles.createTextField()
    private val facilitiesField = UIStyles.createTextField()

    // Labels tracking for theming
    private val formLabels = mutableListOf<JLabel>()
    private val headerLabel = UIStyles.createHeaderLabel("Venues")
    private val sectionList = UIStyles.createSectionLabel("ALL VENUES")
    private val sectionAdd = UIStyles.createSectionLabel("ADD NEW VENUE")

    // Buttons
    private val addButton = UIStyles.createPrimaryButton("Add Venue")
    private val clearButton = UIStyles.createSecondaryButton("Clear")
    private val deleteButton = UIStyles.createDangerButton("Delete Selected")

    init {
        layout = BorderLayout(0, 20)
        isOpaque = false

        add(headerLabel, BorderLayout.NORTH)

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

        applyTheme() // Apply initial theme
        setupListeners()
        refreshVenueTable()
    }

    fun applyTheme() {
        // Update Labels
        headerLabel.foreground = UIStyles.textPrimary
        sectionList.foreground = UIStyles.textMuted
        sectionAdd.foreground = UIStyles.textMuted
        formLabels.forEach { it.foreground = UIStyles.textSecondary }

        // Update Inputs
        val inputs = listOf(nameField, capacityField, locationField, addressField, facilitiesField, searchField)
        inputs.forEach {
            it.background = UIStyles.inputBackground
            it.foreground = UIStyles.textPrimary
            it.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyles.tableBorder),
                EmptyBorder(8, 10, 8, 10)
            )
        }

        // Update Table
        UIStyles.styleTable(venueTable)

        // Redraw
        this.repaint()
    }

    private fun createTableCard(): JPanel {
        val card = UIStyles.createCardPanel()

        val header = JPanel(BorderLayout())
        header.isOpaque = false
        header.add(sectionList, BorderLayout.WEST)

        // FIXED: Added Search Field to UI
        val tools = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        tools.isOpaque = false
        tools.add(searchField)
        header.add(tools, BorderLayout.EAST)
        card.add(header, BorderLayout.NORTH)

        UIStyles.styleTable(venueTable)
        venueTable.rowSorter = tableSorter // FIXED: Assigned sorter

        card.add(UIStyles.createScrollPane(venueTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); btnPanel.isOpaque = false
        btnPanel.add(deleteButton)
        card.add(btnPanel, BorderLayout.SOUTH)
        return card
    }

    private fun createFormCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(sectionAdd, BorderLayout.NORTH)

        val form = JPanel(GridBagLayout()); form.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; insets = Insets(0, 0, 15, 0); weightx = 1.0; gridx = 0 }
        var y = 0

        fun addIn(text: String, comp: JComponent) {
            gbc.gridy = y++
            val label = UIStyles.createLabel(text)
            formLabels.add(label) // Track for theming
            form.add(label, gbc)
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

        // FIXED: Added Filter Listener
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filter()
            override fun removeUpdate(e: DocumentEvent?) = filter()
            override fun changedUpdate(e: DocumentEvent?) = filter()
            fun filter() {
                val text = searchField.text
                if (text.trim().isEmpty()) tableSorter.rowFilter = null else tableSorter.rowFilter = RowFilter.regexFilter("(?i)$text")
            }
        })
    }

    private fun refreshVenueTable() {
        tableModel.rowCount = 0
        displayedVenues = eventManager.getAllVenues()
        displayedVenues.forEach { tableModel.addRow(arrayOf(it.name, it.capacity, it.location, it.address, it.getFacilitiesDisplay())) }
    }

    private fun clearFields() { nameField.text=""; capacityField.text=""; locationField.text=""; addressField.text=""; facilitiesField.text="" }
}