package com.eventplanning. service

import com. eventplanning.domain.Event
import com.eventplanning. domain.Venue
import java.time.LocalDateTime
import java.time.Duration

/**
 * Bridge object for communication between Kotlin and Scala components.
 * Uses reflection to invoke Scala code, keeping the UI layer decoupled from Scala dependencies.
 */
object ScalaBridge {

    private const val SLOT_FINDER_CLASS = "com.eventplanning.scheduling.SlotFinder"
    private const val EVENT_SCHEDULER_CLASS = "com.eventplanning. scheduling.EventScheduler"

    /**
     * Result class for slot finding operations.
     */
    sealed class SlotFinderResult {
        data class Success(val venues: List<Venue>) : SlotFinderResult()
        data class Error(val message: String) : SlotFinderResult()
    }

    /**
     * Result class for scheduling operations.
     */
    sealed class SchedulerResult {
        data class Success(val schedule: List<ScheduleEntry>) : SchedulerResult()
        data class Error(val message: String, val failedCount: Int) : SchedulerResult()
    }

    data class ScheduleEntry(
        val eventId: String,
        val eventTitle: String,
        val venue: String,
        val dateTime: String
    )

    /**
     * Finds all available venues for a proposed event.
     * Invokes the Scala SlotFinder algorithm.
     */
    fun findAvailableVenues(
        venues: List<Venue>,
        events: List<Event>,
        requiredCapacity: Int,
        dateTime: LocalDateTime,
        duration: Duration
    ): SlotFinderResult {
        return try {
            val clazz = Class. forName(SLOT_FINDER_CLASS)
            val method = clazz. getMethod(
                "findAllAvailableSlots",
                java.util.List::class.java,
                java.util. List::class.java,
                Int::class.javaPrimitiveType,
                LocalDateTime::class.java,
                Duration::class.java
            )

            @Suppress("UNCHECKED_CAST")
            val result = method.invoke(null, venues, events, requiredCapacity, dateTime, duration) as List<Venue>
            SlotFinderResult. Success(result)
        } catch (e: ClassNotFoundException) {
            SlotFinderResult.Error("Scala SlotFinder not found.  Ensure Scala components are compiled.")
        } catch (e: Exception) {
            val cause = e.cause?. message ?: e.message ?: "Unknown error"
            SlotFinderResult.Error("SlotFinder error: $cause")
        }
    }

    /**
     * Generates a conflict-free schedule for the given events.
     * Invokes the Scala EventScheduler algorithm.
     */
    fun generateSchedule(events: List<Event>, venues: List<Venue>): SchedulerResult {
        return try {
            val schedulerClass = Class. forName(EVENT_SCHEDULER_CLASS)
            val resultClass = Class.forName("$EVENT_SCHEDULER_CLASS\$ScheduleResult")

            // Invoke scheduleEvents method
            val scheduleMethod = schedulerClass.getMethod(
                "scheduleEvents",
                java.util.List::class.java,
                java.util. List::class.java
            )
            val result = scheduleMethod. invoke(null, events, venues)

            // Convert result to a map for easier processing
            val mapMethod = schedulerClass.getMethod("scheduleToMap", resultClass)

            @Suppress("UNCHECKED_CAST")
            val resultMap = mapMethod. invoke(null, result) as Map<String, Any>

            parseSchedulerResult(resultMap)
        } catch (e: ClassNotFoundException) {
            SchedulerResult.Error("Scala EventScheduler not found. Ensure Scala components are compiled.", 0)
        } catch (e: Exception) {
            val cause = e.cause?.message ?: e.message ?: "Unknown error"
            SchedulerResult. Error("Scheduler error: $cause", 0)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSchedulerResult(resultMap: Map<String, Any>): SchedulerResult {
        val success = resultMap["success"] as?  Boolean ?: false

        return if (success) {
            val scheduleList = resultMap["schedule"] as? java.util.ArrayList<java.util.Map<String, Any>>
                ?: return SchedulerResult. Error("Invalid schedule format", 0)

            val entries = scheduleList. map { entry ->
                ScheduleEntry(
                    eventId = entry["eventId"]?.toString() ?: "",
                    eventTitle = entry["eventTitle"]?.toString() ?: "",
                    venue = entry["venue"]?.toString() ?: "",
                    dateTime = entry["dateTime"]?. toString() ?: ""
                )
            }
            SchedulerResult.Success(entries)
        } else {
            val message = resultMap["message"] as? String ?: "Unknown scheduling error"
            val failedCount = (resultMap["failedCount"] as? Int) ?: 0
            SchedulerResult.Error(message, failedCount)
        }
    }
}