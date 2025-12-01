package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import javax.swing.*
import javax.swing.border.EmptyBorder
import java.awt.*

class StatisticsPanel(private val eventManager: EventManager) : JPanel() {

    // --- MODERN COLORS ---
    private val cardBackground = Color(35, 35, 35)     // Elevated Card
    private val textPrimary = Color(255, 255, 255)     // White
    private val textSecondary = Color(179, 179, 179)   // Gray

    // Accents for Card Headers
    private val accentGreen = Color(30, 215, 96)
    private val accentBlue = Color(65, 105, 225)
    private val accentOrange = Color(255, 165, 0)
    private val accentPurple = Color(138, 43, 226)
    private val accentPink = Color(255, 105, 180)

    // Labels
    private val totalEventsLabel = createStatLabel()
    private val totalParticipantsLabel = createStatLabel()
    private val totalVenuesLabel = createStatLabel()
    private val busyVenueLabel = createStatLabel()
    private val avgOccupancyLabel = createStatLabel()

    init {
        layout = BorderLayout(0, 30)
        isOpaque = false // Let the dark window background show through

        // 1. Header
        val headerPanel = JPanel(BorderLayout())
        headerPanel.isOpaque = false

        val headerLabel = JLabel("Dashboard Overview")
        headerLabel.font = Font("Segoe UI", Font.BOLD, 28)
        headerLabel.foreground = textPrimary
        headerPanel.add(headerLabel, BorderLayout.WEST)

        val refreshBtn = JButton("Refresh Data")
        refreshBtn.font = Font("Segoe UI", Font.PLAIN, 14)
        refreshBtn.background = cardBackground
        refreshBtn.foreground = textPrimary
        refreshBtn.isFocusPainted = false
        refreshBtn.addActionListener { refreshStats() }
        headerPanel.add(refreshBtn, BorderLayout.EAST)

        add(headerPanel, BorderLayout.NORTH)

        // 2. Grid of Cards
        val gridPanel = JPanel(GridLayout(2, 3, 25, 25)) // Generous gap between cards
        gridPanel.isOpaque = false

        // Add Cards with distinct colored top bars (No Emojis)
        gridPanel.add(createCard("Total Events", totalEventsLabel, accentBlue))
        gridPanel.add(createCard("Participants", totalParticipantsLabel, accentGreen))
        gridPanel.add(createCard("Total Venues", totalVenuesLabel, accentOrange))
        gridPanel.add(createCard("Busiest Venue", busyVenueLabel, accentPink))
        gridPanel.add(createCard("Avg. Occupancy", avgOccupancyLabel, accentPurple))

        // Brand Card (Bottom Right)
        gridPanel.add(createBrandCard())

        add(gridPanel, BorderLayout.CENTER)

        refreshStats()
    }

    /**
     * Creates a dark card with a colored accent bar at the top.
     */
    private fun createCard(title: String, valueLabel: JLabel, accentColor: Color): JPanel {
        return object : JPanel(BorderLayout(0, 15)) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // 1. Card Background
                g2.color = cardBackground
                g2.fillRoundRect(0, 0, width, height, 20, 20)

                // 2. Colored Accent Bar (Top)
                g2.color = accentColor
                g2.fillRoundRect(20, 0, 60, 6, 4, 4) // Small pill at the top edge
            }
        }.apply {
            isOpaque = false
            border = EmptyBorder(30, 30, 30, 30) // Internal padding

            // Title
            val lblTitle = JLabel(title)
            lblTitle.font = Font("Segoe UI", Font.PLAIN, 14)
            lblTitle.foreground = textSecondary
            add(lblTitle, BorderLayout.NORTH)

            // Value (The big number)
            valueLabel.horizontalAlignment = SwingConstants.LEFT
            add(valueLabel, BorderLayout.CENTER)
        }
    }

    /**
     * Simple filler card with branding
     */
    private fun createBrandCard(): JPanel {
        return object : JPanel(GridBagLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = cardBackground
                g2.fillRoundRect(0, 0, width, height, 20, 20)
            }
        }.apply {
            isOpaque = false
            val lbl = JLabel("Event Planner Pro")
            lbl.font = Font("Segoe UI", Font.BOLD, 18)
            lbl.foreground = Color(100, 100, 100) // Dark text on dark card = subtle watermark
            add(lbl)
        }
    }

    private fun createStatLabel(): JLabel {
        return JLabel("...").apply {
            font = Font("Segoe UI", Font.BOLD, 36) // Big numbers
            foreground = textPrimary
        }
    }

    fun refreshStats() {
        val events = eventManager.getAllEvents()
        val participants = eventManager.getAllParticipants()
        val venues = eventManager.getAllVenues()

        totalEventsLabel.text = events.size.toString()
        totalParticipantsLabel.text = participants.size.toString()
        totalVenuesLabel.text = venues.size.toString()

        // Busiest Venue Logic
        if (events.isNotEmpty()) {
            val venuesMap = events.groupingBy { it.venue.name }.eachCount()
            val maxEntry = venuesMap.maxByOrNull { it.value }

            if (maxEntry != null) {
                busyVenueLabel.text = maxEntry.key
                // Dynamically resize font if name is long
                if (maxEntry.key.length > 12) {
                    busyVenueLabel.font = Font("Segoe UI", Font.BOLD, 22)
                } else {
                    busyVenueLabel.font = Font("Segoe UI", Font.BOLD, 36)
                }
            } else {
                busyVenueLabel.text = "N/A"
            }
        } else {
            busyVenueLabel.text = "N/A"
        }

        // Occupancy Logic
        if (events.isNotEmpty()) {
            val totalCap = events.sumOf { it.maxParticipants }
            val totalReg = events.sumOf { it.getCurrentCapacity() }
            val percent = if (totalCap > 0) (totalReg.toDouble() / totalCap * 100).toInt() else 0
            avgOccupancyLabel.text = "$percent%"

            // Color code the text based on health
            avgOccupancyLabel.foreground = when {
                percent > 80 -> accentGreen // Healthy
                percent < 20 -> accentOrange // Needs attention
                else -> textPrimary
            }
        } else {
            avgOccupancyLabel.text = "0%"
        }
    }
}