package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import java.awt.*
import java.util.UUID

class ParticipantPanel(
    private val eventManager: EventManager // Changed: Removed DataStore
) : JPanel() {
    private val participantListModel = DefaultListModel<String>()
    private val participantList = JList(participantListModel)

    private val nameField = JTextField(20)
    private val emailField = JTextField(20)
    private val phoneField = JTextField(15)
    private val organizationField = JTextField(20)
    private val addButton = JButton("Add Participant") // Promoted

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(createFormPanel(), BorderLayout.NORTH)
        add(createListPanel(), BorderLayout.CENTER)
        refreshParticipantList()
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Add New Participant")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST

        // ... (Layout same as original) ...
        gbc.gridx = 0; gbc.gridy = 0; panel.add(JLabel("Name:"), gbc)
        gbc.gridx = 1; panel.add(nameField, gbc)
        gbc.gridx = 0; gbc.gridy = 1; panel.add(JLabel("Email:"), gbc)
        gbc.gridx = 1; panel.add(emailField, gbc)
        gbc.gridx = 0; gbc.gridy = 2; panel.add(JLabel("Phone:"), gbc)
        gbc.gridx = 1; panel.add(phoneField, gbc)
        gbc.gridx = 0; gbc.gridy = 3; panel.add(JLabel("Organization:"), gbc)
        gbc.gridx = 1; panel.add(organizationField, gbc)

        gbc.gridx = 1; gbc.gridy = 4
        addButton.addActionListener { addParticipant() }
        panel.add(addButton, gbc)
        return panel
    }

    private fun createListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Registered Participants")
        participantList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        panel.add(JScrollPane(participantList), BorderLayout.CENTER)
        return panel
    }

    // CHANGED: SwingWorker
    private fun addParticipant() {
        try {
            val name = nameField.text.trim()
            val email = emailField.text.trim()
            val phone = phoneField.text.trim()
            val organization = organizationField.text.trim()

            val participant = Participant(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                phone = phone,
                organization = organization
            )

            addButton.isEnabled = false

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean {
                    return eventManager.addParticipant(participant) // Saves to DB automatically
                }

                override fun done() {
                    addButton.isEnabled = true
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(this@ParticipantPanel, "Participant Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
                            clearFields()
                            refreshParticipantList()
                        } else {
                            JOptionPane.showMessageDialog(this@ParticipantPanel, "Failed (Duplicate email?)", "Error", JOptionPane.ERROR_MESSAGE)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            worker.execute()

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshParticipantList() {
        participantListModel.clear()
        eventManager.getAllParticipants().forEach { p ->
            participantListModel.addElement("${p.name} - ${p.email} - ${p.organization}")
        }
    }

    private fun clearFields() {
        nameField.text = ""; emailField.text = ""; phoneField.text = ""; organizationField.text = ""
    }
}