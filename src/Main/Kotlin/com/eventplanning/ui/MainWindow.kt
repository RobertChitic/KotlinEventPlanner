package com.eventplanning.ui

import com.eventplanning.domain.*
import com.eventplanning.persistence.DataStore
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

class MainWindow(
    private val eventManager: EventManager,
    private val dataStore: DataStore  // Add this parameter
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

        // Menu bar
        val menuBar = JMenuBar()
        val fileMenu = JMenu("File")

        // Add Tools menu for scheduling (Part F)
        val toolsMenu = JMenu("Tools")

        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Load Data")
        val exitItem = JMenuItem("Exit")

        // Add scheduler menu item (Part F - Scala)
        val scheduleItem = JMenuItem("📅 Generate Schedule (Scala)")
        scheduleItem.addActionListener { generateSchedule() }
        toolsMenu.add(scheduleItem)

        // Save menu action
        saveItem.addActionListener {
            if (dataStore.saveAll(eventManager)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "All data saved successfully!",
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    frame,
                    "Failed to save some data",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        // Load menu action
        loadItem.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                frame,
                "This will replace all current data. Continue?",
                "Confirm Load",
                JOptionPane.YES_NO_OPTION
            )

            if (confirm == JOptionPane.YES_OPTION) {
                if (dataStore.loadAll(eventManager)) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Data loaded successfully!",
                        "Load Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    refreshAllPanels()
                } else {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Failed to load data",
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }

        exitItem.addActionListener {
            dataStore.saveAll(eventManager)
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
        tabbedPane.addTab("Venues", VenuePanel(eventManager, dataStore))
        tabbedPane.addTab("Events", EventPanel(eventManager, dataStore))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager, dataStore))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager, dataStore))
    }

    private fun refreshAllPanels() {
        // Remove all tabs and re-add them to refresh
        tabbedPane.removeAll()
        addTabs()
    }

    /**
     * Part F - Event Scheduler using Scala
     * Generates a conflict-free schedule using functional Scala algorithm
     */
    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()

        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                "No events to schedule.\nPlease create some events first.",
                "No Events",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        if (venues.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                "No venues available.\nPlease add venues first.",
                "No Venues",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        try {
            // Use reflection to call EventScheduler methods to break circular dependency
            val eventSchedulerClass = Class.forName("com.eventplanning.scheduling.EventScheduler$")
            val eventSchedulerInstance = eventSchedulerClass.getField("MODULE$").get(null)
            
            // Call scheduleEvents method
            val scheduleEventsMethod = eventSchedulerClass.getMethod(
                "scheduleEvents",
                java.util.List::class.java,
                java.util.List::class.java
            )
            val result = scheduleEventsMethod.invoke(eventSchedulerInstance, events, venues)
            
            // Call scheduleToMap method
            val scheduleToMapMethod = eventSchedulerClass.getMethod(
                "scheduleToMap",
                Class.forName("com.eventplanning.scheduling.EventScheduler\$ScheduleResult")
            )
            @Suppress("UNCHECKED_CAST")
            val resultMap = scheduleToMapMethod.invoke(eventSchedulerInstance, result) as java.util.Map<String, Any>

            val success = resultMap["success"] as Boolean

            if (success) {
                @Suppress("UNCHECKED_CAST")
                val schedule = resultMap["schedule"] as java.util.ArrayList<java.util.Map<String, Any>>

                val message = buildString {
                    appendLine("✓ CONFLICT-FREE SCHEDULE GENERATED!")
                    appendLine("=".repeat(50))
                    appendLine()
                    appendLine("Successfully scheduled ${schedule.size} event(s):")
                    appendLine()

                    schedule.forEachIndexed { index, entry ->
                        appendLine("${index + 1}. ${entry["eventTitle"]}")
                        appendLine("   Venue: ${entry["venue"]}")
                        appendLine("   Time: ${entry["dateTime"]}")
                        appendLine()
                    }

                    appendLine("All events scheduled without conflicts!")
                }

                // Show in a scrollable text area dialog
                val textArea = JTextArea(message)
                textArea.isEditable = false
                textArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)

                val scrollPane = JScrollPane(textArea)
                scrollPane.preferredSize = Dimension(600, 400)

                JOptionPane.showMessageDialog(
                    frame,
                    scrollPane,
                    "Schedule Generated (Scala Algorithm)",
                    JOptionPane.INFORMATION_MESSAGE
                )

            } else {
                val errorMessage = resultMap["message"] as String
                val failedCount = resultMap["failedCount"] as Int

                JOptionPane.showMessageDialog(
                    frame,
                    "✗ SCHEDULING FAILED\n\n" +
                            "Error: $errorMessage\n\n" +
                            "Failed to schedule $failedCount event(s)\n\n" +
                            "Possible reasons:\n" +
                            "  • Insufficient venue capacity\n" +
                            "  • Time conflicts cannot be resolved\n" +
                            "  • Not enough venues available",
                    "Scheduling Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                frame,
                "Scheduling error: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            e.printStackTrace()
        }
    }
}