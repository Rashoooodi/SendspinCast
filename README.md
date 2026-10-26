# Sendspin Cast (Android → Music Assistant)

**Experimental v0.1.0** · Android 10+ · Kotlin · No root or Shizuku required

A small Android app that captures *permitted* app playback audio via Android MediaProjection / AudioPlaybackCapture, encodes it to AAC-LC at 48 kHz stereo, and publishes an HTTP live radio stream. Play that stream in Music Assistant on your existing Sendspin speaker group.

**Note:** This first version is **not a native Sendspin source**. It uses a Music Assistant radio URL as the bridge. No APK has been compiled or tested yet.

## Build

Requires Android Studio (Android SDK 35 and JDK 17).

1. Open this repository as a project in Android Studio and sync Gradle.
2. Run **Build → Build APK(s)**.
3. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.

On Windows you can instead run `./build-windows.ps1` in PowerShell; on macOS/Linux run `./build-linux-mac.sh`. These scripts download Gradle 8.10.2; an installed Android SDK and JDK are still required.

## Use

1. Connect your Android phone and Music Assistant server so the server can reach the phone's LAN IP on TCP port **8765**.
2. Open the app, press **Start audio cast**, and approve microphone/audio recording and screen-capture consent.
3. Press **Copy stream URL**.
4. In Music Assistant, add a custom radio station **from URL** using the copied private link.
5. Play that station on your Sendspin speaker group. Use the app's **Stop audio cast** button when finished.

Stream URL format: `http://PHONE_IP:8765/live.aac?key=RANDOM_PRIVATE_TOKEN`

## Test

- Open `http://PHONE_IP:8765/health` from a machine on your LAN. It should say `Sendspin Cast is running`.
- Test the private URL in VLC or `ffplay -nodisp 'http://PHONE_IP:8765/live.aac?key=YOUR_KEY'` before adding it to Music Assistant.
- If the URL times out, check phone/server routing and Wi-Fi client isolation. If silent, try playing audio from an app that permits playback capture.

## Limits and security

- Android only allows capturing audio from apps whose capture policy permits it. DRM media, calls and restricted apps may be silent. Shizuku by itself cannot override this.
- Music Assistant's live radio playback buffer may introduce **seconds of latency**. Speakers in the group can stay synchronized with each other but not necessarily with video on your phone.
- HTTP audio is **unencrypted**, and anyone who possesses the private link and can reach the phone can listen to it while running. Use only on trusted LANs or protected VPNs; don't publish the URL.
- The phone must stay connected and broadcasting. A changing phone IP requires updating the radio station URL.
- This is a proof of concept, not a finished or device-tested release.

## References

- [Android AudioPlaybackCapture](https://developer.android.com/media/platform/av-capture)
- [Music Assistant documentation](https://www.music-assistant.io/)
- [Sendspin source protocol](https://github.com/Sendspin/spec/blob/main/roles/source/v1.md)
