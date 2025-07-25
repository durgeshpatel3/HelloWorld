package com.example.usbserialapp.utils

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.example.usbserialapp.data.SerialMessage
import com.example.usbserialapp.data.SerialSettings
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException

class UsbSerialManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbSerialManager"
        private const val READ_TIMEOUT = 100
        private const val WRITE_TIMEOUT = 1000
    }
    
    private var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var isConnected = false
    private var readingJob: Job? = null
    
    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()
    
    private val _receivedMessages = MutableSharedFlow<SerialMessage>()
    val receivedMessages: SharedFlow<SerialMessage> = _receivedMessages.asSharedFlow()
    
    private val _sentMessages = MutableSharedFlow<SerialMessage>()
    val sentMessages: SharedFlow<SerialMessage> = _sentMessages.asSharedFlow()
    
    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()
    
    init {
        _connectionState.tryEmit(false)
    }
    
    fun getAvailableDevices(): List<UsbDevice> {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return availableDrivers.map { it.device }
    }
    
    fun connect(device: UsbDevice?, settings: SerialSettings): Boolean {
        if (device == null) {
            _errors.tryEmit("No USB device provided")
            return false
        }
        
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            _errors.tryEmit("No driver found for device")
            return false
        }
        
        if (driver.ports.isEmpty()) {
            _errors.tryEmit("No ports available on device")
            return false
        }
        
        val port = driver.ports[0]
        
        try {
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                _errors.tryEmit("Failed to open USB device")
                return false
            }
            
            port.open(connection)
            port.setParameters(
                settings.baudRate,
                settings.dataBits,
                settings.stopBits,
                settings.parity
            )
            
            serialPort = port
            isConnected = true
            _connectionState.tryEmit(true)
            
            // Start reading data
            startReading()
            
            Log.d(TAG, "Connected to USB serial device")
            return true
            
        } catch (e: IOException) {
            Log.e(TAG, "Error connecting to device", e)
            _errors.tryEmit("Connection failed: ${e.message}")
            return false
        }
    }
    
    fun disconnect() {
        readingJob?.cancel()
        
        try {
            serialPort?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing serial port", e)
        }
        
        serialPort = null
        isConnected = false
        _connectionState.tryEmit(false)
        
        Log.d(TAG, "Disconnected from USB serial device")
    }
    
    fun sendData(data: String, lineEnding: String = ""): Boolean {
        val port = serialPort
        if (!isConnected || port == null) {
            _errors.tryEmit("Not connected to any device")
            return false
        }
        
        try {
            val dataToSend = data + lineEnding
            val bytes = dataToSend.toByteArray()
            val bytesWritten = port.write(bytes, WRITE_TIMEOUT)
            
            if (bytesWritten == bytes.size) {
                _sentMessages.tryEmit(SerialMessage(data, isOutgoing = true))
                Log.d(TAG, "Sent: $data")
                return true
            } else {
                _errors.tryEmit("Failed to send complete data")
                return false
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error sending data", e)
            _errors.tryEmit("Send failed: ${e.message}")
            return false
        }
    }
    
    private fun startReading() {
        readingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            val stringBuilder = StringBuilder()
            
            while (isConnected && !isInterrupted) {
                try {
                    val port = serialPort ?: break
                    val bytesRead = port.read(buffer, READ_TIMEOUT)
                    
                    if (bytesRead > 0) {
                        val receivedData = String(buffer, 0, bytesRead)
                        stringBuilder.append(receivedData)
                        
                        // Process complete lines
                        val lines = stringBuilder.toString().split("\\r\\n|\\r|\\n".toRegex())
                        if (lines.size > 1) {
                            // Process all complete lines except the last one (which might be incomplete)
                            for (i in 0 until lines.size - 1) {
                                if (lines[i].isNotEmpty()) {
                                    _receivedMessages.tryEmit(SerialMessage(lines[i]))
                                    Log.d(TAG, "Received: ${lines[i]}")
                                }
                            }
                            // Keep the last line (might be incomplete)
                            stringBuilder.clear()
                            stringBuilder.append(lines.last())
                        }
                    }
                    
                } catch (e: IOException) {
                    if (isConnected) {
                        Log.e(TAG, "Error reading data", e)
                        _errors.tryEmit("Read error: ${e.message}")
                        break
                    }
                }
            }
        }
    }
    
    fun isConnected(): Boolean = isConnected
}