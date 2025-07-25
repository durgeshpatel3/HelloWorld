# USB Serial Android App

A modern Android application for communicating with hardware devices via USB serial connection. The app provides an intuitive interface for configuring serial communication parameters, sending commands, and monitoring incoming data in real-time.

## Features

### Core Functionality
- **USB Host Support**: Connect to USB-to-Serial adapters and devices
- **Real-time Communication**: Continuous listening for incoming serial data
- **Command Interface**: Send custom commands to connected hardware
- **Settings Screen**: Configure baud rate, data bits, stop bits, parity, and line endings
- **Auto-connect**: Automatic connection when USB devices are attached
- **Background Service**: Foreground service for continuous operation

### Supported USB Serial Adapters
- FTDI devices (FT232, FT2232, etc.)
- Prolific PL2303 devices
- CH340/CH341 devices
- CP210x devices (Silicon Labs)
- Arduino boards with USB serial capability
- Generic CDC (Communication Device Class) devices

### Modern UI/UX
- Material Design 3 interface
- Dark/Light theme support
- Real-time connection status
- Color-coded message logging (TX/RX)
- Scrollable communication log with timestamps
- Settings organized in categories

## Technical Architecture

### Key Components

1. **MainActivity**: Main interface for device connection and communication
2. **SettingsActivity**: Configuration screen for serial parameters
3. **UsbSerialManager**: Core USB serial communication handler
4. **UsbSerialService**: Background foreground service
5. **SharedPreferencesManager**: Settings persistence utility

### Communication Flow
```
USB Device → UsbSerialManager → MainActivity → UI Display
                ↓
Settings ← SharedPreferencesManager ← SettingsActivity
```

### Dependencies
- `usb-serial-for-android`: USB serial communication library
- `AndroidX`: Modern Android components
- `Material Design Components`: UI components
- `Kotlin Coroutines`: Asynchronous programming
- `SharedFlow`: Reactive data streams

## Installation and Setup

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK API 21+ (Android 5.0+)
- USB-enabled Android device
- USB OTG cable (for connecting USB devices to phone/tablet)

### Build Instructions

1. **Clone the repository**:
```bash
git clone <repository-url>
cd usb-serial-android-app
```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory

3. **Build the project**:
   - Wait for Gradle sync to complete
   - Build → Make Project (Ctrl+F9)

4. **Run on device**:
   - Connect an Android device via USB
   - Enable USB debugging in Developer Options
   - Run → Run 'app' (Shift+F10)

### APK Installation
You can also build and install the APK directly:
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage Guide

### First-Time Setup

1. **Enable USB Host Mode**:
   - Ensure your Android device supports USB Host mode (most modern devices do)
   - Connect a USB OTG adapter if using a phone/tablet

2. **Configure Settings**:
   - Open the app and tap the settings icon
   - Configure serial parameters to match your hardware:
     - **Baud Rate**: Common values are 9600, 115200
     - **Data Bits**: Usually 8
     - **Stop Bits**: Usually 1
     - **Parity**: Usually None
     - **Line Ending**: Choose based on your device (CRLF is common)

### Connecting to Hardware

1. **Connect USB Device**:
   - Plug your USB-to-Serial adapter into the Android device
   - The app should detect the device automatically

2. **Grant Permissions**:
   - When prompted, grant USB permission to the app
   - Enable auto-connect for automatic future connections

3. **Establish Connection**:
   - Tap the "Connect" button in the main screen
   - Connection status will show "Connected" in green

### Sending Commands

1. **Enter Command**:
   - Type your command in the text input field
   - Commands are sent with the configured line ending

2. **Send Data**:
   - Tap "Send Command" button
   - Or press Enter on the keyboard
   - Sent commands appear in the log with timestamp

### Monitoring Data

1. **Real-time Display**:
   - Received data appears automatically in the log
   - Each message includes timestamp and direction (TX/RX)
   - Different colors for sent vs received data

2. **Log Management**:
   - Tap "Clear Log" to reset the display
   - Log automatically scrolls to show latest data

## Configuration Options

### Serial Communication Settings

| Setting | Options | Description |
|---------|---------|-------------|
| Baud Rate | 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600 | Data transmission speed |
| Data Bits | 5, 6, 7, 8 | Number of data bits per character |
| Stop Bits | 1, 1.5, 2 | Number of stop bits |
| Parity | None, Even, Odd, Mark, Space | Error checking method |
| Line Ending | None, CR, LF, CRLF | End-of-line character(s) |

### Connection Settings

- **Auto Connect**: Automatically connect when USB device is attached
- Useful for embedded applications requiring immediate communication

## Hardware Compatibility

### Tested USB-to-Serial Adapters
- FTDI FT232RL/FT232H modules
- Prolific PL2303 adapters
- CH340G/CH341 adapters
- Silicon Labs CP2102/CP2104
- Arduino Uno/Nano/Pro Mini with USB

### Connection Examples

**Arduino Connection**:
```
Android Device (USB Host) ←→ USB Cable ←→ Arduino (USB Serial)
```

**Generic Hardware**:
```
Android Device (USB Host) ←→ USB-to-Serial Adapter ←→ Hardware Device (UART)
```

## Troubleshooting

### Common Issues

1. **Device Not Detected**:
   - Check USB OTG support on your Android device
   - Verify USB cable connection
   - Try a different USB-to-Serial adapter

2. **Permission Denied**:
   - Grant USB permission when prompted
   - Check that the device is listed in supported devices
   - Restart the app and try again

3. **Connection Failed**:
   - Verify serial settings match your hardware
   - Check baud rate compatibility
   - Ensure hardware device is powered and ready

4. **No Data Received**:
   - Verify hardware is sending data
   - Check line ending settings
   - Confirm baud rate and other parameters

### Debug Information

Enable debugging in Android Studio to see detailed logs:
```bash
adb logcat | grep -E "(UsbSerialManager|MainActivity)"
```

## Development

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/usbserialapp/
│   │   ├── MainActivity.kt              # Main activity
│   │   ├── SettingsActivity.kt          # Settings screen
│   │   ├── UsbSerialService.kt          # Background service
│   │   ├── data/                        # Data classes
│   │   │   ├── SerialSettings.kt
│   │   │   └── SerialMessage.kt
│   │   └── utils/                       # Utility classes
│   │       ├── UsbSerialManager.kt
│   │       └── SharedPreferencesManager.kt
│   ├── res/                            # Resources
│   │   ├── layout/                     # UI layouts
│   │   ├── values/                     # Strings, colors, themes
│   │   ├── drawable/                   # Icons
│   │   ├── menu/                       # Menus
│   │   └── xml/                        # Preferences, device filters
│   └── AndroidManifest.xml
└── build.gradle
```

### Adding New Features

1. **New Serial Protocols**: Extend `UsbSerialManager` class
2. **Additional Settings**: Add to `preferences.xml` and `SerialSettings`
3. **Custom UI**: Modify layouts in `res/layout/`
4. **Device Support**: Update `device_filter.xml`

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) library by Mike Wakerly
- Material Design Components for Android
- Android USB Host API documentation
- Contributors to the open-source USB serial ecosystem

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Search existing GitHub issues
3. Create a new issue with detailed information
4. Include device model, Android version, and logs

---

**Note**: This app requires Android devices with USB Host mode support. Most modern Android devices (Android 3.1+) support this feature, but some budget devices may not include the necessary hardware.
