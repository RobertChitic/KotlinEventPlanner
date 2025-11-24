package com.eventplanning.scheduling

import com.eventplanning.domain.{Venue, Event}
import java.time.LocalDateTime
import scala.jdk.CollectionConverters._

object SlotFinder {

  /**
   * Finds the first available venue that meets capacity requirements
   * and is available at or after the specified start date.
   *
   * Functional implementation using Scala collections.
   *
   * @param venues List of all available venues
   * @param existingEvents List of already scheduled events
   * @param requiredCapacity Minimum capacity needed
   * @param earliestStartDate Earliest acceptable start date/time
   * @return Option containing the first available venue, or None if no venue available
   */
  def findAvailableSlot(
                         venues: java.util.List[Venue],
                         existingEvents: java.util.List[Event],
                         requiredCapacity: Int,
                         earliestStartDate: LocalDateTime
                       ): Option[Venue] = {

    // Convert Java collections to Scala collections for functional operations
    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    // Functional pipeline: filter by capacity, then find first available
    venueList
      .filter(venue => venue.getCapacity >= requiredCapacity)
      .sortBy(_.getCapacity) // Prioritize smaller venues that meet requirements
      .find(venue => isVenueAvailableAtTime(venue, eventList, earliestStartDate))
  }

  /**
   * Checks if a venue is available at a specific date/time.
   * A venue is available if no existing event is scheduled at that venue and time.
   *
   * @param venue The venue to check
   * @param events List of existing events
   * @param proposedDateTime The proposed date/time
   * @return true if venue is available, false otherwise
   */
  private def isVenueAvailableAtTime(
                                      venue: Venue,
                                      events: List[Event],
                                      proposedDateTime: LocalDateTime
                                    ): Boolean = {
    // Functional approach: check if NO events conflict
    !events.exists(event =>
      event.getVenue.getId == venue.getId &&
        eventsOverlap(event, proposedDateTime)
    )
  }

  /**
   * Checks if an existing event overlaps with a proposed start time.
   * Considers event duration when checking for conflicts.
   */
  private def eventsOverlap(
                             existingEvent: Event,
                             proposedStartTime: LocalDateTime
                           ): Boolean = {
    val existingStart = existingEvent.getDateTime
    val existingEnd = existingEvent.getEndTime

    // Check if proposed time falls within existing event's time slot
    proposedStartTime.isAfter(existingStart) && proposedStartTime.isBefore(existingEnd) ||
      proposedStartTime.isEqual(existingStart)
  }

  /**
   * Finds all available venues (not just the first one).
   * Useful for giving users options.
   */
  def findAllAvailableSlots(
                             venues: java.util.List[Venue],
                             existingEvents: java.util.List[Event],
                             requiredCapacity: Int,
                             earliestStartDate: LocalDateTime
                           ): java.util.List[Venue] = {

    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    val availableVenues = venueList
      .filter(_.getCapacity >= requiredCapacity)
      .filter(venue => isVenueAvailableAtTime(venue, eventList, earliestStartDate))
      .sortBy(_.getCapacity)

    // Convert back to Java List for Kotlin interop
    availableVenues.asJava
  }
}