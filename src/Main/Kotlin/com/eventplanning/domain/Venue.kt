package com.eventplanning.domain


data class Venue(
    val id: String,
    val name: String,
    val capacity: Int,
    val location: String,
    val address: String,
    val facilities: List<String> = emptyList()
){
    init {
        require(capacity > 0) { "Capacity must be greater than zero." }
        require(name.isNotBlank()) { "Name must not be empty." }
        require(location.isNotBlank()) { "Location must not be empty." }
        require(address.isNotBlank()) { "Address must not be empty." }
        require(id.isNotBlank()) { "Id must not be empty." }
        require(facilities.all { it.isNotBlank() }) { "Facilities must not be empty." }
    }
}