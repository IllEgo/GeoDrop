package com.e3hi.geodrop.data

data class Drop(
    val id: String = "",
    val text: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
