package com.eventplanning.scheduling

import _root_.com.eventplanning.domain.Event
import _root_.com.eventplanning.domain.Venue
import java.time.LocalDateTime
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object EventScheduler {

  case class ScheduledEvent(
                             event: Event,
                             assignedVenue: Venue,
                             assignedDateTime: LocalDateTime
                           )

  sealed trait ScheduleResult
  case class Success(schedule: List[ScheduledEvent]) extends ScheduleResult
  case class Failure(message: String, problematicEvents: List[Event]) extends ScheduleResult


  def scheduleEvents(
                      events: java.util.List[Event],
                      venues: java.util.List[Venue]
                    ): ScheduleResult = {

    val eventList = events.asScala.toList
    val venueList = venues.asScala.toList

    if (eventList.isEmpty) {
      return Success(List.empty)
    }

    if (venueList.isEmpty) {
      return Failure("No venues available", eventList)
    }

    // Sort events by date/time (schedule earlier events first)
    val sortedEvents = eventList.sortBy(_.getDateTime)

    // Attempt to schedule all events
    scheduleEventsRecursive(sortedEvents, venueList, List.empty)
  }

  @tailrec
  private def scheduleEventsRecursive(
                                       remainingEvents: List[Event],
                                       availableVenues: List[Venue],
                                       scheduledSoFar: List[ScheduledEvent]
                                     ): ScheduleResult = {

    remainingEvents match {
      case Nil =>
        // All events scheduled successfully
        Success(scheduledSoFar)

      case event :: restEvents =>
        // Try to find a venue for this event
        findVenueForEvent(event, availableVenues, scheduledSoFar) match {
          case Some(venue) =>
            // Successfully assigned venue, continue with remaining events
            val scheduled = ScheduledEvent(event, venue, event.getDateTime)
            scheduleEventsRecursive(restEvents, availableVenues, scheduledSoFar :+ scheduled)

          case None =>
            // Could not find venue for this event
            Failure(
              s"Could not schedule event: ${event.getTitle}",
              event :: restEvents
            )
        }
    }
  }

  /**
   * Finds a suitable venue for a single event that doesn't conflict with existing schedule.
   */
  private def findVenueForEvent(
                                 event: Event,
                                 venues: List[Venue],
                                 existingSchedule: List[ScheduledEvent]
                               ): Option[Venue] = {

    venues
      .filter(_.getCapacity >= event.getMaxParticipants) // Capacity check
      .find(venue => !hasConflict(event, venue, existingSchedule)) // Conflict check
  }

  private def hasConflict(
                           proposedEvent: Event,
                           proposedVenue: Venue,
                           existingSchedule: List[ScheduledEvent]
                         ): Boolean = {

    existingSchedule.exists { scheduled =>
      // Conflict if same venue and overlapping time
      scheduled.assignedVenue.getId == proposedVenue.getId &&
        timeOverlap(
          proposedEvent.getDateTime,
          proposedEvent.getEndTime,
          scheduled.event.getDateTime,
          scheduled.event.getEndTime
        )
    }
  }

  private def timeOverlap(
                           start1: LocalDateTime,
                           end1: LocalDateTime,
                           start2: LocalDateTime,
                           end2: LocalDateTime
                         ): Boolean = {
    // Two ranges overlap if one starts before the other ends
    start1.isBefore(end2) && end1.isAfter(start2)
  }

  def scheduleToMap(result: ScheduleResult): java.util.Map[String, Object] = {
    import scala.collection.mutable

    val map = new java.util.HashMap[String, Object]()

    result match {
      case Success(schedule) =>
        map.put("success", java.lang.Boolean.TRUE)

        // Convert schedule to Java format
        val scheduleList = new java.util.ArrayList[java.util.Map[String, Object]]()
        schedule.foreach { scheduled =>
          val eventMap = new java.util.HashMap[String, Object]()
          eventMap.put("eventId", scheduled.event.getId)
          eventMap.put("eventTitle", scheduled.event.getTitle)
          eventMap.put("venue", scheduled.assignedVenue.getName)
          eventMap.put("dateTime", scheduled.assignedDateTime.toString)
          scheduleList.add(eventMap)
        }
        map.put("schedule", scheduleList)

      case Failure(message, problematicEvents) =>
        map.put("success", java.lang.Boolean.FALSE)
        map.put("message", message)
        map.put("failedCount", Integer.valueOf(problematicEvents.size))
    }

    map
  }
}