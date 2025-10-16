package com.eventplanning.domain

import com.eventplanning.domain.Venue

import java.time.LocalDateTime

data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val venue: Venue,
    val description: String = "",
    val maxParticipants: Int = venue.capacity,
    val registrationParticipants: Int = venue.capacity ,





)