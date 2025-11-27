package com.eventplanning.scheduling

import com.eventplanning.domain.Venue
import com.eventplanning.domain.Event
import java.time.LocalDateTime
import java.time.Duration
import scala.jdk.CollectionConverters._

object SlotFinder {

  def findAvailableSlot(
                         venues: java.util.List[Venue],
                         existingEvents: java.util.List[Event],
                         requiredCapacity: Int,
                         earliestStartDate: LocalDateTime,
                         duration: Duration
                       ): Option[Venue] = {
    // Convert Java collections to Scala collections for functional operations
    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    // Functional pipeline: filter by capacity, then find first available
    venueList
      .filter(venue => venue.getCapacity >= requiredCapacity)
      .sortBy(_.getCapacity) // Prioritize smaller venues that meet requirements
      .find(venue => isVenueAvailableAtTime(venue, eventList, earliestStartDate, duration))
  }

  /**
   * Checks if a venue is available at a specific date/time range.
   * A venue is available if no existing event is scheduled at that venue during the proposed duration.
   */
  private def isVenueAvailableAtTime(
                                      venue: Venue,
                                      events: List[Event],
                                      proposedStart: LocalDateTime,
                                      duration: Duration
                                    ): Boolean = {

    val proposedEnd = proposedStart.plus(duration)

    // Functional approach: check if NO events conflict
    !events.exists(event =>
      event.getVenue.getId == venue.getId &&
        eventsOverlap(event, proposedStart, proposedEnd)
    )
  }

  /**
   * Checks if an existing event overlaps with the proposed time range.
   * Uses standard overlap formula: (StartA < EndB) && (EndA > StartB)
   */
  private def eventsOverlap(
                             existingEvent: Event,
                             proposedStart: LocalDateTime,
                             proposedEnd: LocalDateTime
                           ): Boolean = {
    val existingStart = existingEvent.getDateTime
    val existingEnd = existingEvent.getEndTime

    // Returns true if the time ranges overlap
    proposedStart.isBefore(existingEnd) && proposedEnd.isAfter(existingStart)
  }

  /**
   * Finds all available venues (not just the first one).
   * Useful for giving users options.
   */
  def findAllAvailableSlots(
                             venues: java.util.List[Venue],
                             existingEvents: java.util.List[Event],
                             requiredCapacity: Int,
                             earliestStartDate: LocalDateTime,
                             duration: Duration
                           ): java.util.List[Venue] = {

    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    val availableVenues = venueList
      .filter(_.getCapacity >= requiredCapacity)
      .filter(venue => isVenueAvailableAtTime(venue, eventList, earliestStartDate, duration))
      .sortBy(_.getCapacity)

    // Convert back to Java List for Kotlin interop
    availableVenues.asJava
  }
}