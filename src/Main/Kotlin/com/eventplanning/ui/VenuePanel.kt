package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.domain.Venue
import com.eventplanning.persistence.DataStore
import javax.swing.*
import java.awt.*
import java.util.UUID

class VenuePanel(
    private val eventManager: EventManager,
    private val dataStore: DataStore
) : JPanel() {
    private val venueListModel = DefaultListModel<String>()
    private val venueList = JList(venueListModel)

    private val nameField = JTextField(20)
    private val capacityField = JTextField(10)
    private val locationField = JTextField(20)
    private val addressField = JTextField(30)

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

        // Name
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Venue Name:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(nameField, gbc)

        // Capacity
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Capacity:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(capacityField, gbc)

        // Location
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Location:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(locationField, gbc)

        // Address
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE
        panel.add(JLabel("Address:"), gbc)
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(addressField, gbc)

        // Add button
        gbc.gridx = 1; gbc.gridy = 4
        val addButton = JButton("Add Venue")
        addButton.addActionListener { addVenue() }
        panel.add(addButton, gbc)

        return panel
    }

    private fun createListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Existing Venues")

        venueList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scrollPane = JScrollPane(venueList)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

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

            if (eventManager.addVenue(venue)) {
                // Save to database immediately
                if (dataStore.saveVenue(venue)) {
                    JOptionPane.showMessageDialog(this,
                        "Venue added and saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Venue added but failed to save to database",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE)
                }
                clearFields()
                refreshVenueList()
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to add venue",
                    "Error",
                    JOptionPane.ERROR_MESSAGE)
            }
        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid number for capacity",
                "Input Error",
                JOptionPane.ERROR_MESSAGE)
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this,
                e.message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun refreshVenueList() {
        venueListModel.clear()
        eventManager.getAllVenues().forEach { venue ->
            venueListModel.addElement(
                "${venue.name} - Capacity: ${venue.capacity} - ${venue.location}"
            )
        }
    }

    private fun clearFields() {
        nameField.text = ""
        capacityField.text = ""
        locationField.text = ""
        addressField.text = ""
    }
}
