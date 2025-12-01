package com.eventplanning.ui

import com.eventplanning.domain.Event
import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Participant
import javax.swing.*
import javax.swing.border.EmptyBorder
import java.awt.*
import java.time.format.DateTimeFormatter

class RegistrationPanel(private val eventManager: EventManager) : JPanel() {

    // Data Models
    private val registeredListModel = DefaultListModel<String>()
    private val registeredList = JList(registeredListModel)

    // Styled Inputs
    private val participantCombo = UIStyles.createComboBox()
    private val eventCombo = UIStyles.createComboBox()
    private val eventDetailsArea = UIStyles.createTextArea(10, 30)

    // Theme Tracking
    private val formLabels = mutableListOf<JLabel>()
    private val headerLabel = UIStyles.createHeaderLabel("Registration Desk")
    private val sectionForm = UIStyles.createSectionLabel("NEW REGISTRATION")
    private val sectionList = UIStyles.createSectionLabel("ATTENDEE LIST")
    private val sectionDetails = UIStyles.createSectionLabel("EVENT DETAILS")

    // Styled Buttons
    private val registerBtn = UIStyles.createPrimaryButton("Register")
    private val unregisterBtn = UIStyles.createDangerButton("Unregister")
    private val refreshBtn = UIStyles.createSecondaryButton("Refresh Data")

    init {
        layout = BorderLayout(0, 20)
        isOpaque = false

        add(headerLabel, BorderLayout.NORTH)

        val content = JPanel(GridBagLayout())
        content.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.BOTH; insets = Insets(0, 0, 0, 20) }

        gbc.gridx = 0; gbc.weightx = 0.6; gbc.weighty = 1.0
        content.add(createLeftPanel(), gbc)

        gbc.gridx = 1; gbc.weightx = 0.4; gbc.insets = Insets(0, 0, 0, 0)
        content.add(createDetailsCard(), gbc)

        add(content, BorderLayout.CENTER)

