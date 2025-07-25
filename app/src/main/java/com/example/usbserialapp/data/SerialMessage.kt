package com.example.usbserialapp.data

import java.text.SimpleDateFormat
import java.util.*

data class SerialMessage(
    val data: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false
) {
    fun getFormattedTimestamp(): String {
        val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return format.format(Date(timestamp))
    }
    
    fun getDisplayText(): String {
        val direction = if (isOutgoing) "TX" else "RX"
        return "[${getFormattedTimestamp()}] $direction: $data"
    }
}