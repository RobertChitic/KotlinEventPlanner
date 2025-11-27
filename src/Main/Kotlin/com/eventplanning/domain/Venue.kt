package com. eventplanning.domain

/**
 * Represents a venue where events can be held.
 * Contains information about capacity, location, and available facilities.
 */
data class Venue(
    val id: String,
    val name: String,
    val capacity: Int,
    val location: String,
    val address: String,
    val facilities: List<String> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "ID must not be empty." }
        require(name.isNotBlank()) { "Name must not be empty." }
        require(capacity > 0) { "Capacity must be greater than zero." }
        require(location.isNotBlank()) { "Location must not be empty." }
        require(address.isNotBlank()) { "Address must not be empty." }
        require(facilities.all { it.isNotBlank() }) { "Facility names must not be empty." }
    }

    /**
     * Returns a formatted string of all facilities.
     */
    fun getFacilitiesDisplay(): String =
        if (facilities.isEmpty()) "None" else facilities.joinToString(", ")

    override fun toString(): String = "$name (Capacity: $capacity)"
}