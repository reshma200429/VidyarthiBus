package com.reshma.vidyarthibus.model

data class BusReport(
    val reportId: String = "",
    val busId: String = "",
    val userId: String = "",
    val crowdStatus: String = "EMPTY",
    val timestamp: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun getCrowdStatusEnum(): CrowdStatus {
        return CrowdStatus.valueOf(crowdStatus)
    }
}