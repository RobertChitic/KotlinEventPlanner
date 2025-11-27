package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistance.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager
import com.formdev.flatlaf.FlatLightLaf

fun main() {
    // Initialize Look and Feel
    try {
        FlatLightLaf. setup()
        UIManager.put("Button.arc", 12)
        UIManager.put("Component.arc", 12)
        UIManager.put("ProgressBar.arc", 12)
        UIManager.put("TextComponent.arc", 12)
    } catch (e: Exception) {
        System.err.println("Failed to initialize FlatLaf theme: ${e.message}")
    }

    // Initialize database
    val repository = DataStore()
    repository.connect()
    repository. initializeStorage()

    // Initialize event manager
    val eventManager = EventManager(repository)

    if (eventManager.initializeData()) {
        println("Data loaded successfully.")
    } else {
        System.err.println("Warning: Failed to load some data.")
    }

    // Launch UI
    SwingUtilities.invokeLater {
        val mainWindow = MainWindow(eventManager)
        mainWindow.show()
    }

    // Graceful shutdown
    Runtime.getRuntime(). addShutdownHook(Thread {
        println("Shutting down...")
        eventManager. saveAllData()
        repository. disconnect()
    })
}