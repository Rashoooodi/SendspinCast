# Sendspin Cast

Turn Android playback audio into a Music Assistant radio stream, then play it through your Sendspin speaker groups.

### v0.2.0-alpha: Essentials-inspired UI

- Material 3 styling with device dynamic dark colors on Android 12+.
- Animated ambient gradients, pulsing waveform orb, responsive casting controls.
- Real frosted **progressive backdrop blur** using [Haze](https://github.com/chrisbanes/haze) (Apache-2.0). No screenshots or source code copied from Essentials or BitChord.
- Private stream key hidden on screen, explicit Copy / Share actions.
- **Audio engine unchanged** from working v0.1: AAC-LC, 48 kHz stereo, local HTTP streaming, Android AudioPlaybackCapture.

### Install

Open [GitHub Actions](https://github.com/Rashoooodi/SendspinCast/actions), choose the latest successful **Build Android APK** run, download `SendspinCast-debug-apk` from Artifacts, extract and install the APK.

### Use

1. On Android 10+, open the app and tap **Start casting**.
2. Accept Android's screen/audio capture permission (the app captures **audio only**).
3. Copy the private stream link.
4. In Music Assistant → Radio, add a station by URL and paste the link.
5. Play it on a single speaker or Sendspin group.

`http://PHONE_IP:8765/live.aac?key=PRIVATE_TOKEN`

### Technical notes

- Android only permits audio capture from apps whose playback-capture policy allows it. DRM/calls may be silent.
- The HTTP stream is **not encrypted**. Don't share the URL outside your trusted LAN/tailnet. Anyone with network access and the key can listen while casting.
- Music Assistant radio buffering may add several seconds; on a tested setup, playback lag was ~8 seconds with Music Assistant and ~2–3 seconds with VLC.
- No root or Shizuku is needed. Phone and Music Assistant server must be mutually reachable.
- Build from source with Android Studio + JDK 17 + Android SDK 35. See `build-windows.ps1` or `build-linux-mac.sh` as alternatives.
- This is an alpha build. The UI has not been device-tested across OEMs.

### Attribution

Inspired by modern Material 3 Expressive and Android glass UIs, including [Essentials](https://www.sameerasw.com/essentials) and [BitChord](https://github.com/search?q=BitChord&type=repositories). This project does not include or claim their branding, artwork, or proprietary assets. Haze is used under its Apache-2.0 license.
