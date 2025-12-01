package com.eventplanning.scheduling

import com.eventplanning.domain.Event
import com.eventplanning.domain.Venue
import java.time.LocalDateTime
import java.time.LocalTime
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

  // Configurable constraints for the auto-scheduler
  val WORK_DAY_START = LocalTime.of(8, 0)
  val WORK_DAY_END = LocalTime.of(20, 0)

  def scheduleEvents(
                      events: java.util.List[Event],
                      venues: java.util.List[Venue]
                    ): ScheduleResult = {

    // 1. Input Validation
    if (events == null || venues == null) return Failure("Input lists cannot be null", List.empty)

    val eventList = events.asScala.toList
    val venueList = venues.asScala.toList

    if (eventList.isEmpty) return Success(List.empty)
    if (venueList.isEmpty) return Failure("No venues available to schedule events.", eventList)

    // 2. Heuristic Sort: Schedule longest events first (Bin Packing heuristic)
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
        // Attempt to schedule this event.
        // It tries the preferred time first, then searches forward if needed.
        findBestSlotForEvent(event, availableVenues, scheduledSoFar) match {
          case Some(scheduledEvent) =>
            scheduleEventsRecursive(restEvents, availableVenues, scheduledSoFar :+ scheduledEvent)

          case None =>
            Failure(
              s"Conflict: '${event.getTitle}' (Needs Cap: ${event.getMaxParticipants}) could not be booked.\n" +
                s"Could not find any free slot starting from ${event.getDateTime} within limits.",
              event :: restEvents
            )
        }
    }
  }

  /**
   * Tries to find a venue for the event.
   * If the event's current time is blocked, it hunts for a new time slot.
   */
  private def findBestSlotForEvent(
                                    event: Event,
                                    venues: List[Venue],
                                    existingSchedule: List[ScheduledEvent]
                                  ): Option[ScheduledEvent] = {

    // Filter venues capable of holding the event
    val capableVenues = venues
      .filter(_.getCapacity >= event.getMaxParticipants)
      .sortBy(_.getCapacity) // Use smallest capable venue first (efficiency)

    // Try to find a slot in any capable venue
    findSlotInVenues(event, capableVenues, existingSchedule)
  }

  /**
   * Iterates through capable venues to find a time slot.
   */
  @tailrec
  private def findSlotInVenues(
                                event: Event,
                                venues: List[Venue],
                                schedule: List[ScheduledEvent]
                              ): Option[ScheduledEvent] = {
    venues match {
      case Nil => None // No venues work
      case venue :: tail =>
        // For this venue, try to find a time slot starting at event.dateTime
        findNextTimeSlot(event, venue, event.getDateTime, schedule, 0) match {
          case Some(time) => Some(ScheduledEvent(event, venue, time))
          case None => findSlotInVenues(event, tail, schedule) // Try next venue
        }
    }
  }

  /**
   * Recursively searches for a time slot within a specific venue.
   * Starts at `candidateTime` and moves forward in 30-minute increments if blocked.
   * NOTE: Uses if-else chain to satisfy @tailrec requirements (no explicit return statements).
   */
  @tailrec
  private def findNextTimeSlot(
                                event: Event,
                                venue: Venue,
                                candidateTime: LocalDateTime,
                                schedule: List[ScheduledEvent],
                                attempts: Int
                              ): Option[LocalDateTime] = {

    if (attempts > 336) {
      // Limit reached (e.g., 7 days of searching)
      None
    } else if (candidateTime.toLocalTime.isAfter(WORK_DAY_END.minus(event.getDuration))) {
      // Case 1: Outside working hours -> Jump to 8 AM next day
      val nextMorning = candidateTime.plusDays(1).withHour(WORK_DAY_START.getHour).withMinute(0)
      findNextTimeSlot(event, venue, nextMorning, schedule, attempts + 1)
    } else if (!hasConflict(candidateTime, event.getDuration, venue, schedule)) {
      // Case 2: Slot is free -> Return it
      Some(candidateTime)
    } else {
      // Case 3: Conflict found -> Move forward 30 minutes
      findNextTimeSlot(event, venue, candidateTime.plusMinutes(30), schedule, attempts + 1)
    }
  }

  /**
   * Checks if a specific time slot at a specific venue overlaps with the existing schedule.
   */
  private def hasConflict(
                           start: LocalDateTime,
                           duration: java.time.Duration,
                           venue: Venue,
                           existingSchedule: List[ScheduledEvent]
                         ): Boolean = {
    val end = start.plus(duration)

    existingSchedule.exists { scheduled =>
      scheduled.assignedVenue.getId == venue.getId &&
        timeOverlap(start, end, scheduled.assignedDateTime, scheduled.assignedDateTime.plus(scheduled.event.getDuration))
    }
  }

  private def timeOverlap(
                           start1: LocalDateTime,
                           end1: LocalDateTime,
                           start2: LocalDateTime,
                           end2: LocalDateTime
                         ): Boolean = {
    start1.isBefore(end2) && end1.isAfter(start2)
  }

  // Helper for Java Interop (Reflection Bridge)
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