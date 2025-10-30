package com.eventplanning.domain


// This data class represents a single physical venue for events.
data class Venue(
    val id: String, // A read-only property for the venue's unique ID.
    val name: String, // A read-only property for the venue's unique name.
    val capacity: Int, // A read-only property for the maximum number of people the venue can hold
    val location: String, // A read-only property for the venue's general area.
    val address: String, // A read-only property for the specific street address.
    // A read-only property for the unique features
    val facilities: List<String> = emptyList() // Provides a default empty list if none are given.
){
    // This block of code is used for validation
    init{
        // Gives an error if the condition is false.
        // Ensures the venue can hold at least one person
        require(capacity > 0) { "Capacity must be greater than zero." }
        // Ensures the venue has a name.
        require(name.isNotBlank()) { "Name must not be empty." }
        // Ensures the location is specified.
        require(location.isNotBlank()) { "Location must not be empty." }
        // Ensures the address is specified.
        require(address.isNotBlank()) { "Address must not be empty." }
        // Ensures the ID is specified
        require(id.isNotBlank()) { "Id must not be empty." }
        // Ensures that if a "facilities" list is provided, none of its items are blank strings.
        require(facilities.all { it.isNotBlank() }) { "Facilities must not be empty." }
    }
}