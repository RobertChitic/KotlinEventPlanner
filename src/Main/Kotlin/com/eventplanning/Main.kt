package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistance.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    try {
        UIManager.put("Button.arc", 12)
        UIManager.put("Component.arc", 12)
        UIManager.put("ProgressBar.arc", 12)
        UIManager.put("TextComponent.arc", 12)
    } catch (e: Exception) {
        System.err.println("Failed to set UIManager properties: ${e.message}")
    }

    val repository = DataStore()
    repository.connect()
    repository.initializeStorage()

    val eventManager = EventManager(repository)

    if (eventManager.initializeData()) {
        println("Data loaded successfully.")
    } else {
        System.err.println("Warning: Failed to load some data.")
    }

    SwingUtilities.invokeLater {
        val mainWindow = MainWindow(eventManager)
        mainWindow.show()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down...")
        eventManager.saveAllData()
        repository.disconnect()
    })
}