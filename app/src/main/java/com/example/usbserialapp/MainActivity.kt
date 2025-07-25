package com.example.usbserialapp

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.usbserialapp.data.SerialMessage
import com.example.usbserialapp.data.SerialSettings
import com.example.usbserialapp.databinding.ActivityMainBinding
import com.example.usbserialapp.utils.SharedPreferencesManager
import com.example.usbserialapp.utils.UsbSerialManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val ACTION_USB_PERMISSION = "com.example.usbserialapp.USB_PERMISSION"
    }
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var usbSerialManager: UsbSerialManager
    private lateinit var preferencesManager: SharedPreferencesManager
    
    private var currentSettings: SerialSettings = SerialSettings()
    private var pendingDevice: UsbDevice? = null
    
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.also { 
                                connectToDevice(it)
                            }
                        } else {
                            showToast(getString(R.string.usb_permission_denied))
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null && usbSerialManager.isConnected()) {
                        usbSerialManager.disconnect()
                        showToast("USB device disconnected")
                    }
                }
            }
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, service can show notifications
        } else {
            showToast("Notification permission denied. Background service notifications won't be shown.")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        initializeManagers()
        setupUI()
        observeSerialManager()
        requestNotificationPermissionIfNeeded()
        
        // Handle USB device attached intent
        handleUsbDeviceIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleUsbDeviceIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        registerUsbReceiver()
        currentSettings = preferencesManager.getSerialSettings()
    }
    
    override fun onPause() {
        super.onPause()
        unregisterReceiver(usbPermissionReceiver)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        usbSerialManager.disconnect()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun initializeManagers() {
        usbSerialManager = UsbSerialManager(this)
        preferencesManager = SharedPreferencesManager(this)
        currentSettings = preferencesManager.getSerialSettings()
    }
    
    private fun setupUI() {
        binding.connectButton.setOnClickListener {
            if (usbSerialManager.isConnected()) {
                usbSerialManager.disconnect()
            } else {
                connectToFirstAvailableDevice()
            }
        }
        
        binding.sendButton.setOnClickListener {
            sendCommand()
        }
        
        binding.commandEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendCommand()
                true
            } else {
                false
            }
        }
        
        binding.clearLogButton.setOnClickListener {
            binding.dataLogTextView.text = ""
        }
    }
    
    private fun observeSerialManager() {
        lifecycleScope.launch {
            usbSerialManager.connectionState.collect { isConnected ->
                updateConnectionUI(isConnected)
            }
        }
        
        lifecycleScope.launch {
            usbSerialManager.receivedMessages.collect { message ->
                appendToLog(message, ContextCompat.getColor(this@MainActivity, R.color.received_data))
            }
        }
        
        lifecycleScope.launch {
            usbSerialManager.sentMessages.collect { message ->
                appendToLog(message, ContextCompat.getColor(this@MainActivity, R.color.sent_data))
            }
        }
        
        lifecycleScope.launch {
            usbSerialManager.errors.collect { error ->
                showToast(error)
            }
        }
    }
    
    private fun updateConnectionUI(isConnected: Boolean) {
        if (isConnected) {
            binding.connectionStatusText.text = getString(R.string.connected)
            binding.connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
            binding.connectButton.text = getString(R.string.disconnect)
            binding.sendButton.isEnabled = true
        } else {
            binding.connectionStatusText.text = getString(R.string.disconnected)
            binding.connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
            binding.connectButton.text = getString(R.string.connect)
            binding.sendButton.isEnabled = false
        }
    }
    
    private fun connectToFirstAvailableDevice() {
        val devices = usbSerialManager.getAvailableDevices()
        if (devices.isEmpty()) {
            showToast(getString(R.string.no_usb_device))
            return
        }
        
        val device = devices.first()
        requestUsbPermission(device)
    }
    
    private fun requestUsbPermission(device: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun connectToDevice(device: UsbDevice) {
        val success = usbSerialManager.connect(device, currentSettings)
        if (success) {
            showToast("Connected to USB device")
            startUsbSerialService()
        } else {
            showToast(getString(R.string.connection_failed))
        }
    }
    
    private fun sendCommand() {
        val command = binding.commandEditText.text.toString().trim()
        if (command.isNotEmpty()) {
            val success = usbSerialManager.sendData(command, currentSettings.lineEnding.value)
            if (success) {
                binding.commandEditText.text?.clear()
            }
        }
    }
    
    private fun appendToLog(message: SerialMessage, color: Int) {
        val spannable = SpannableStringBuilder(message.getDisplayText() + "\n")
        spannable.setSpan(
            ForegroundColorSpan(color),
            0, spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.dataLogTextView.append(spannable)
        
        // Auto-scroll to bottom
        binding.dataLogTextView.post {
            val scrollView = binding.dataLogTextView.parent as? android.widget.ScrollView
            scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
    
    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbPermissionReceiver, filter)
    }
    
    private fun handleUsbDeviceIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (device != null && currentSettings.autoConnect && !usbSerialManager.isConnected()) {
                requestUsbPermission(device)
            }
        }
    }
    
    private fun startUsbSerialService() {
        val serviceIntent = Intent(this, UsbSerialService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}