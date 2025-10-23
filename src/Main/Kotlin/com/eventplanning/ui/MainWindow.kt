package com.eventplanning.ui

import com.eventplanning.domain.*
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

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
        frame.size = Dimension(1000, 700)
        frame.layout = BorderLayout()

        // Menu bar
        val menuBar = JMenuBar()
        val fileMenu = JMenu("File")
        val saveItem = JMenuItem("Save All")
        val loadItem = JMenuItem("Load Data")
        val exitItem = JMenuItem("Exit")

        exitItem.addActionListener { System.exit(0) }

        fileMenu.add(saveItem)
        fileMenu.add(loadItem)
        fileMenu.addSeparator()
        fileMenu.add(exitItem)
        menuBar.add(fileMenu)

        frame.jMenuBar = menuBar
        frame.add(tabbedPane, BorderLayout.CENTER)
    }

    private fun addTabs() {
        tabbedPane.addTab("Venues", VenuePanel(eventManager))
        tabbedPane.addTab("Events", EventPanel(eventManager))
        tabbedPane.addTab("Participants", ParticipantPanel(eventManager))
        tabbedPane.addTab("Registration", RegistrationPanel(eventManager))
    }
}