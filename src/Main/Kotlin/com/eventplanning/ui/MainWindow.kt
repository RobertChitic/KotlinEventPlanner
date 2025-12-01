package com.eventplanning.ui

import com.eventplanning.domain.EventManager
import com.eventplanning.service.ScalaBridge
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf

class MainWindow(private val eventManager: EventManager) {

    private val frame = JFrame("Event Planning Application")
    private val tabbedPane = JTabbedPane()
    private val statsPanel = StatisticsPanel(eventManager)

    fun show() {
        setupFrame()
        addTabs()
        frame.isVisible = true
    }

    private fun setupFrame() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(1100, 750)
        frame.setLocationRelativeTo(null)
        frame.layout = BorderLayout()

        val menuBar = createMenuBar()
        frame.jMenuBar = menuBar
        frame.add(tabbedPane, BorderLayout.CENTER)

        tabbedPane.addChangeListener {
            if (tabbedPane.selectedComponent == statsPanel) {
                statsPanel.refreshStats()
            }
        }
    }

    private fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // File
        val fileMenu = JMenu("File")
        val saveItem = JMenuItem("Save All").apply { addActionListener { saveAllData() } }
        val loadItem = JMenuItem("Reload Data").apply { addActionListener { reloadData() } }
        val exitItem = JMenuItem("Exit").apply { addActionListener { exitApplication() } }

        fileMenu.add(saveItem); fileMenu.add(loadItem); fileMenu.addSeparator(); fileMenu.add(exitItem)

        // Tools
        val toolsMenu = JMenu("Tools")
        val scheduleItem = JMenuItem("Generate Schedule (Scala)").apply { addActionListener { generateSchedule() } }

        // --- DARK MODE (Back in Tools Menu) ---
        val darkModeItem = JMenuItem("Toggle Dark Mode")
        darkModeItem.addActionListener {
            SwingUtilities.invokeLater {
                try {
                    if (FlatLaf.isLafDark()) {
                        UIManager.setLookAndFeel(FlatLightLaf())
                    } else {
                        UIManager.setLookAndFeel(FlatDarkLaf())
                    }
                    FlatLaf.updateUI()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        toolsMenu.add(scheduleItem)
        toolsMenu.addSeparator()
        toolsMenu.add(darkModeItem)

        menuBar.add(fileMenu)
        menuBar.add(toolsMenu)
        return menuBar
    }

    private fun addTabs() {
        tabbedPane.addTab("Dashboard", statsPanel)
        tabbedPane.addTab("Venues", VenuePanel(eventManager))
        tabbedPane.addTab("Events", EventPanel(eventManager))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager))
    }

    private fun refreshAllPanels() {
        tabbedPane.removeAll()
        addTabs()
        statsPanel.refreshStats()
    }

    private fun saveAllData() {
        val worker = object : SwingWorker<Boolean, Void>() {
            override fun doInBackground(): Boolean = eventManager.saveAllData()
            override fun done() {
                if (get()) JOptionPane.showMessageDialog(frame, "Saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                else JOptionPane.showMessageDialog(frame, "Save failed.", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
        worker.execute()
    }

    private fun reloadData() {
        if (JOptionPane.showConfirmDialog(frame, "Reload data? Unsaved changes will be lost.", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            val worker = object : SwingWorker<Boolean, Void>() {
                override fun doInBackground(): Boolean = eventManager.initializeData()
                override fun done() {
                    if (get()) {
                        refreshAllPanels()
                        JOptionPane.showMessageDialog(frame, "Reloaded!", "Success", JOptionPane.INFORMATION_MESSAGE)
                    } else JOptionPane.showMessageDialog(frame, "Reload failed.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            worker.execute()
        }
    }

    private fun exitApplication() {
        if (JOptionPane.showConfirmDialog(frame, "Save before exiting?", "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            eventManager.saveAllData()
        }
        System.exit(0)
    }

    private fun generateSchedule() {
        val events = eventManager.getAllEvents()
        val venues = eventManager.getAllVenues()
        if (events.isEmpty()) { JOptionPane.showMessageDialog(frame, "No events.", "Info", JOptionPane.INFORMATION_MESSAGE); return }

        val worker = object : SwingWorker<ScalaBridge.SchedulerResult, Void>() {
            override fun doInBackground() = ScalaBridge.generateSchedule(events, venues)
            override fun done() { displayScheduleResult(get()) }
        }
        worker.execute()
    }

    private fun displayScheduleResult(result: ScalaBridge.SchedulerResult) {
        when (result) {
            is ScalaBridge.SchedulerResult.Success -> {
                val message = buildString {
                    appendLine("SCHEDULE GENERATED")
                    appendLine("=".repeat(30))
                    result.schedule.forEach { e -> appendLine("Event: ${e.eventTitle}\n   Venue: ${e.venue}\n   Time:  ${e.dateTime}\n${"-".repeat(20)}") }
                }
                val textArea = JTextArea(message).apply { isEditable = false }
                JOptionPane.showMessageDialog(frame, JScrollPane(textArea).apply { preferredSize = Dimension(400, 300) }, "Schedule", JOptionPane.INFORMATION_MESSAGE)
            }
            is ScalaBridge.SchedulerResult.Error -> JOptionPane.showMessageDialog(frame, result.message, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
}