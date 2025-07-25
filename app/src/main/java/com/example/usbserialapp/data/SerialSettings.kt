package com.example.usbserialapp.data

import com.hoho.android.usbserial.driver.UsbSerialPort

data class SerialSettings(
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = UsbSerialPort.STOPBITS_1,
    val parity: Int = UsbSerialPort.PARITY_NONE,
    val lineEnding: LineEnding = LineEnding.CRLF,
    val autoConnect: Boolean = false
)

enum class LineEnding(val value: String) {
    NONE(""),
    CR("\r"),
    LF("\n"),
    CRLF("\r\n");
    
    companion object {
        fun fromString(value: String): LineEnding {
            return when (value) {
                "cr" -> CR
                "lf" -> LF
                "crlf" -> CRLF
                else -> NONE
            }
        }
    }
}