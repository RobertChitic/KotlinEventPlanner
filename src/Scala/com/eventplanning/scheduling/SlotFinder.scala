package com.eventplanning.scheduling

import com.eventplanning.domain.{Event, Venue}
import java.time.LocalDateTime
import scala.jdk.CollectionConverters._

/*
Slot finding algorithm will find the first available time for an venue, based on its capacity


*/
object SlotFinder {

  def findFirstAvailableVenue(
                               venues: java.util.List[Venue], // a list of venues, from the event manager
                               existingEvents: java.util.List[Event], //  a list of events, from the event manager
                               requiredCapacity: Int, // the reuqired capacity of the event, put in by the user
                               requestedDateTime: LocalDateTime // from what time should the slot be looked from
                             ): Option[Venue] = {

    // converting the kotlin data structures to Scala
    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    // filtering venues based on their capacity
    val suitableVenues = venueList.filter(_.capacity >= requiredCapacity)

    suitableVenues.find(venue => isVenueAvailable(venue, eventList, requestedDateTime))
  }

  private def isVenueAvailable(
                                venue: Venue,
                                events: List[Event],
                                requestedDateTime: LocalDateTime
                              ): Boolean = {

    val eventsAtVenue = events.filter(_.venue.id == venue.id)

    eventsAtVenue.forall(event => !isTimeConflict(event, requestedDateTime))
  }

  private def isTimeConflict(
                              event: Event,
                              requestedDateTime: LocalDateTime
                            ): Boolean = {
    val eventStart = event.dateTime
    val eventEnd = event.getEndTime

    !requestedDateTime.isBefore(eventStart) && requestedDateTime.isBefore(eventEnd)
  }

  def findAllAvailableVenues(
                              venues: java.util.List[Venue],
                              existingEvents: java.util.List[Event],
                              requiredCapacity: Int,
                              requestedDateTime: LocalDateTime
                            ): List[Venue] = {

    val venueList = venues.asScala.toList
    val eventList = existingEvents.asScala.toList

    venueList
      .filter(_.capacity >= requiredCapacity)
      .filter(venue => isVenueAvailable(venue, eventList, requestedDateTime))
  }

  def findAvailableVenuesSortedByCapacity(
                                           venues: java.util.List[Venue],
                                           existingEvents: java.util.List[Event],
                                           requiredCapacity: Int,
                                           requestedDateTime: LocalDateTime
                                         ): List[Venue] = {

    findAllAvailableVenues(venues, existingEvents, requiredCapacity, requestedDateTime)
      .sortBy(_.capacity)
  }
}