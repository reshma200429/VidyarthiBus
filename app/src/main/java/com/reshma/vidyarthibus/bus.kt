package com.reshma.vidyarthibus

data class Bus(

    val busId: String = "",

    val busNumber: String = "",

    val route: String = "",

    val driver: String = "",

    val status: String = "",

    val timing: String = "",

    val totalSeats: Int = 0,

    val availableSeats: Int = 0,

    val crowdStatus: String = "",

    val latitude: Double = 0.0,

    val longitude: Double = 0.0
)