package com.eventplanning

import com.eventplanning.domain.EventManager
import com.eventplanning.persistence.DataStore
import com.eventplanning.scheduling.SlotFinder
import com.eventplanning.ui.MainWindow
import javax.swing.SwingUtilities
import java.time.LocalDateTime
import java.time.Duration
import scala.jdk.CollectionConverters

fun main() {
    val dataStore = DataStore()
    dataStore.connectToDatabase()
    dataStore.createTables()

    val eventManager = EventManager()
    dataStore.loadAll(eventManager)

    val mainWindow = MainWindow(eventManager, dataStore)

    SwingUtilities.invokeLater {
        mainWindow.show()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        dataStore.saveAll(eventManager)
        dataStore.closeConnection()
    })

    val slotFinder =  SlotFinder()

        val venues = CollectionConverters.asScala(eventManager.getAllVenues()).toList()
    val events = CollectionConverters.asScala(eventManager.getAllEvents()).toList()

    val result = slotFinder.findSlot(
        venues,
        events,
        40,
        LocalDateTime.of(2021, 12, 12, 0, 0),
        Duration.ofHours(2)
    )

    println(result)
}