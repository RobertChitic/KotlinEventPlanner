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

    if (eventList.isEmpty) return Success(List.empty)
    if (venueList.isEmpty) return Failure("No venues available to schedule events.", eventList)

    // OPTIMIZATION: Sort events by duration (longest first) to solve hardest problems first
    // This reduces the chance of getting stuck with a big event and no slots left.
    val sortedEvents = eventList.sortBy(_.getDuration).reverse

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
        Success(scheduledSoFar)

      case event :: restEvents =>
        // Try to find a venue
        findVenueForEvent(event, availableVenues, scheduledSoFar) match {
          case Some(venue) =>
            val scheduled = ScheduledEvent(event, venue, event.getDateTime)
            scheduleEventsRecursive(restEvents, availableVenues, scheduledSoFar :+ scheduled)

          case None =>
            // Detailed error message for the user
            Failure(
              s"Conflict: '${event.getTitle}' (Needs Cap: ${event.getMaxParticipants}) could not be booked.\n" +
                "Check if venues are fully booked at ${event.getDateTime} or if capacity is too low.",
              event :: restEvents
            )
        }
    }
  }

  private def findVenueForEvent(
                                 event: Event,
                                 venues: List[Venue],
                                 existingSchedule: List[ScheduledEvent]
                               ): Option[Venue] = {

    venues
      .filter(_.getCapacity >= event.getMaxParticipants) // 1. Filter by Capacity
      .filter(venue => !hasConflict(event, venue, existingSchedule)) // 2. Filter by Schedule Conflict
      .sortBy(_.getCapacity) // 3. Best Fit Strategy: Use the smallest venue that fits the crowd
      .headOption // Take the best match
  }

  private def hasConflict(
                           proposedEvent: Event,
                           proposedVenue: Venue,
                           existingSchedule: List[ScheduledEvent]
                         ): Boolean = {

    existingSchedule.exists { scheduled =>
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
    // (StartA < EndB) and (EndA > StartB)
    start1.isBefore(end2) && end1.isAfter(start2)
  }

  def scheduleToMap(result: ScheduleResult): java.util.Map[String, Object] = {
    val map = new java.util.HashMap[String, Object]()

    result match {
      case Success(schedule) =>
        map.put("success", java.lang.Boolean.TRUE)
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