package com.eventplanning.domain

data class Participant(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val organization: String = ""
)
{
    init{
        require(name.isNotBlank()) { "Name must not be blank" }
        require(email.matches(Regex(".+@.+\\..+"))) { "Email Invalid" }
    }
}