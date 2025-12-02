package com.eventplanning.scheduling

import com.eventplanning.domain.Venue
import com.eventplanning.domain.Event
import java.time.LocalDateTime
import java.time.Duration
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object SlotFinder {

  private def findAvailableSlot(
                         venues: java.util.List[Venue],
                         existingEvents: java.util.List[Event],
                         requiredCapacity: Int,
                         earliestStartDate: LocalDateTime,
                         duration: Duration
                       ): java.util.List[java.util.Map[String, Any]] = {

    if (requiredCapacity <= 0 || duration.isNegative || duration.isZero) {
      return new java.util.ArrayList[java.util.Map[String, Any]]()
    }

    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList
    val capableVenues = venueList.filter(_.getCapacity >= requiredCapacity)

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
        findNextFreeSlot(venue, allEvents, requestedStart, duration, 0) match {
          case Some(foundStart) =>
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
   * Recursively jumps forward until a valid slot is found or limit reached.
   */
  @tailrec
  private def findNextFreeSlot(
                                venue: Venue,
                                events: List[Event],
                                currentStart: LocalDateTime,
                                duration: Duration,
                                attempt: Int
                              ): Option[LocalDateTime] = {
    // FIXED: Removed 'return' keyword. 'None' is the expression value.
    if (attempt > 672) {
      None
    } else if (isVenueFree(venue, events, currentStart, duration)) {
      Some(currentStart)
    } else {
      findNextFreeSlot(venue, events, currentStart.plusMinutes(15), duration, attempt + 1)
    }
  }

  private def calculateFreeUntil(venue: Venue, events: List[Event], startTime: LocalDateTime): LocalDateTime = {
    val nextEvent = events
      .filter(e => e.getVenue.getId == venue.getId)
      .filter(e => e.getDateTime.isAfter(startTime))
      .sortBy(_.getDateTime)
      .headOption

    nextEvent match {
      case Some(event) => event.getDateTime
      case None => startTime.plusHours(24)
    }
  }

  private def isVenueFree(venue: Venue, events: List[Event], start: LocalDateTime, duration: Duration): Boolean = {
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