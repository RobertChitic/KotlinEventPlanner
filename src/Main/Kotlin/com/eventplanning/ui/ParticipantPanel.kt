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

    private val searchField = JTextField(15)
    private val nameField = JTextField(20)
    private val emailField = JTextField(20)
    private val phoneField = JTextField(15)
    private val organizationField = JTextField(20)

    private val addButton = JButton("Add Participant")
    private val clearButton = JButton("Clear")
    private val deleteButton = JButton("Delete Participant")

    init {
        layout = BorderLayout()
        participantTable.rowSorter = tableSorter
        participantTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        participantTable.rowHeight = 25
        participantTable.showHorizontalLines = true
        participantTable.gridColor = Color.LIGHT_GRAY
        participantTable.fillsViewportHeight = true

        searchField.putClientProperty("JTextField.placeholderText", "Filter...")

        // Red Box Delete Button
        deleteButton.apply {
            foreground = Color.WHITE
            background = Color(220, 53, 69)
            isOpaque = true
        }

        // Green Add Button
        addButton.apply {
            foreground = Color.WHITE
            background = Color(40, 167, 69) // Green
            isOpaque = true
        }

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
        val top = JPanel(FlowLayout(FlowLayout.LEFT)); top.add(JLabel("Filter:")); top.add(searchField)
        panel.add(top, BorderLayout.NORTH)
        panel.add(JScrollPane(participantTable), BorderLayout.CENTER)
        val bot = JPanel(FlowLayout(FlowLayout.LEFT)); bot.add(deleteButton)
        panel.add(bot, BorderLayout.SOUTH)
        return panel
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Register New Person")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10, 10, 10, 10); gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL
        var gridY = 0
        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.0; panel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0; panel.add(component, gbc); gridY++
        }
        addRow("Name:", nameField); addRow("Email:", emailField); addRow("Phone:", phoneField); addRow("Organization:", organizationField)
        gbc.gridy = gridY; gbc.weighty = 1.0; panel.add(JPanel(), gbc)
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); buttonPanel.add(clearButton); buttonPanel.add(addButton)
        gbc.gridy = gridY + 1; gbc.weighty = 0.0; panel.add(buttonPanel, gbc)
        return panel
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
                if (text.trim().isEmpty()) tableSorter.setRowFilter(null) else tableSorter.setRowFilter(RowFilter.regexFilter("(?i)$text"))
            }
        })
    }

    private fun addParticipant() {
        val name = nameField.text.trim(); val email = emailField.text.trim()
        if (name.isBlank() || email.isBlank()) { JOptionPane.showMessageDialog(this, "Required: Name, Email", "Error", JOptionPane.WARNING_MESSAGE); return }
        val p = Participant(UUID.randomUUID().toString(), name, email, phoneField.text.trim(), organizationField.text.trim())
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground() = eventManager.addParticipant(p)
            override fun done() {
                if (get()) {
                    refreshParticipantTable()
                    clearFields()
                    JOptionPane.showMessageDialog(this@ParticipantPanel, "Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this@ParticipantPanel, "Failed to add participant (ID/Email may exist).", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        worker.execute()
    }

    private fun deleteSelectedParticipant() {
        val viewRow = participantTable.selectedRow
        if (viewRow == -1) return
        val modelRow = participantTable.convertRowIndexToModel(viewRow)
        val p = displayedParticipants.getOrNull(modelRow) ?: return
        if (JOptionPane.showConfirmDialog(this, "Delete '${p.name}'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.deleteParticipant(p)
                override fun done() { if(get()) refreshParticipantTable() else JOptionPane.showMessageDialog(this@ParticipantPanel, "Failed", "Error", JOptionPane.ERROR_MESSAGE) }
            }
            worker.execute()
        }
    }

    private fun refreshParticipantTable() {
        tableModel.rowCount = 0
        displayedParticipants = eventManager.getAllParticipants()
        displayedParticipants.forEach { tableModel.addRow(arrayOf(it.name, it.email, it.organization, it.phone)) }
    }

    private fun clearFields() { nameField.text=""; emailField.text=""; phoneField.text=""; organizationField.text="" }
}