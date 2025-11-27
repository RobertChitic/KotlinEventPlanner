package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.util.UUID

class ParticipantPanel(
    private val eventManager: EventManager
) : JPanel() {

    // --- TABLE (Left Side) ---
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Email", "Organization", "Phone"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }
    private val participantTable = JTable(tableModel)

    // --- FORM (Right Side) ---
    private val nameField = JTextField(20)
    private val emailField = JTextField(20)
    private val phoneField = JTextField(15)
    private val organizationField = JTextField(20)
    private val addButton = JButton("Add Participant")
    private val clearButton = JButton("Clear Form")

    init {
        layout = BorderLayout()

        // 1. Table Styling
        participantTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        participantTable.rowHeight = 25
        participantTable.showHorizontalLines = true
        participantTable.gridColor = Color.LIGHT_GRAY
        participantTable.fillsViewportHeight = true

        // 2. Placeholders
        nameField.putClientProperty("JTextField.placeholderText", "Full Name")
        emailField.putClientProperty("JTextField.placeholderText", "user@example.com")
        phoneField.putClientProperty("JTextField.placeholderText", "Optional")
        organizationField.putClientProperty("JTextField.placeholderText", "Company / Uni")

        // 3. Split Layout
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createTablePanel()
        splitPane.rightComponent = createFormPanel()
        splitPane.dividerLocation = 550
        splitPane.resizeWeight = 0.6

        add(splitPane, BorderLayout.CENTER)

        // 4. Data
        refreshParticipantTable()
        setupListeners()
    }

    private fun createTablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Participants Directory")
        panel.add(JScrollPane(participantTable), BorderLayout.CENTER)
        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Register New Person")
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
        addRow("Email:", emailField)
        addRow("Phone:", phoneField)
        addRow("Organization:", organizationField)

        // Spacer to push buttons down
        gbc.gridy = gridY; gbc.weighty = 1.0
        panel.add(JPanel(), gbc)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        buttonPanel.add(addButton)

        gbc.gridy = gridY + 1; gbc.weighty = 0.0
        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun setupListeners() {
        addButton.addActionListener { addParticipant() }
        clearButton.addActionListener { clearFields() }
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
            val p = Participant(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                phone = phone,
                organization = org
            )

            addButton.isEnabled = false
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.addParticipant(p)
                override fun done() {
                    addButton.isEnabled = true
                    if (get()) {
                        JOptionPane.showMessageDialog(this@ParticipantPanel, "Participant Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
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

    private fun refreshParticipantTable() {
        tableModel.rowCount = 0
        eventManager.getAllParticipants().forEach { p ->
            tableModel.addRow(arrayOf(p.name, p.email, p.organization, p.phone))
        }
    }

    private fun clearFields() {
        nameField.text = ""
        emailField.text = ""
        phoneField.text = ""
        organizationField.text = ""
    }
}