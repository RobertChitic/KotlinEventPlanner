package com.eventplanning.scheduling

import com.eventplanning.domain.Venue
import com.eventplanning.domain.Event
import java.time.LocalDateTime
import java.time.Duration
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object SlotFinder {

  /**
   * Finds available venues searching forward from start time.
   * Returns a List of Maps containing: "venue", "start" (LocalDateTime), "end" (LocalDateTime).
   * The "end" represents the "Free Until" time.
   */
  def findAvailableSlot(
                         venues: java.util.List[Venue],
                         existingEvents: java.util.List[Event],
                         requiredCapacity: Int,
                         earliestStartDate: LocalDateTime,
                         duration: Duration
                       ): java.util.List[java.util.Map[String, Any]] = {

    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    // Filter by Capacity
    val capableVenues = venueList.filter(_.getCapacity >= requiredCapacity)

    // Find slots recursively
    val validSlots = findSlotsForVenues(capableVenues, eventList, earliestStartDate, duration, Nil)

    // Sort: Earliest start time first
    val sortedSlots = validSlots.sortWith((a, b) => {
      val startA = a.get("start").asInstanceOf[LocalDateTime]
      val startB = b.get("start").asInstanceOf[LocalDateTime]
      startA.isBefore(startB)
    })

    sortedSlots.asJava
  }

  // Method alias for compatibility
  def findAllAvailableSlots(
                             venues: java.util.List[Venue],
                             existingEvents: java.util.List[Event],
                             requiredCapacity: Int,
                             earliestStartDate: LocalDateTime,
                             duration: Duration
                           ): java.util.List[java.util.Map[String, Any]] = {
    findAvailableSlot(venues, existingEvents, requiredCapacity, earliestStartDate, duration)
  }

  @tailrec
  private def findSlotsForVenues(
                                  remainingVenues: List[Venue],
                                  allEvents: List[Event],
                                  requestedStart: LocalDateTime,
                                  duration: Duration,
                                  acc: List[java.util.Map[String, Any]]
                                ): List[java.util.Map[String, Any]] = {
    remainingVenues match {
      case Nil => acc
      case venue :: tail =>
        // Recursive search for this specific venue
        findNextFreeSlot(venue, allEvents, requestedStart, duration, 0) match {
          case Some(foundStart) =>
            // Found a slot! Now calculate "Free Until"
            val freeUntil = calculateFreeUntil(venue, allEvents, foundStart)

            val map = new java.util.HashMap[String, Any]()
            map.put("venue", venue)
            map.put("start", foundStart)
            map.put("end", freeUntil)

            findSlotsForVenues(tail, allEvents, requestedStart, duration, map :: acc)

          case None =>
            findSlotsForVenues(tail, allEvents, requestedStart, duration, acc)
        }
    }
  }

  /**
   * Recursively jumps forward 30 minutes until a valid slot is found or limit reached.
   */
  @tailrec
  private def findNextFreeSlot(
                                venue: Venue,
                                events: List[Event],
                                currentStart: LocalDateTime,
                                duration: Duration,
                                attempt: Int
                              ): Option[LocalDateTime] = {
    // Cap search at 48 attempts (24 hours)
    if (attempt > 48) return None

    if (isVenueFree(venue, events, currentStart, duration)) {
      Some(currentStart)
    } else {
      // Recursive Step: Try 30 minutes later
      findNextFreeSlot(venue, events, currentStart.plusMinutes(30), duration, attempt + 1)
    }
  }

  /**
   * Calculates when the next event starts to determine "Free Until".
   * Caps at 24 hours from the start time.
   */
  private def calculateFreeUntil(venue: Venue, events: List[Event], startTime: LocalDateTime): LocalDateTime = {
    val nextEvent = events
      .filter(e => e.getVenue.getId == venue.getId) // Events at this venue
      .filter(e => e.getDateTime.isAfter(startTime)) // Starting after our slot
      .sortBy(_.getDateTime) // Earliest first
      .headOption // Get the first one

    nextEvent match {
      case Some(event) => event.getDateTime // Free until that event starts
      case None => startTime.plusHours(24) // No future events? Cap at 24 hours.
    }
  }

  private def isVenueFree(
                           venue: Venue,
                           events: List[Event],
                           start: LocalDateTime,
                           duration: Duration
                         ): Boolean = {
    val end = start.plus(duration)
    !events.exists { event =>
      event.getVenue.getId == venue.getId &&
        eventsOverlap(event.getDateTime, event.getEndTime, start, end)
    }
  }

  private def eventsOverlap(start1: LocalDateTime, end1: LocalDateTime, start2: LocalDateTime, end2: LocalDateTime): Boolean = {
    start1.isBefore(end2) && end1.isAfter(start2)
  }
}