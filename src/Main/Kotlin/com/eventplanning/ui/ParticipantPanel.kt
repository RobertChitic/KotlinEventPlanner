package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import java.awt.*
import java.util.UUID

class ParticipantPanel(
    private val eventManager: EventManager
) : JPanel() {

    private var displayedParticipants: List<Participant> = emptyList()

    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Email", "Organization", "Phone"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val participantTable = JTable(tableModel)
    private val tableSorter = TableRowSorter(tableModel)

    // Form
    private val searchField = JTextField(15)
    private val nameField = JTextField(20)
    private val emailField = JTextField(20)
    private val phoneField = JTextField(15)
    private val organizationField = JTextField(20)
    private val addButton = JButton("Add Participant")
    private val clearButton = JButton("Clear")
    private val deleteButton = JButton("❌ Delete Participant")

    init {
        layout = BorderLayout()

        participantTable.rowSorter = tableSorter
        participantTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        participantTable.rowHeight = 25
        participantTable.showHorizontalLines = true
        participantTable.gridColor = Color.LIGHT_GRAY
        participantTable.fillsViewportHeight = true

        searchField.putClientProperty("JTextField.placeholderText", "🔍 Filter...")

        deleteButton.foreground = Color.WHITE
        deleteButton.background = Color(220, 53, 69)
        deleteButton.isContentAreaFilled = false
        deleteButton.isOpaque = true

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 550
        splitPane.resizeWeight = 0.6

        add(splitPane, BorderLayout.CENTER)

        refreshParticipantTable()
        setupListeners()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Participants Directory")

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.add(JLabel("Filter:"))
        topPanel.add(searchField)
        panel.add(topPanel, BorderLayout.NORTH)

        panel.add(JScrollPane(participantTable), BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        btnPanel.add(deleteButton)
        panel.add(btnPanel, BorderLayout.SOUTH)

        return panel
    }

    // FIXED: Safe GridBagLayout logic here too
    private fun createFormPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = BorderFactory.createTitledBorder("Register New Person")

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

        resetGBC(0, 1); formPanel.add(JLabel("Email:"), gbc)
        resetGBC(1, 1); gbc.weightx = 1.0; formPanel.add(emailField, gbc)

        resetGBC(0, 2); formPanel.add(JLabel("Phone:"), gbc)
        resetGBC(1, 2); gbc.weightx = 1.0; formPanel.add(phoneField, gbc)

        resetGBC(0, 3); formPanel.add(JLabel("Organization:"), gbc)
        resetGBC(1, 3); gbc.weightx = 1.0; formPanel.add(organizationField, gbc)

        // Push everything up to avoid vertical centering
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
        addButton.addActionListener { addParticipant() }
        clearButton.addActionListener { clearFields() }
        deleteButton.addActionListener { deleteSelectedParticipant() }

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

    private fun addParticipant() {
        val name = nameField.text.trim()
        val email = emailField.text.trim()
        val phone = phoneField.text.trim()
        val org = organizationField.text.trim()

        if (name.isBlank() || email.isBlank()) {
            JOptionPane.showMessageDialog(this, "Name and Email are required.", "Validation", JOptionPane.WARNING_MESSAGE)
            return
        }

        try {
            val p = Participant(UUID.randomUUID().toString(), name, email, phone, org)
            addButton.isEnabled = false
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.addParticipant(p)
                override fun done() {
                    addButton.isEnabled = true
                    if (get()) {
                        JOptionPane.showMessageDialog(this@ParticipantPanel, "Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
                        refreshParticipantTable()
                        clearFields()
                    } else {
                        JOptionPane.showMessageDialog(this@ParticipantPanel, "Failed (Duplicate email?)", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }
            worker.execute()
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun deleteSelectedParticipant() {
        val viewRow = participantTable.selectedRow
        if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Select a participant.", "Error", JOptionPane.WARNING_MESSAGE); return }
        val modelRow = participantTable.convertRowIndexToModel(viewRow)
        val participantToDelete = displayedParticipants.getOrNull(modelRow) ?: return

        if (JOptionPane.showConfirmDialog(this, "Delete '${participantToDelete.name}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.deleteParticipant(participantToDelete)
                override fun done() { if (get()) { refreshParticipantTable(); JOptionPane.showMessageDialog(this@ParticipantPanel, "Deleted.", "Success", JOptionPane.INFORMATION_MESSAGE) } else JOptionPane.showMessageDialog(this@ParticipantPanel, "Failed.", "Error", JOptionPane.ERROR_MESSAGE) }
            }
            worker.execute()
        }
    }

    private fun refreshParticipantTable() {
        tableModel.rowCount = 0
        displayedParticipants = eventManager.getAllParticipants()
        displayedParticipants.forEach { p ->
            tableModel.addRow(arrayOf(p.name, p.email, p.organization, p.phone))
        }
    }

    private fun clearFields() { nameField.text=""; emailField.text=""; phoneField.text=""; organizationField.text=""; participantTable.clearSelection() }
}