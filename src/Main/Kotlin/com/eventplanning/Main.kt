package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistence.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities

// In your main application
fun main() {
    val dataStore = DataStore()
    dataStore.connectToDatabase()
    dataStore.createTables()

    val eventManager = EventManager()

    // Load existing data from database
    dataStore.loadAll(eventManager)

    val mainWindow = MainWindow(eventManager, dataStore)

    SwingUtilities.invokeLater {
        mainWindow.show()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        dataStore.saveAll(eventManager)
        dataStore.closeConnection()
    })
}