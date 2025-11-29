package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import javax.swing.*
import java.awt.*

/**
 * Dashboard to show system-wide statistics.
 * This adds "Analytical" functionality to the application.
 */
class StatisticsPanel(private val eventManager: EventManager) : JPanel() {

    private val totalEventsLabel = createStatLabel()
    private val totalParticipantsLabel = createStatLabel()
    private val totalVenuesLabel = createStatLabel()
    private val busyVenueLabel = createStatLabel()
    private val avgOccupancyLabel = createStatLabel()

    private val refreshButton = JButton(" Refresh Statistics")

    init {
        layout = BorderLayout(20, 20)
        border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

        // Header
        val headerLabel = JLabel("System Dashboard", SwingConstants.CENTER)
        headerLabel.font = Font("SansSerif", Font.BOLD, 24)
        add(headerLabel, BorderLayout.NORTH)

        // Grid for Stats
        val statsPanel = JPanel(GridLayout(2, 3, 15, 15)) // 2 rows, 3 columns

        statsPanel.add(createCard("Total Events", totalEventsLabel))
        statsPanel.add(createCard("Total Participants", totalParticipantsLabel))
        statsPanel.add(createCard("Total Venues", totalVenuesLabel))
        statsPanel.add(createCard("Busiest Venue", busyVenueLabel))
        statsPanel.add(createCard("Avg. Occupancy", avgOccupancyLabel))

        // Add an empty panel for the 6th slot to keep layout clean
        val logoPanel = JPanel(BorderLayout())
        val hintLabel = JLabel("Event Planner v1.0", SwingConstants.CENTER)
        hintLabel.foreground = Color.GRAY
        logoPanel.add(hintLabel, BorderLayout.CENTER)
        logoPanel.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        statsPanel.add(logoPanel)

        add(statsPanel, BorderLayout.CENTER)

        // Refresh Button at bottom
        refreshButton.addActionListener { refreshStats() }
        refreshButton.font = Font("SansSerif", Font.BOLD, 14)
        val btnPanel = JPanel(); btnPanel.add(refreshButton)
        add(btnPanel, BorderLayout.SOUTH)

        // Initial Load
        refreshStats()
    }

    private fun createCard(title: String, valueLabel: JLabel): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )
        panel.background = Color.WHITE

        val titleLabel = JLabel(title)
        titleLabel.foreground = Color.DARK_GRAY
        titleLabel.font = Font("SansSerif", Font.PLAIN, 14)

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(valueLabel, BorderLayout.CENTER)

        return panel
    }

    private fun createStatLabel(): JLabel {
        val lbl = JLabel("...", SwingConstants.CENTER)
        lbl.font = Font("SansSerif", Font.BOLD, 28)
        lbl.foreground = Color(0, 102, 204) // Nice Blue
        return lbl
    }

    fun refreshStats() {
        val events = eventManager.getAllEvents()
        val participants = eventManager.getAllParticipants()
        val venues = eventManager.getAllVenues()

        // 1. Basic Counts
        totalEventsLabel.text = events.size.toString()
        totalParticipantsLabel.text = participants.size.toString()
        totalVenuesLabel.text = venues.size.toString()

        // 2. Busiest Venue (Venue with most events)
        if (events.isNotEmpty()) {
            val venuesMap = events.groupingBy { it.venue.name }.eachCount()
            val maxEntry = venuesMap.maxByOrNull { it.value }
            busyVenueLabel.text = if (maxEntry != null) {
                "<html><div style='text-align: center'>${maxEntry.key}<br/><span style='font-size:12px'>(${maxEntry.value} events)</span></div></html>"
            } else "N/A"
        } else {
            busyVenueLabel.text = "N/A"
        }

        // 3. Average Occupancy
        if (events.isNotEmpty()) {
            val totalCap = events.sumOf { it.maxParticipants }
            val totalReg = events.sumOf { it.getCurrentCapacity() }
            val percent = if (totalCap > 0) (totalReg.toDouble() / totalCap * 100).toInt() else 0
            avgOccupancyLabel.text = "$percent%"

            // Color code it
            avgOccupancyLabel.foreground = when {
                percent > 80 -> Color(0, 153, 51) // Green (High usage is good?)
                percent < 20 -> Color(204, 0, 0)  // Red (Low usage)
                else -> Color(0, 102, 204)
            }
        } else {
            avgOccupancyLabel.text = "0%"
        }
    }
}