package com.eventplanning.service

import com.eventplanning.domain.Event
import com.eventplanning.domain.Venue
import java.time.LocalDateTime
import java.time.Duration

object ScalaBridge {

    private const val SLOT_FINDER_CLASS = "com.eventplanning.scheduling.SlotFinder"
    private const val EVENT_SCHEDULER_CLASS = "com.eventplanning.scheduling.EventScheduler"

    /**
     * Represents a specific time slot found for a venue.
     */
    data class VenueTimeSlot(
        val venue: Venue,
        val startTime: LocalDateTime,
        val freeUntil: LocalDateTime
    )

    sealed class SlotFinderResult {
        data class Success(val slots: List<VenueTimeSlot>) : SlotFinderResult()
        data class Error(val message: String) : SlotFinderResult()
    }

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
     * Finds available venues and their specific time slots.
     */
    fun findAvailableVenues(
        venues: List<Venue>,
        events: List<Event>,
        requiredCapacity: Int,
        dateTime: LocalDateTime,
        duration: Duration
    ): SlotFinderResult {
        return try {
            val clazz = Class.forName(SLOT_FINDER_CLASS)
            val method = clazz.getMethod(
                "findAllAvailableSlots",
                java.util.List::class.java,
                java.util.List::class.java,
                Int::class.javaPrimitiveType,
                LocalDateTime::class.java,
                Duration::class.java
            )

            @Suppress("UNCHECKED_CAST")
            val rawResult = method.invoke(null, venues, events, requiredCapacity, dateTime, duration)
                    as java.util.List<java.util.Map<String, Any>>

            // Parse the generic map from Scala into clean Kotlin objects
            val slots = rawResult.map { map ->
                VenueTimeSlot(
                    venue = map["venue"] as Venue,
                    startTime = map["start"] as LocalDateTime,
                    freeUntil = map["end"] as LocalDateTime
                )
            }

            SlotFinderResult.Success(slots)
        } catch (e: ClassNotFoundException) {
            SlotFinderResult.Error("Scala SlotFinder not found.")
        } catch (e: Exception) {
            val cause = e.cause?.message ?: e.message ?: "Unknown error"
            SlotFinderResult.Error("SlotFinder error: $cause")
        }
    }

    // ... (Scheduler methods remain unchanged) ...
    fun generateSchedule(events: List<Event>, venues: List<Venue>): SchedulerResult {
        return try {
            val schedulerClass = Class.forName(EVENT_SCHEDULER_CLASS)
            val resultClass = Class.forName("$EVENT_SCHEDULER_CLASS\$ScheduleResult")
            val scheduleMethod = schedulerClass.getMethod("scheduleEvents", java.util.List::class.java, java.util.List::class.java)
            val result = scheduleMethod.invoke(null, events, venues)
            val mapMethod = schedulerClass.getMethod("scheduleToMap", resultClass)
            @Suppress("UNCHECKED_CAST")
            val resultMap = mapMethod.invoke(null, result) as Map<String, Any>
            parseSchedulerResult(resultMap)
        } catch (e: Exception) {
            val cause = e.cause?.message ?: e.message ?: "Unknown error"
            SchedulerResult.Error("Scheduler error: $cause", 0)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSchedulerResult(resultMap: Map<String, Any>): SchedulerResult {
        val success = resultMap["success"] as? Boolean ?: false
        return if (success) {
            val scheduleList = resultMap["schedule"] as? java.util.ArrayList<java.util.Map<String, Any>>
                ?: return SchedulerResult.Error("Invalid schedule format", 0)
            val entries = scheduleList.map { entry ->
                ScheduleEntry(
                    eventId = entry["eventId"]?.toString() ?: "",
                    eventTitle = entry["eventTitle"]?.toString() ?: "",
                    venue = entry["venue"]?.toString() ?: "",
                    dateTime = entry["dateTime"]?.toString() ?: ""
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