package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import javax.swing.*
import java.awt.*
import java.util.UUID

class VenuePanel(
    private val eventManager: EventManager // Changed: Removed DataStore
) : JPanel() {
    private val venueListModel = DefaultListModel<String>()
    private val venueList = JList(venueListModel)

    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(30)
    private val addButton = JButton("Add Venue") // Promoted to class level

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(createFormPanel(), BorderLayout.NORTH)
        add(createListPanel(), BorderLayout.CENTER)
        refreshVenueList()
    }

    private fun createFormPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder("Add New Venue")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST

        // ... (Layout code same as original) ...
        gbc.gridx = 0; gbc.gridy = 0; panel.add(JLabel("Venue Name:"), gbc)
        gbc.gridx = 1; panel.add(nameField, gbc)
        gbc.gridx = 0; gbc.gridy = 1; panel.add(JLabel("Capacity:"), gbc)
        gbc.gridx = 1; panel.add(capacityField, gbc)
        gbc.gridx = 0; gbc.gridy = 2; panel.add(JLabel("Location:"), gbc)
        gbc.gridx = 1; panel.add(locationField, gbc)
        gbc.gridx = 0; gbc.gridy = 3; panel.add(JLabel("Address:"), gbc)
        gbc.gridx = 1; panel.add(addressField, gbc)

        gbc.gridx = 1; gbc.gridy = 4
        addButton.addActionListener { addVenue() }
        panel.add(addButton, gbc)
        return panel
    }

    private fun createListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Existing Venues")
        venueList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        panel.add(JScrollPane(venueList), BorderLayout.CENTER)
        return panel
    }

    // CHANGED: SwingWorker for responsiveness
    private fun addVenue() {
        try {
            val name = nameField.text.trim()
            val capacity = capacityField.text.toInt()
            val location = locationField.text.trim()
            val address = addressField.text.trim()

            val venue = Venue(
                id = UUID.randomUUID().toString(),
                name = name,
                capacity = capacity,
                location = location,
                address = address
            )

            addButton.isEnabled = false
            addButton.text = "Saving..."

            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean {
                    return eventManager.addVenue(venue) // DB save happens here now
                }

                override fun done() {
                    addButton.isEnabled = true
                    addButton.text = "Add Venue"
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(this@VenuePanel, "Venue Added!", "Success", JOptionPane.INFORMATION_MESSAGE)
                            clearFields()
                            refreshVenueList()
                        } else {
                            JOptionPane.showMessageDialog(this@VenuePanel, "Failed to save venue", "Error", JOptionPane.ERROR_MESSAGE)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            worker.execute()

        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(this, "Invalid Capacity", "Error", JOptionPane.ERROR_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshVenueList() {
        venueListModel.clear()
        eventManager.getAllVenues().forEach { venue ->
            venueListModel.addElement("${venue.name} - Capacity: ${venue.capacity} - ${venue.location}")
        }
    }

    private fun clearFields() {
        nameField.text = ""; capacityField.text = ""; locationField.text = ""; addressField.text = ""
    }
}