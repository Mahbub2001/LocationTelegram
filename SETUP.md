# Setup Configuration

## Quick Setup Checklist

### 1. Google Maps API Key (REQUIRED)
- [ ] Go to [Google Cloud Console](https://console.cloud.google.com/)
- [ ] Create/select project
- [ ] Enable "Maps SDK for Android"
- [ ] Create Android API key
- [ ] Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` in `app/src/main/res/values/strings.xml`

### 2. Telegram Bot (REQUIRED)
- [ ] Message @BotFather on Telegram
- [ ] Create new bot with `/newbot`
- [ ] Save your bot token (format: `123456789:ABC...`)
- [ ] Get your chat ID by messaging the bot and visiting:
  `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`

### 3. Build & Install
```bash
./gradlew assembleDebug
```

## Sample Configuration

### Telegram Bot Token
```
1234567890:ABCdefGHIjklMNOpqrsTUVwxyz1234567890
```

### Chat ID Examples
```
123456789          # Numeric ID
@your_username     # Username (with @)
```

### Message Template Examples
```
"I've arrived at the pickup location! 🚗"
"Hey! I'm here at [location]. Come out when ready!"
"Arrived at destination. ETA for pickup: 2 minutes"
```

## Testing Steps

1. **Test Telegram Setup**:
   - Enter bot token and chat ID
   - Tap "Test Telegram Message"
   - Verify message is received

2. **Test Location**:
   - Allow location permissions
   - Select a nearby destination (10-20 meters away)
   - Set radius to 50 meters
   - Start tracking and walk to destination

3. **Production Use**:
   - Set realistic destination (pickup location)
   - Set appropriate radius (200-400 meters recommended)
   - Configure meaningful message
   - Start tracking before beginning journey

## Troubleshooting

### Common Issues

**Maps not loading**:
- Check API key is correct
- Verify Maps SDK for Android is enabled
- Check internet connection

**Messages not sending**:
- Verify bot token format
- Check chat ID (try both numeric and @username)
- Test with "Test Telegram Message" button

**Location not tracking**:
- Grant all location permissions
- Allow background location access
- Disable battery optimization for the app

### Permissions Needed
- ✅ Location (Fine & Coarse)
- ✅ Background Location 
- ✅ Internet
- ✅ Foreground Service

## Advanced Configuration

### Radius Recommendations
- **Urban areas**: 100-200m (signal interference)
- **Suburban areas**: 200-400m (balanced)
- **Rural areas**: 400-1000m (GPS accuracy)

### Battery Optimization
- Add app to battery whitelist
- Enable "Allow background activity"
- Consider larger radius to reduce GPS polling

## Security Notes

- API keys should be kept secure
- Bot tokens grant access to your Telegram bot
- Location data stays on device only
- Consider using environment variables for production
