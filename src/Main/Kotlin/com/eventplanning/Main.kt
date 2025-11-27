package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistance.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager
import com.formdev.flatlaf.FlatLightLaf // Modern Theme

fun main() {
    try {
        FlatLightLaf.setup()
        // Optional: Add rounded corners to components for a softer look
        UIManager.put("Button.arc", 12)
        UIManager.put("Component.arc", 12)
        UIManager.put("ProgressBar.arc", 12)
        UIManager.put("TextComponent.arc", 12)
    } catch (e: Exception) {
        println("Failed to initialize FlatLaf theme. Falling back to default.")
    }

    // 2. Initialize the Repository (Abstraction)
    val repository = DataStore()
    repository.connect()
    repository.initializeStorage()

    // 3. Inject Repository into EventManager
    val eventManager = EventManager(repository)

    // 4. Load data
    if (eventManager.initializeData()) {
        println("Data loaded successfully.")
    } else {
        println("Warning: Failed to load some data.")
    }

    // 5. Launch GUI
    SwingUtilities.invokeLater {
        val mainWindow = MainWindow(eventManager)
        mainWindow.show()
    }

    // 6. Shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        eventManager.saveAllData()
        repository.disconnect()
    })
}