        applyTheme()
        setupListeners()
        refreshCombos()
    }

    fun applyTheme() {
        headerLabel.foreground = UIStyles.textPrimary
        sectionForm.foreground = UIStyles.textMuted
        sectionList.foreground = UIStyles.textMuted
        sectionDetails.foreground = UIStyles.textMuted

        formLabels.forEach { it.foreground = UIStyles.textSecondary }

        // FIXED: Apply deep styling with custom renderer
        UIStyles.styleComboBox(participantCombo)
        UIStyles.styleComboBox(eventCombo)

        // List
        registeredList.apply {
            background = UIStyles.cardBackground
            foreground = UIStyles.textPrimary
            selectionBackground = UIStyles.tableSelection
            selectionForeground = UIStyles.accentGreen
        }

        // Text Area
        eventDetailsArea.background = UIStyles.inputBackground
        eventDetailsArea.foreground = UIStyles.textPrimary
        eventDetailsArea.border = BorderFactory.createLineBorder(UIStyles.tableBorder)

        this.repaint()
    }

    private fun createLeftPanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 20))
        panel.isOpaque = false

        // 1. Registration Form Card
        val formCard = UIStyles.createCardPanel()
        formCard.add(sectionForm, BorderLayout.NORTH)

        val form = JPanel(GridBagLayout())
        form.isOpaque = false
        val gbc = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; insets = Insets(0, 0, 15, 0); weightx = 1.0; gridx = 0 }
        var y = 0

        fun addIn(text: String, comp: JComponent) {
            gbc.gridy = y++
            val lbl = UIStyles.createLabel(text)
            formLabels.add(lbl)
            form.add(lbl, gbc)
            gbc.gridy = y++
            form.add(comp, gbc)
        }

        addIn("Select Participant:", participantCombo)
        addIn("Select Event:", eventCombo)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        btnPanel.isOpaque = false
        btnPanel.add(registerBtn); btnPanel.add(unregisterBtn); btnPanel.add(refreshBtn)

        gbc.gridy = y++; gbc.weighty = 1.0
        form.add(JPanel().apply { isOpaque = false }, gbc)
        gbc.gridy = y++; gbc.weighty = 0.0
        form.add(btnPanel, gbc)

        formCard.add(form, BorderLayout.CENTER)
        panel.add(formCard, BorderLayout.NORTH)

        // 2. Attendee List Card
        val listCard = UIStyles.createCardPanel()
        listCard.add(sectionList, BorderLayout.NORTH)
        registeredList.font = UIStyles.fontBody
        registeredList.border = EmptyBorder(5, 5, 5, 5)
        listCard.add(UIStyles.createScrollPane(registeredList), BorderLayout.CENTER)
        panel.add(listCard, BorderLayout.CENTER)

        return panel
    }

    private fun createDetailsCard(): JPanel {
        val card = UIStyles.createCardPanel()
        card.add(sectionDetails, BorderLayout.NORTH)
        eventDetailsArea.isEditable = false
        eventDetailsArea.font = Font("Monospaced", Font.PLAIN, 13)
        card.add(UIStyles.createScrollPane(eventDetailsArea), BorderLayout.CENTER)
        return card
    }

    // --- LOGIC ---
    private fun setupListeners() {
        refreshBtn.addActionListener { refreshCombos() }
        registerBtn.addActionListener { registerParticipant() }
        unregisterBtn.addActionListener { unregisterParticipant() }
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
        if (participantItem == null || eventItem == null) { showError("Select both."); return }
        val participant = participantItem.participant; val event = eventItem.event
        if (event.isFull()) { showError("Full."); return }
        if (event.isParticipantRegistered(participant)) { showInfo("Already registered."); return }
        registerBtn.isEnabled = false
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean { event.registerParticipant(participant); return eventManager.updateEvent(event) }
            override fun done() {
                registerBtn.isEnabled = true
                if (get()) { showSuccess("Registered!"); updateRegisteredList(event); updateEventDetails(event) }
                else { event.unregisterParticipant(participant); showError("Failed.") }
            }
        }
        worker.execute()
    }

    private fun unregisterParticipant() {
        val participantItem = participantCombo.selectedItem as? ParticipantItem
        val eventItem = eventCombo.selectedItem as? EventItem
        if (participantItem == null || eventItem == null) return
        val participant = participantItem.participant; val event = eventItem.event
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean { event.unregisterParticipant(participant); return eventManager.updateEvent(event) }
            override fun done() { if (get()) { updateRegisteredList(event); updateEventDetails(event); showSuccess("Unregistered!") } }
        }
        worker.execute()
    }

    private fun updateRegisteredList(event: Event) {
        registeredListModel.clear()
        event.getRegisteredParticipants().forEach { p -> registeredListModel.addElement("• ${p.name} (${p.email})") }
    }

    private fun updateEventDetails(event: Event) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val hours = event.duration.toMinutes() / 60
        val minutes = event.duration.toMinutes() % 60
        val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        eventDetailsArea.text = buildString {
            appendLine("📅 SUMMARY"); appendLine("────────────────────────────────")
            appendLine("Title:     ${event.title}")
            appendLine("Date:      ${event.dateTime.format(formatter)}")
            appendLine("Venue:     ${event.venue.name}")
            appendLine("Location:  ${event.venue.address}")
            appendLine("Duration:  $durationStr")
            appendLine()
            appendLine("📊 STATISTICS"); appendLine("────────────────────────────────")
            appendLine("Status:    ${if (event.isFull()) "FULL" else "Open"}")
            appendLine("Occupancy: ${event.getCurrentCapacity()} / ${event.maxParticipants}")
            appendLine("Spots:     ${event.getAvailableSpots()}")
            if (event.description.isNotBlank()) {
                // FIXED: Truncated line fixed
                appendLine(); appendLine("📝 DESCRIPTION"); appendLine("────────────────────────────────"); appendLine(event.description)
            }
        }
        eventDetailsArea.caretPosition = 0
    }

    private fun refreshCombos() {
        participantCombo.removeAllItems()
        eventManager.getAllParticipants().forEach { participantCombo.addItem(ParticipantItem(it)) }
        val selectedEvent = (eventCombo.selectedItem as? EventItem)?.event
        eventCombo.removeAllItems()
        eventManager.getAllEvents().forEach { eventCombo.addItem(EventItem(it)) }
        if (selectedEvent != null) {
            for (i in 0 until eventCombo.itemCount) {
                val item = eventCombo.getItemAt(i) as? EventItem
                if (item != null && item.event.id == selectedEvent.id) { eventCombo.selectedIndex = i; break }
            }
        }
    }

    private data class ParticipantItem(val participant: Participant) { override fun toString() = "${participant.name} (${participant.email})" }
    private data class EventItem(val event: Event) { override fun toString() = event.title }
    private fun showSuccess(msg: String) = JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE)
    private fun showError(msg: String) = JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE)
    private fun showInfo(msg: String) = JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE)
}