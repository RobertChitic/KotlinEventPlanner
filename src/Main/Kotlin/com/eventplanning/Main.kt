package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistance.DataStore
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager
import java.awt.Font
import com.formdev.flatlaf.FlatLightLaf

fun main() {
    try {
        FlatLightLaf.setup()

        // --- FIX FOR MISSING TEXT ON MACOS ---
        // Force a standard font instead of the system font
        val safeFont = javax.swing.plaf.FontUIResource("SansSerif", Font.PLAIN, 13)
        UIManager.put("defaultFont", safeFont)
        // -------------------------------------

        UIManager.put("Button.arc", 12)
        UIManager.put("Component.arc", 12)
        UIManager.put("ProgressBar.arc", 12)
        UIManager.put("TextComponent.arc", 12)
    } catch (e: Exception) {
        println("Failed to initialize FlatLaf theme.")
    }

    val repository = DataStore()
    repository.connect()
    repository.initializeStorage()

    val eventManager = EventManager(repository)

    if (eventManager.initializeData()) {
        println("Data loaded successfully.")
    } else {
        println("Warning: Failed to load some data.")
    }

    SwingUtilities.invokeLater {
        val mainWindow = MainWindow(eventManager)
        mainWindow.show()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        eventManager.saveAllData()
        repository.disconnect()
    })
}