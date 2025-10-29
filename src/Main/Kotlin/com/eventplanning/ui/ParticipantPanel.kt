package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import com.eventplanning.persistence.DataStore
import javax.swing.*
import java.awt.*
import java.util.UUID

class ParticipantPanel(
    private val eventManager: EventManager,
    private val dataStore: DataStore
) : JPanel() {
    private val participantListModel = DefaultListModel<String>()
    private val participantList = JList(participantListModel)

    private val nameField = JTextField(20)
    private val emailField = JTextField(20)
    private val phoneField = JTextField(15)
    private val organizationField = JTextField(20)

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
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST

        // Name
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Name:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(nameField, gbc)

        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Email:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(emailField, gbc)

        // Phone
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Phone:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(phoneField, gbc)

        // Organization
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Organization:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(organizationField, gbc)

        // Add button
        gbc.gridx = 1; gbc.gridy = 4
        val addButton = JButton("Add Participant")
        addButton.addActionListener { addParticipant() }
        panel.add(addButton, gbc)

        return panel
    }

    private fun createListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Registered Participants")

        participantList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scrollPane = JScrollPane(participantList)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

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

            if (eventManager.addParticipant(participant)) {
                // Save to database immediately
                if (dataStore.saveParticipant(participant)) {
                    JOptionPane.showMessageDialog(this,
                        "Participant added and saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Participant added but failed to save to database",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE)
                }
                clearFields()
                refreshParticipantList()
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to add participant",
                    "Error",
                    JOptionPane.ERROR_MESSAGE)
            }
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this,
                e.message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshParticipantList() {
        participantListModel.clear()
        eventManager.getAllParticipants().forEach { participant ->
            participantListModel.addElement(
                "${participant.name} - ${participant.email} - ${participant.organization}"
            )
        }
    }

    private fun clearFields() {
        nameField.text = ""
        emailField.text = ""
        phoneField.text = ""
        organizationField.text = ""
    }
}
