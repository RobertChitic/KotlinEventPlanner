package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import javax.swing.*
import javax.swing.border.EmptyBorder
import java.awt.*

class StatisticsPanel(private val eventManager: EventManager) : JPanel() {

    private val headerLabel = UIStyles.createHeaderLabel("Dashboard Overview")

    // Labels tracking for color updates
    private val cardTitles = mutableListOf<JLabel>()

    // Value Labels
    private val totalEventsLabel = createStatLabel()
    private val totalParticipantsLabel = createStatLabel()
    private val totalVenuesLabel = createStatLabel()
    private val busyVenueLabel = createStatLabel()
    private val avgOccupancyLabel = createStatLabel()

    private val refreshBtn = JButton("Refresh Data")

    init {
        layout = BorderLayout(0, 30)
        isOpaque = false

        // 1. Header
        val headerPanel = JPanel(BorderLayout())
        headerPanel.isOpaque = false
        headerPanel.add(headerLabel, BorderLayout.WEST)

        refreshBtn.font = Font("Segoe UI", Font.PLAIN, 14)
        refreshBtn.isFocusPainted = false
        refreshBtn.addActionListener { refreshStats() }
        headerPanel.add(refreshBtn, BorderLayout.EAST)

        add(headerPanel, BorderLayout.NORTH)

        // 2. Grid of Cards
        val gridPanel = JPanel(GridLayout(2, 3, 25, 25))
        gridPanel.isOpaque = false

        gridPanel.add(createCard("Total Events", totalEventsLabel, UIStyles.accentBlue))
        gridPanel.add(createCard("Participants", totalParticipantsLabel, UIStyles.accentGreen))
        gridPanel.add(createCard("Total Venues", totalVenuesLabel, UIStyles.accentOrange))
        gridPanel.add(createCard("Busiest Venue", busyVenueLabel, UIStyles.accentPink))
        gridPanel.add(createCard("Avg. Occupancy", avgOccupancyLabel, UIStyles.accentPurple))
        gridPanel.add(createBrandCard())

        add(gridPanel, BorderLayout.CENTER)

        applyTheme() // Apply Initial Theme
        refreshStats()
    }

    fun applyTheme() {
        // Update Background & Text
        headerLabel.foreground = UIStyles.textPrimary

        // Update Card Titles (The small text)
        cardTitles.forEach { it.foreground = UIStyles.textSecondary }

        // Update Stat Numbers (The big text)
        val valueLabels = listOf(totalEventsLabel, totalParticipantsLabel, totalVenuesLabel, busyVenueLabel)
        valueLabels.forEach { it.foreground = UIStyles.textPrimary }

        // Update Refresh Button
        refreshBtn.background = UIStyles.cardBackground
        refreshBtn.foreground = UIStyles.textPrimary

        // IMPORTANT: Force Repaint of backgrounds (Cards)
        this.repaint()

        // IMPORTANT: Recalculate the "Color Logic" for Occupancy
        refreshStats()
    }

    private fun createCard(title: String, valueLabel: JLabel, accentColor: Color): JPanel {
        // Anonymous class to paint custom card background
        val card = object : JPanel(BorderLayout(0, 15)) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // DYNAMIC COLOR ACCESS VIA GETTER
                g2.color = UIStyles.cardBackground
                g2.fillRoundRect(0, 0, width, height, 20, 20)

                g2.color = accentColor
                g2.fillRoundRect(20, 0, 60, 6, 4, 4)
            }
        }
        card.isOpaque = false
        card.border = EmptyBorder(30, 30, 30, 30)

        val lblTitle = JLabel(title)
        lblTitle.font = Font("Segoe UI", Font.PLAIN, 14)
        cardTitles.add(lblTitle) // Track for theming

        card.add(lblTitle, BorderLayout.NORTH)
        valueLabel.horizontalAlignment = SwingConstants.LEFT
        card.add(valueLabel, BorderLayout.CENTER)

        return card
    }

    private fun createBrandCard(): JPanel {
        return object : JPanel(GridBagLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = UIStyles.cardBackground
                g2.fillRoundRect(0, 0, width, height, 20, 20)
            }
        }.apply {
            isOpaque = false
            val lbl = JLabel("Event Planner Pro")
            lbl.font = Font("Segoe UI", Font.BOLD, 18)
            lbl.foreground = Color(100, 100, 100)
            add(lbl)
        }
    }

    private fun createStatLabel(): JLabel {
        return JLabel("...").apply {
            font = Font("Segoe UI", Font.BOLD, 36)
            // Initial foreground set in applyTheme/refreshStats
        }
    }

    fun refreshStats() {
        val events = eventManager.getAllEvents()
        val participants = eventManager.getAllParticipants()
        val venues = eventManager.getAllVenues()

        totalEventsLabel.text = events.size.toString()
        totalParticipantsLabel.text = participants.size.toString()
        totalVenuesLabel.text = venues.size.toString()

        if (events.isNotEmpty()) {
            val venuesMap = events.groupingBy { it.venue.name }.eachCount()
            val maxEntry = venuesMap.maxByOrNull { it.value }
            busyVenueLabel.text = maxEntry?.key ?: "N/A"
            busyVenueLabel.font = if ((maxEntry?.key?.length ?: 0) > 12) Font("Segoe UI", Font.BOLD, 22) else Font("Segoe UI", Font.BOLD, 36)
        } else {
            busyVenueLabel.text = "N/A"
        }

        // Logic for coloring the occupancy
        if (events.isNotEmpty()) {
            val totalCap = events.sumOf { it.maxParticipants }
            val totalReg = events.sumOf { it.getCurrentCapacity() }
            val percent = if (totalCap > 0) (totalReg.toDouble() / totalCap * 100).toInt() else 0
            avgOccupancyLabel.text = "$percent%"
            avgOccupancyLabel.foreground = when {
                percent > 80 -> UIStyles.accentGreen
                percent < 20 -> UIStyles.accentOrange
                else -> UIStyles.textPrimary // Uses current theme text color
            }
        } else {
            avgOccupancyLabel.text = "0%"
            avgOccupancyLabel.foreground = UIStyles.textPrimary
        }
    }
}