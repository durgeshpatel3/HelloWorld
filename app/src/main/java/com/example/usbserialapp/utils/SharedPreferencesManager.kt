package com.example.usbserialapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.usbserialapp.data.LineEnding
import com.example.usbserialapp.data.SerialSettings
import com.hoho.android.usbserial.driver.UsbSerialPort

class SharedPreferencesManager(context: Context) {
    
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    fun getSerialSettings(): SerialSettings {
        val baudRate = preferences.getString("baud_rate", "115200")?.toIntOrNull() ?: 115200
        val dataBits = preferences.getString("data_bits", "8")?.toIntOrNull() ?: 8
        val stopBitsString = preferences.getString("stop_bits", "1") ?: "1"
        val parityString = preferences.getString("parity", "0") ?: "0"
        val lineEndingString = preferences.getString("line_ending", "crlf") ?: "crlf"
        val autoConnect = preferences.getBoolean("auto_connect", false)
        
        val stopBits = when (stopBitsString) {
            "1" -> UsbSerialPort.STOPBITS_1
            "3" -> UsbSerialPort.STOPBITS_1_5
            "2" -> UsbSerialPort.STOPBITS_2
            else -> UsbSerialPort.STOPBITS_1
        }
        
        val parity = when (parityString) {
            "0" -> UsbSerialPort.PARITY_NONE
            "1" -> UsbSerialPort.PARITY_ODD
            "2" -> UsbSerialPort.PARITY_EVEN
            "3" -> UsbSerialPort.PARITY_MARK
            "4" -> UsbSerialPort.PARITY_SPACE
            else -> UsbSerialPort.PARITY_NONE
        }
        
        val lineEnding = LineEnding.fromString(lineEndingString)
        
        return SerialSettings(
            baudRate = baudRate,
            dataBits = dataBits,
            stopBits = stopBits,
            parity = parity,
            lineEnding = lineEnding,
            autoConnect = autoConnect
        )
    }
    
    fun registerOnPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }
    
    fun unregisterOnPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}