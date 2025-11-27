package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge // NEW IMPORT
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

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
        val toolsMenu = JMenu("Tools")

        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Reload Data")
        val exitItem = JMenuItem("Exit")

        val scheduleItem = JMenuItem("Generate Schedule (Scala)")
        scheduleItem.addActionListener { generateSchedule() }
        toolsMenu.add(scheduleItem)

        saveItem.addActionListener {
            if (eventManager.saveAllData()) {
                JOptionPane.showMessageDialog(frame, "All data saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to save some data", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }

        loadItem.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(frame, "Reload data from Database?", "Confirm", JOptionPane.YES_NO_OPTION)
            if (confirm == JOptionPane.YES_OPTION) {
                if (eventManager.initializeData()) {
                    JOptionPane.showMessageDialog(frame, "Data reloaded!", "Success", JOptionPane.INFORMATION_MESSAGE)
                    refreshAllPanels()
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to reload data", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }

        exitItem.addActionListener {
            eventManager.saveAllData()
            System.exit(0)
        }

        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        fileMenu.addSeparator()
        fileMenu.add(exitItem)

        menuBar.add(fileMenu)
        menuBar.add(toolsMenu)
        frame.jMenuBar = menuBar
        frame.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun addTabs() {
        tabbedPane.addTab("Venues", VenuePanel(eventManager))
        tabbedPane.addTab("Events", EventPanel(eventManager))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager))
    }

    private fun refreshAllPanels() {
        tabbedPane.removeAll()
        addTabs()
    }

    // === UPDATED: Using ScalaBridge ===
    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()

        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No events to schedule.", "No Events", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        try {
            // Clean bridge call
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
                JOptionPane.showMessageDialog(frame, "Scheduling Failed:\n$msg", "Failed", JOptionPane.ERROR_MESSAGE)
            }

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(frame, "Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}