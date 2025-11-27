package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf

class MainWindow(
    private val eventManager: EventManager
) {
    private val frame = JFrame("Event Planning Application")
    private val tabbedPane = JTabbedPane()

    fun show() {
        setupFrame()
        addTabs()
        frame.isVisible = true
    }

    private fun setupFrame() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(1000, 700)
        frame.layout = BorderLayout()

        val menuBar = JMenuBar()
        val fileMenu = JMenu("File")
        val viewMenu = JMenu("View") // NEW
        val toolsMenu = JMenu("Tools")

        // --- File Menu ---
        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Reload Data")
        val exitItem = JMenuItem("Exit")

        // --- View Menu (Dark Mode) ---
        val darkModeItem = JCheckBoxMenuItem("🌙 Dark Mode")
        darkModeItem.addActionListener {
            if (darkModeItem.isSelected) {
                try { UIManager.setLookAndFeel(FlatDarkLaf()) } catch (e: Exception) {}
            } else {
                try { UIManager.setLookAndFeel(FlatLightLaf()) } catch (e: Exception) {}
            }
            FlatLaf.updateUI() // Magic command to refresh colors instantly
        }

        // --- Tools Menu ---
        val scheduleItem = JMenuItem("📅 Generate Schedule (Scala)")
        scheduleItem.addActionListener { generateSchedule() }

        // Listeners
        saveItem.addActionListener {
            if (eventManager.saveAllData()) {
                JOptionPane.showMessageDialog(frame, "Saved!", "Success", JOptionPane.INFORMATION_MESSAGE)
            }
        }

        loadItem.addActionListener {
            if (eventManager.initializeData()) {
                JOptionPane.showMessageDialog(frame, "Reloaded!", "Success", JOptionPane.INFORMATION_MESSAGE)
                refreshAllPanels()
            }
        }

        exitItem.addActionListener {
            eventManager.saveAllData()
            System.exit(0)
        }

        // Build Menu
        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        fileMenu.addSeparator()
        fileMenu.add(exitItem)

        viewMenu.add(darkModeItem)

        toolsMenu.add(scheduleItem)

        menuBar.add(fileMenu)
        menuBar.add(viewMenu) // Add View menu
        menuBar.add(toolsMenu)
        frame.jMenuBar = menuBar
        frame.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun addTabs() {
        tabbedPane.addTab("🏛 Venues", VenuePanel(eventManager))
        tabbedPane.addTab("📅 Events", EventPanel(eventManager))
        tabbedPane.addTab("👥 Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("📝 Registration", RegistrationPanel(eventManager))
    }

    private fun refreshAllPanels() {
        tabbedPane.removeAll()
        addTabs()
    }

    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()

        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No events to schedule.", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        try {
            val resultMap = ScalaBridge.generateSchedule(events, venues)

            if (resultMap["success"] as Boolean) {
                @Suppress("UNCHECKED_CAST")
                val schedule = resultMap["schedule"] as java.util.ArrayList<java.util.Map<String, Any>>

                val message = buildString {
                    appendLine("✓ SCHEDULE GENERATED")
                    appendLine("=".repeat(30))
                    schedule.forEach { entry ->
                        appendLine("Event: ${entry["eventTitle"]}")
                        appendLine("Venue: ${entry["venue"]}")
                        appendLine("Time:  ${entry["dateTime"]}")
                        appendLine("-".repeat(20))
                    }
                }
                val textArea = JTextArea(message)
                textArea.isEditable = false
                JOptionPane.showMessageDialog(frame, JScrollPane(textArea), "Schedule", JOptionPane.INFORMATION_MESSAGE)

            } else {
                val msg = resultMap["message"] as String
                JOptionPane.showMessageDialog(frame, "Scheduling Failed:\n$msg", "Error", JOptionPane.ERROR_MESSAGE)
            }

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(frame, "Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}