package com.eventplanning.domain

// This data class represents a single participant who can register for events
data class Participant(
    val id: String, //read-only property, for participant's unique ID
    val name: String, //read-only property, for participant's full name
    val email: String, //read-only property, for participant's email address
    val phone: String = "", // //read-only property, for participant's phone number
    val organization: String = "" //read-only property, for participant's organization
)
{
    // This block of code is used for validation
    init{
        // Ensures the participant does not create a blank name
        // Will give an error if the condition is false
        require(name.isNotBlank()) { "Name must not be blank" }
        // Checks if the email matches a require expression
        // A format such as "something@something.something
        require(email.matches(Regex(".+@.+\\..+"))) { "Email Invalid" }
    }
}