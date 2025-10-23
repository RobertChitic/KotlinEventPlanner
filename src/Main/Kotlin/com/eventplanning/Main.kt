package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities

fun main() {
    val eventManager = EventManager()
    val mainWindow = MainWindow(eventManager)

    SwingUtilities.invokeLater {
        mainWindow.show()
    }
}