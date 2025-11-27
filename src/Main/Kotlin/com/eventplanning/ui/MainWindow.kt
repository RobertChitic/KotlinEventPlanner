package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

class MainWindow(
    private val eventManager: EventManager // Changed: Removed DataStore parameter
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
        val toolsMenu = JMenu("Tools")

        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Reload Data") // Renamed since "Load" usually implies startup
        val exitItem = JMenuItem("Exit")

        val scheduleItem = JMenuItem("📅 Generate Schedule (Scala)")
        scheduleItem.addActionListener { generateSchedule() }
        toolsMenu.add(scheduleItem)

        // Save menu action
        saveItem.addActionListener {
            // CHANGED: Call eventManager directly
            if (eventManager.saveAllData()) {
                JOptionPane.showMessageDialog(frame, "All data saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to save some data", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }

        // Load menu action
        loadItem.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(frame, "Reload data from Database?", "Confirm", JOptionPane.YES_NO_OPTION)
            if (confirm == JOptionPane.YES_OPTION) {
                // CHANGED: Call eventManager directly
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
        // CHANGED: Removed dataStore argument from these calls
        tabbedPane.addTab("Venues", VenuePanel(eventManager))
        tabbedPane.addTab("Events", EventPanel(eventManager))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager))
    }

    private fun refreshAllPanels() {
        tabbedPane.removeAll()
        addTabs()
    }

    // ... Keep generateSchedule() EXACTLY the same as your original code ...
    private fun generateSchedule() {
        // (Copy your existing Scala Reflection code here from the original file)
        // I am omitting it for brevity, but do NOT delete it from your file.
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()

        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No events to schedule.", "No Events", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        // ... rest of the reflection logic ...
        try {
            val schedulerClass = Class.forName("com.eventplanning.scheduling.EventScheduler")
            // ... etc ...
            val resultClass = Class.forName("com.eventplanning.scheduling.EventScheduler\$ScheduleResult")

            val scheduleMethod = schedulerClass.getMethod("scheduleEvents", java.util.List::class.java, java.util.List::class.java)
            val result = scheduleMethod.invoke(null, events, venues)

            val mapMethod = schedulerClass.getMethod("scheduleToMap", resultClass)
            @Suppress("UNCHECKED_CAST")
            val resultMap = mapMethod.invoke(null, result) as java.util.Map<String, Any>

            // ... (Keep your existing display logic) ...
            if (resultMap["success"] as Boolean) {
                JOptionPane.showMessageDialog(frame, "Schedule Generated!", "Success", JOptionPane.INFORMATION_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(frame, resultMap["message"], "Failed", JOptionPane.ERROR_MESSAGE)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(frame, "Error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}