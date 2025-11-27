package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatLaf

/**
 * Main application window containing all UI tabs.
 */
class MainWindow(private val eventManager: EventManager) {

    private val frame = JFrame("Event Planning Application")
    private val tabbedPane = JTabbedPane()

    fun show() {
        setupFrame()
        addTabs()
        frame.isVisible = true
    }

    private fun setupFrame() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(1100, 750)
        frame.setLocationRelativeTo(null) // Center on screen
        frame.layout = BorderLayout()

        val menuBar = createMenuBar()
        frame.jMenuBar = menuBar
        frame.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // File Menu
        val fileMenu = JMenu("File")
        val saveItem = JMenuItem("Save All").apply {
            addActionListener { saveAllData() }
        }
        val loadItem = JMenuItem("Reload Data").apply {
            addActionListener { reloadData() }
        }
        val exitItem = JMenuItem("Exit").apply {
            addActionListener { exitApplication() }
        }

        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        fileMenu.addSeparator()
        fileMenu.add(exitItem)

        // Tools Menu
        val toolsMenu = JMenu("Tools")
        val scheduleItem = JMenuItem("Generate Schedule (Scala)").apply {
            addActionListener { generateSchedule() }
        }

        val darkModeItem = JMenuItem("Toggle Dark Mode").apply {
            addActionListener { toggleDarkMode() }
        }

        toolsMenu.add(scheduleItem)
        toolsMenu.addSeparator()
        toolsMenu.add(darkModeItem)

        menuBar.add(fileMenu)
        menuBar.add(toolsMenu)

        return menuBar
    }

    private fun addTabs() {
        // Emojis removed to prevent text rendering issues
        tabbedPane.addTab("Venues", VenuePanel(eventManager))
        tabbedPane.addTab("Events", EventPanel(eventManager))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager))
    }

    private fun toggleDarkMode() {
        try {
            if (FlatLaf.isLafDark()) {
                UIManager.setLookAndFeel(FlatLightLaf())
            } else {
                UIManager.setLookAndFeel(FlatDarkLaf())
            }
            SwingUtilities.updateComponentTreeUI(frame)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                frame,
                "Failed to switch theme: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun refreshAllPanels() {
        tabbedPane.removeAll()
        addTabs()
    }

    private fun saveAllData() {
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.saveAllData()
            override fun done() {
                if (get()) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "All data saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Failed to save some data. Check console for details.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        worker.execute()
    }

    private fun reloadData() {
        val confirm = JOptionPane.showConfirmDialog(
            frame,
            "Reload all data from the database?\nAny unsaved changes will be lost.",
            "Confirm Reload",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.initializeData()
                override fun done() {
                    if (get()) {
                        refreshAllPanels()
                        JOptionPane.showMessageDialog(
                            frame,
                            "Data reloaded successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Failed to reload data. Check console for details.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            worker.execute()
        }
    }

    private fun exitApplication() {
        val confirm = JOptionPane.showConfirmDialog(
            frame,
            "Save all data before exiting?",
            "Exit Application",
            JOptionPane.YES_NO_CANCEL_OPTION
        )

        when (confirm) {
            JOptionPane.YES_OPTION -> {
                eventManager.saveAllData()
                System.exit(0)
            }
            JOptionPane.NO_OPTION -> System.exit(0)
            // CANCEL - do nothing
        }
    }

    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()

        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                "No events to schedule.",
                "No Events",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        // Show loading dialog
        val loadingDialog = JDialog(frame, "Generating Schedule...", true)
        loadingDialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        loadingDialog.add(JLabel("Please wait...", SwingConstants.CENTER))
        loadingDialog.setSize(200, 100)
        loadingDialog.setLocationRelativeTo(frame)

        val worker = object : SwingWorker<ScalaBridge.SchedulerResult, Void>() {
            override fun doInBackground(): ScalaBridge.SchedulerResult {
                return ScalaBridge.generateSchedule(events, venues)
            }

            override fun done() {
                loadingDialog.dispose()
                displayScheduleResult(get())
            }
        }

        worker.execute()
        loadingDialog.isVisible = true
    }

    private fun displayScheduleResult(result: ScalaBridge.SchedulerResult) {
        when (result) {
            is ScalaBridge.SchedulerResult.Success -> {
                val message = buildString {
                    appendLine("SCHEDULE GENERATED SUCCESSFULLY")
                    appendLine("========================================")
                    appendLine()
                    result.schedule.forEach { entry ->
                        appendLine("Event: ${entry.eventTitle}")
                        appendLine("   Venue: ${entry.venue}")
                        appendLine("   Time:  ${entry.dateTime}")
                        appendLine("----------------------------------------")
                    }
                }

                val textArea = JTextArea(message).apply {
                    isEditable = false
                    font = font.deriveFont(12f)
                }
                val scrollPane = JScrollPane(textArea).apply {
                    preferredSize = Dimension(450, 350)
                }

                JOptionPane.showMessageDialog(
                    frame,
                    scrollPane,
                    "Generated Schedule",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }

            is ScalaBridge.SchedulerResult.Error -> {
                JOptionPane.showMessageDialog(
                    frame,
                    "Scheduling Failed:\n\n${result.message}\n\nFailed events: ${result.failedCount}",
                    "Scheduling Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
}