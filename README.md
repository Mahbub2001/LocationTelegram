# Location-Based Telegram Messenger

This Android app allows users to automatically send Telegram messages when they reach a specific destination. The workflow tracks the user's real-time location and sends a predefined message to a Telegram contact when the user comes within a specified radius of their destination.

## Features

- **Real-time location tracking** using GPS
- **Telegram integration** for automatic messaging
- **Interactive map** for destination selection
- **Configurable detection radius** (50m to 1000m)
- **Background location tracking** with foreground service
- **Customizable messages** and contacts

## Setup Instructions

### 1. Google Maps API Key

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Maps SDK for Android
   - Places API (optional, for enhanced map features)
4. Create credentials (API Key) for Android
5. Restrict the API key to your app's package name and SHA-1 fingerprint
6. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` in `app/src/main/res/values/strings.xml` with your actual API key

### 2. Telegram Bot Setup

1. Create a new Telegram bot:
   - Open Telegram and search for `@BotFather`
   - Send `/newbot` command
   - Follow the instructions to create your bot
   - Copy the bot token (format: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

2. Get your Chat ID:
   - Send a message to your bot
   - Visit: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
   - Find your chat ID in the response JSON
   - Alternatively, you can use the username directly (e.g., `@yourusername`)

### 3. App Configuration

1. Open the app
2. Enter your Telegram Bot Token
3. Enter the Chat ID or username of the recipient
4. Write your custom message
5. Tap "Select Destination on Map" to choose your pickup location
6. Adjust the detection radius using the slider
7. Tap "Test Telegram Message" to verify your setup
8. Tap "Start Tracking" to begin location monitoring

## How It Works

1. **Source**: The app continuously tracks your current location using GPS
2. **Process**: When you come within the specified radius of your destination, the app automatically sends your predefined message to the specified Telegram contact
3. **Destination**: The pickup location you selected on the map

## Permissions Required

- **Location Access**: For real-time location tracking
- **Background Location**: For tracking when the app is not in foreground
- **Internet**: For sending Telegram messages
- **Foreground Service**: For continuous location tracking

## Technical Details

- **Minimum SDK**: Android 12 (API 31)
- **Target SDK**: Android 14 (API 35)
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Location Services**: Google Play Services Location API
- **Network**: Retrofit with OkHttp
- **Storage**: SharedPreferences

## Usage Tips

1. **Battery Optimization**: The app uses a foreground service for location tracking, which may consume battery. Consider adjusting the detection radius to balance accuracy and battery life.

2. **Location Accuracy**: For best results, ensure you have a clear view of the sky for GPS signal reception.

3. **Testing**: Always test your Telegram configuration before starting a tracking session.

4. **Privacy**: The app only stores data locally on your device. Location data is not sent to any server except for the destination coordinate comparison.

## Troubleshooting

### Location Not Updating
- Check if location permissions are granted
- Ensure GPS is enabled on your device
- Try moving to an area with better GPS reception

### Telegram Message Not Sending
- Verify your bot token is correct
- Check if the chat ID is valid
- Ensure you have internet connectivity
- Test the configuration using the "Test Telegram Message" button

### App Stops Tracking
- Check if battery optimization is disabled for the app
- Ensure background app refresh is enabled
- Verify that the app has background location permission

## Privacy & Security

- All configuration data is stored locally on your device
- The app only communicates with Telegram's official API
- Location data is used only for distance calculation to your destination
- No personal data is transmitted to third-party services

## Contributing

Feel free to submit issues and enhancement requests!

## License

This project is for educational purposes. Please ensure compliance with local privacy laws when using location tracking features.
