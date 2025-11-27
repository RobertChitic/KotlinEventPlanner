package com.eventplanning.service

import com.eventplanning.domain.Event
import com.eventplanning.domain.Venue
import java.time.LocalDateTime

/**
 * A dedicated bridge to handle communication with Scala components.
 * This isolates the reflection logic, keeping the UI clean and decoupled.
 */
object ScalaBridge {

    // --- Slot Finder (Part E) ---
    fun findAvailableVenues(
        venues: List<Venue>,
        events: List<Event>,
        requiredCapacity: Int,
        dateTime: LocalDateTime,
        duration: java.time.Duration
    ): List<Venue> {
        try {
            val clazz = Class.forName("com.eventplanning.scheduling.SlotFinder")
            val method = clazz.getMethod(
                "findAllAvailableSlots",
                java.util.List::class.java,
                java.util.List::class.java,
                Int::class.javaPrimitiveType,
                java.time.LocalDateTime::class.java,
                java.time.Duration::class.java
            )

            @Suppress("UNCHECKED_CAST")
            return method.invoke(null, venues, events, requiredCapacity, dateTime) as List<Venue>
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to Scala SlotFinder: ${e.message}", e)
        }
    }

    // --- Event Scheduler (Part F) ---
    fun generateSchedule(events: List<Event>, venues: List<Venue>): Map<String, Any> {
        try {
            val schedulerClass = Class.forName("com.eventplanning.scheduling.EventScheduler")
            val resultClass = Class.forName("com.eventplanning.scheduling.EventScheduler\$ScheduleResult")

            // 1. Run the scheduleEvents method
            val scheduleMethod = schedulerClass.getMethod(
                "scheduleEvents",
                java.util.List::class.java,
                java.util.List::class.java
            )
            val result = scheduleMethod.invoke(null, events, venues)

            // 2. Convert the result to a friendly Map
            val mapMethod = schedulerClass.getMethod("scheduleToMap", resultClass)

            @Suppress("UNCHECKED_CAST")
            return mapMethod.invoke(null, result) as Map<String, Any>
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to Scala EventScheduler: ${e.message}", e)
        }
    }
}