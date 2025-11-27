package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistance.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities

fun main() {
    // 1. Initialize the Repository (Abstraction)
    val repository = DataStore()
    repository.connect()
    repository.initializeStorage()

    // 2. Inject Repository into EventManager
    val eventManager = EventManager(repository)

    // 3. Load data
    if (eventManager.initializeData()) {
        println("Data loaded successfully.")
    } else {
        println("Warning: Failed to load some data.")
    }

    // 4. Launch GUI
    // Note: We don't need to pass repository to UI anymore,
    // because EventManager handles persistence internally now!
    SwingUtilities.invokeLater {
        val mainWindow = MainWindow(eventManager)
        mainWindow.show()
    }

    // 5. Shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        eventManager.saveAllData()
        repository.disconnect()
    })
}