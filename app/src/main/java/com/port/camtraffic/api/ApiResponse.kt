package com.port.camtraffic.api

class ApiResponse<T> (
    val rows: List<T>,
    val time: Double,
    val fields: Any,
    val total_rows: Int
)