# Radmon Radio

An Android application for streaming Radio Monash live radio.

## Features

- Live radio streaming
- Real-time song metadata and album artwork
- Favorite tracks management
- Listening statistics
- Quick Settings tile for easy playback control
- Background playback with media notifications
- Multi-platform music links (Spotify, Apple Music, YouTube Music, etc.)

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection

## Building

1. Clone this repository
2. Open in Android Studio
3. Create a `local.properties` file in the root directory with your Android SDK path:
   ```
   sdk.dir=/path/to/your/Android/Sdk
   ```
4. Build the project:
   ```bash
   ./gradlew build
   ```

## Tech Stack

- Kotlin
- Jetpack Compose
- Media3 (ExoPlayer)
- Retrofit
- Coil
- DataStore
- Material 3

## License

This project is licensed under the MIT License - see the LICENSE file for details.
