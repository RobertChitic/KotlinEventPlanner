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
        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Load Data")
        val exitItem = JMenuItem("Exit")

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
                    // Refresh all panels
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
            // Auto-save on exit
            dataStore.saveAll(eventManager)
            System.exit(0)
        }

        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        fileMenu.addSeparator()
        fileMenu.add(exitItem)
        menuBar.add(fileMenu)

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
}