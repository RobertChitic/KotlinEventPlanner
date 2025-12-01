package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import java.awt.*
import java.util.UUID

class ParticipantPanel(private val eventManager: EventManager) : JPanel() {

    private var displayedParticipants: List<Participant> = emptyList()
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Email", "Organization", "Phone"), 0) { override fun isCellEditable(row: Int, column: Int) = false }
    private val participantTable = JTable(tableModel)
    private val tableSorter = TableRowSorter(tableModel)

    // Inputs
    private val searchField = UIStyles.createTextField(20)
    private val nameField = UIStyles.createTextField()
    private val emailField = UIStyles.createTextField()
    private val phoneField = UIStyles.createTextField()
    private val orgField = UIStyles.createTextField()

    // Buttons
    private val addButton = UIStyles.createPrimaryButton("Add Person")
    private val deleteButton = UIStyles.createDangerButton("Delete")

    init {
        layout = BorderLayout(0, 20)
        background = UIStyles.background
        isOpaque = false

        add(UIStyles.createHeaderLabel("Participants"), BorderLayout.NORTH)

        val content = JPanel(GridBagLayout()); content.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.BOTH; insets = Insets(0, 0, 0, 20) }

        gbc.gridx = 0; gbc.weightx = 0.65; gbc.weighty = 1.0
        content.add(createTableCard(), gbc)

        gbc.gridx = 1; gbc.weightx = 0.35; gbc.insets = Insets(0, 0, 0, 0)
        content.add(createFormCard(), gbc)

        add(content, BorderLayout.CENTER)
        refreshTable()
        setupListeners()
    }

    private fun createTableCard(): JPanel {
        val card = UIStyles.createCardPanel()

        val top = JPanel(BorderLayout()); top.isOpaque = false
        top.add(UIStyles.createSectionLabel("DIRECTORY"), BorderLayout.WEST)

        // Search box in header
        val searchPanel = JPanel(FlowLayout(FlowLayout.RIGHT)); searchPanel.isOpaque = false
        searchPanel.add(UIStyles.createLabel("Search:"))
        searchPanel.add(searchField)
        top.add(searchPanel, BorderLayout.EAST)
        card.add(top, BorderLayout.NORTH)

        UIStyles.styleTable(participantTable)
        participantTable.rowSorter = tableSorter
        card.add(UIStyles.createScrollPane(participantTable), BorderLayout.CENTER)

        val bot = JPanel(FlowLayout(FlowLayout.LEFT)); bot.isOpaque = false
        bot.add(deleteButton)
        card.add(bot, BorderLayout.SOUTH)

        return card
    }

    private fun createFormCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(UIStyles.createSectionLabel("REGISTER NEW"), BorderLayout.NORTH)

        val form = JPanel(GridBagLayout()); form.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; insets = Insets(0, 0, 15, 0); weightx = 1.0; gridx = 0 }
        var y = 0

        fun addIn(label: String, comp: JComponent) {
            gbc.gridy = y++; form.add(UIStyles.createLabel(label), gbc)
            gbc.gridy = y++; form.add(comp, gbc)
        }

        addIn("Full Name", nameField)
        addIn("Email Address", emailField)
        addIn("Phone Number", phoneField)
        addIn("Organization", orgField)

        gbc.gridy = y++; gbc.weighty = 1.0
        form.add(JPanel().apply { isOpaque = false }, gbc) // Spacer
        gbc.gridy = y++; gbc.weighty = 0.0

        val btnBox = JPanel(FlowLayout(FlowLayout.RIGHT)); btnBox.isOpaque = false
        btnBox.add(addButton)
        form.add(btnBox, gbc)

        card.add(form, BorderLayout.CENTER)
        return card
    }

    private fun setupListeners() {
        addButton.addActionListener {
            if(nameField.text.isBlank() || emailField.text.isBlank()) return@addActionListener
            val p = Participant(UUID.randomUUID().toString(), nameField.text, emailField.text, phoneField.text, orgField.text)
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground() = eventManager.addParticipant(p)
                override fun done() { if(get()) { refreshTable(); nameField.text=""; emailField.text=""; phoneField.text=""; orgField.text="" } }
            }
            worker.execute()
        }
        deleteButton.addActionListener {
            val row = participantTable.selectedRow
            if (row != -1) {
                val p = displayedParticipants[participantTable.convertRowIndexToModel(row)]
                val worker = object : SwingWorker<Boolean, Void>() {
                    override fun doInBackground() = eventManager.deleteParticipant(p)
                    override fun done() { if(get()) refreshTable() }
                }
                worker.execute()
            }
        }
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = filter()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = filter()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = filter()
            fun filter() {
                val text = searchField.text
                if (text.isBlank()) tableSorter.rowFilter = null
                else tableSorter.rowFilter = RowFilter.regexFilter("(?i)$text")
            }
        })
    }

    private fun refreshTable() {
        tableModel.rowCount = 0
        displayedParticipants = eventManager.getAllParticipants()
        displayedParticipants.forEach { tableModel.addRow(arrayOf(it.name, it.email, it.organization, it.phone)) }
    }
}