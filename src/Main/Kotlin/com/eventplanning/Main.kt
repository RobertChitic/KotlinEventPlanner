package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistence.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities


fun main() {
    // Initialize database
    val dataStore = DataStore()
    dataStore.connectToDatabase()
    dataStore.createTables()

    // Initialize EventManager and load existing data
    val eventManager = EventManager()
    dataStore.loadAll(eventManager)

    // Create and show GUI
    val mainWindow = MainWindow(eventManager, dataStore)

    SwingUtilities.invokeLater {
        mainWindow.show()
    }

    // Save data when application closes
    Runtime.getRuntime().addShutdownHook(Thread {
        dataStore.saveAll(eventManager)
        dataStore.closeConnection()
    })
}