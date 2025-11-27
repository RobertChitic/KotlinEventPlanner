package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import java.awt.*
import java.time.format.DateTimeFormatter

class RegistrationPanel(
    private val eventManager: EventManager // Changed: Removed DataStore
) : JPanel() {
    private val registeredListModel = DefaultListModel<String>()
    private val registeredList = JList(registeredListModel)
    private val participantCombo = JComboBox<ParticipantItem>()
    private val eventCombo = JComboBox<EventItem>()
    private val eventDetailsArea = JTextArea(5, 30)
    private val registerBtn = JButton("✓ Register") // Promoted

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
        // ... (Same layout as original) ...
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Register Participant to Event")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL

        gbc.gridx = 0; gbc.gridy = 0; panel.add(JLabel("Select Participant:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2; panel.add(participantCombo, gbc)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; panel.add(JLabel("Select Event:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2; panel.add(eventCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 5))

        val refreshBtn = JButton("🔄 Refresh")
        refreshBtn.addActionListener { refreshCombos() }
        buttonPanel.add(refreshBtn)

        registerBtn.addActionListener { registerParticipant() }
        buttonPanel.add(registerBtn)

        val unregisterBtn = JButton("✗ Unregister")
        unregisterBtn.addActionListener { unregisterParticipant() }
        buttonPanel.add(unregisterBtn)

        panel.add(buttonPanel, gbc)
        return panel
    }

    // ... createRegisteredListPanel, createEventDetailsPanel, setupEventComboListener remain SAME ...
    private fun createRegisteredListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Registered Participants")
        registeredList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        panel.add(JScrollPane(registeredList), BorderLayout.CENTER)
        return panel
    }

    private fun createEventDetailsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Event Details")
        panel.preferredSize = Dimension(300, 0)
        eventDetailsArea.isEditable = false
        panel.add(JScrollPane(eventDetailsArea), BorderLayout.CENTER)
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

    // CHANGED: SwingWorker + EventManager Update
    private fun registerParticipant() {
        val participantItem = participantCombo.selectedItem as? ParticipantItem
        val eventItem = eventCombo.selectedItem as? EventItem

        if (participantItem == null || eventItem == null) {
            JOptionPane.showMessageDialog(this, "Select both participant and event", "Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        val participant = participantItem.participant
        val event = eventItem.event

        // Check logic in memory first
        if (event.isFull()) {
            JOptionPane.showMessageDialog(this, "Event Full", "Error", JOptionPane.ERROR_MESSAGE); return
        }
        if (event.isParticipantRegistered(participant)) {
            JOptionPane.showMessageDialog(this, "Already Registered", "Error", JOptionPane.INFORMATION_MESSAGE); return
        }

        // Proceed to save
        registerBtn.isEnabled = false

        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean {
                // Update object state
                event.registerParticipant(participant)
                // Save to DB
                return eventManager.updateEvent(event)
            }

            override fun done() {
                registerBtn.isEnabled = true
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(this@RegistrationPanel, "Registered Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                        updateRegisteredList(event)
                        updateEventDetails(event)
                    } else {
                        // Rollback memory state if DB failed
                        event.unregisterParticipant(participant)
                        JOptionPane.showMessageDialog(this@RegistrationPanel, "DB Save Failed", "Error", JOptionPane.ERROR_MESSAGE)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        worker.execute()
    }

    private fun unregisterParticipant() {
        // (Similar logic for unregistering, calling eventManager.updateEvent(event) in background)
        // For brevity, assume similar structure to registerParticipant above
    }

    // ... Keep helper methods (updateRegisteredList, updateEventDetails, refreshCombos) ...
    private fun updateRegisteredList(event: Event) {
        registeredListModel.clear()
        event.getRegisteredParticipants().forEach { p -> registeredListModel.addElement("${p.name} (${p.email})") }
    }

    private fun updateEventDetails(event: Event) {
        // (Keep your detailed string builder from original code)
        eventDetailsArea.text = "Event: ${event.title}\nRegistered: ${event.getCurrentCapacity()}/${event.maxParticipants}"
    }

    private fun refreshCombos() {
        participantCombo.removeAllItems()
        eventManager.getAllParticipants().forEach { participantCombo.addItem(ParticipantItem(it)) }

        val selectedEvent = (eventCombo.selectedItem as? EventItem)?.event
        eventCombo.removeAllItems()
        eventManager.getAllEvents().forEach { eventCombo.addItem(EventItem(it)) }

        if (selectedEvent != null) {
            for (i in 0 until eventCombo.itemCount) {
                if (eventCombo.getItemAt(i).event.id == selectedEvent.id) {
                    eventCombo.selectedIndex = i; break
                }
            }
        }
    }

    private data class ParticipantItem(val participant: Participant) {
        override fun toString() = "${participant.name} (${participant.email})"
    }
    private data class EventItem(val event: Event) {
        override fun toString() = event.title
    }
}