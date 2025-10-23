package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import com.eventplanning.persistence.DataStore
import javax.swing.*
import java.awt.*
import java.time.format.DateTimeFormatter

class RegistrationPanel(
    private val eventManager: EventManager,
    private val dataStore: DataStore
) : JPanel() {
    private val registeredListModel = DefaultListModel<String>()
    private val registeredList = JList(registeredListModel)

    private val participantCombo = JComboBox<ParticipantItem>()
    private val eventCombo = JComboBox<EventItem>()
    private val eventDetailsArea = JTextArea(5, 30)

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        add(createRegistrationPanel(), BorderLayout.NORTH)
        add(createRegisteredListPanel(), BorderLayout.CENTER)
        add(createEventDetailsPanel(), BorderLayout.EAST)

        refreshCombos()
        setupEventComboListener()
    }

    private fun createRegistrationPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Register Participant to Event")

        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        // Participant Selection
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Select Participant:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2
        panel.add(participantCombo, gbc)

        // Event Selection
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1
        panel.add(JLabel("Select Event:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2
        panel.add(eventCombo, gbc)

        // Buttons Panel
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.CENTER
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 5))

        val refreshBtn = JButton("🔄 Refresh")
        refreshBtn.addActionListener { refreshCombos() }
        buttonPanel.add(refreshBtn)

        val registerBtn = JButton("✓ Register")
        registerBtn.addActionListener { registerParticipant() }
        buttonPanel.add(registerBtn)

        val unregisterBtn = JButton("✗ Unregister")
        unregisterBtn.addActionListener { unregisterParticipant() }
        buttonPanel.add(unregisterBtn)

        panel.add(buttonPanel, gbc)

        return panel
    }

    private fun createRegisteredListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Registered Participants for Selected Event")

        registeredList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scrollPane = JScrollPane(registeredList)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createEventDetailsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")
        panel.preferredSize = Dimension(300, 0)

        eventDetailsArea.isEditable = false
        eventDetailsArea.lineWrap = true
        eventDetailsArea.wrapStyleWord = true
        eventDetailsArea.font = Font("Monospaced", Font.PLAIN, 12)

        val scrollPane = JScrollPane(eventDetailsArea)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun setupEventComboListener() {
        eventCombo.addActionListener {
            val selectedEvent = (eventCombo.selectedItem as? EventItem)?.event
            if (selectedEvent != null) {
                updateRegisteredList(selectedEvent)
                updateEventDetails(selectedEvent)
            }
        }
    }

    private fun registerParticipant() {
        val participantItem = participantCombo.selectedItem as? ParticipantItem
        val eventItem = eventCombo.selectedItem as? EventItem

        if (participantItem == null || eventItem == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select both a participant and an event",
                "Selection Error",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val participant = participantItem.participant
        val event = eventItem.event

        try {
            // Attempt to register using the Event's built-in method
            if (event.registerParticipant(participant)) {
                // Save the updated event to database
                if (dataStore.saveEvent(event)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Successfully registered ${participant.name} to ${event.title}",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Registration successful but failed to save to database",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
                updateRegisteredList(event)
                updateEventDetails(event)
            } else {
                // Registration failed - check why
                if (event.isFull()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Event is at full capacity (${event.maxParticipants} participants)",
                        "Capacity Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                } else if (event.isParticipantRegistered(participant)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "${participant.name} is already registered for this event",
                        "Already Registered",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to register participant",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Error: ${e.message}",
                "Registration Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun unregisterParticipant() {
        val selectedIndex = registeredList.selectedIndex
        val eventItem = eventCombo.selectedItem as? EventItem

        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a participant from the list to unregister",
                "Selection Error",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        if (eventItem == null) {
            JOptionPane.showMessageDialog(
                this,
                "No event selected",
                "Selection Error",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val event = eventItem.event
        val participants = event.getRegisteredParticipants()

        if (selectedIndex >= participants.size) {
            JOptionPane.showMessageDialog(
                this,
                "Invalid selection",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val participant = participants[selectedIndex]

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Unregister ${participant.name} from ${event.title}?",
            "Confirm Unregistration",
            JOptionPane.YES_NO_OPTION
        )

        if (confirm == JOptionPane.YES_OPTION) {
            if (event.unregisterParticipant(participant)) {
                // Save the updated event to database
                if (dataStore.saveEvent(event)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Successfully unregistered ${participant.name}",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Unregistration successful but failed to save to database",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
                updateRegisteredList(event)
                updateEventDetails(event)
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to unregister participant",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun updateRegisteredList(event: Event) {
        registeredListModel.clear()
        val participants = event.getRegisteredParticipants()
        participants.forEach { participant ->
            registeredListModel.addElement(
                "${participant.name} - ${participant.email} (${participant.organization})"
            )
        }
    }

    private fun updateEventDetails(event: Event) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val registeredCount = event.getCurrentCapacity()
        val availableSpots = event.getAvailableSpots()
        val percentFull = (registeredCount.toDouble() / event.maxParticipants * 100).toInt()

        val details = buildString {
            appendLine("Event: ${event.title}")
            appendLine("=".repeat(40))
            appendLine()
            appendLine("📅 Date/Time:")
            appendLine("   ${event.dateTime.format(formatter)}")
            appendLine()
            appendLine("📍 Venue:")
            appendLine("   ${event.venue.name}")
            appendLine("   ${event.venue.location}")
            appendLine()
            appendLine("👥 Capacity:")
            appendLine("   Registered: $registeredCount")
            appendLine("   Maximum: ${event.maxParticipants}")
            appendLine("   Available: $availableSpots")
            appendLine("   Filled: $percentFull%")
            appendLine()
            if (event.description.isNotBlank()) {
                appendLine("📝 Description:")
                appendLine("   ${event.description}")
            }
        }

        eventDetailsArea.text = details
        eventDetailsArea.caretPosition = 0
    }

    private fun refreshCombos() {
        // Refresh participants
        participantCombo.removeAllItems()
        eventManager.getAllParticipants().forEach { participant ->
            participantCombo.addItem(ParticipantItem(participant))
        }

        // Refresh events
        val selectedEvent = (eventCombo.selectedItem as? EventItem)?.event
        eventCombo.removeAllItems()
        eventManager.getAllEvents().forEach { event ->
            eventCombo.addItem(EventItem(event))
        }

        // Restore selection if possible
        if (selectedEvent != null) {
            for (i in 0 until eventCombo.itemCount) {
                if (eventCombo.getItemAt(i).event.id == selectedEvent.id) {
                    eventCombo.selectedIndex = i
                    break
                }
            }
        }
    }

    private data class ParticipantItem(val participant: Participant) {
        override fun toString(): String =
            "${participant.name} (${participant.email})"
    }

    private data class EventItem(val event: Event) {
        override fun toString(): String {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            return "${event.title} - ${event.dateTime.format(formatter)}"
        }
    }
}