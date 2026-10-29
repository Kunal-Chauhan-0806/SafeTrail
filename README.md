# SafeTrail

Proactive journey check-in and personal safety app for students, built as the project for the Mobile and Application Development course.

Most safety apps only help if you can press a button while in danger. SafeTrail works when you cannot: you start a journey with an expected arrival time, and if you do not check in, the app alerts your trusted contacts by itself, with a map link to your location.

## Features

- **Welcome screen**: explains each permission before asking for it.
- **Trusted circle**: save two to five emergency contacts, stored on the phone only.
- **Journey check-in**: set a destination and an ETA. The destination is checked as a real place and its address is shown before the journey starts.
- **Live location**: a foreground service keeps your position updated during the journey, and the journey screen shows your address, coordinates and a button to open it in Maps.
- **Automatic escalation**: when the countdown ends, SafeTrail asks "Are you safe?" and gives 30 seconds to answer. If there is no reply, it sends an SMS with a map link to every trusted contact.
- **Honest delivery results**: the SOS dialog and the alert notification report which messages were really sent, and why any failed (no SIM, airplane mode, no signal).
- **SOS and shake-to-SOS**: press SOS or shake the phone hard three times. A 5 second countdown lets you cancel a false alarm.
- **Fake incoming call**: a realistic call screen with a caller name, ringtone, vibration and ring delay.
- **Light and dark themes**, switchable on the home screen.

## Screenshots

| Home | Journey | Journey in progress | Contacts | Fake call |
|------|---------|---------------------|----------|-----------|
| ![Home](docs/screenshots/home.png) | ![Journey](docs/screenshots/journey-start.png) | ![Active](docs/screenshots/journey-active.png) | ![Contacts](docs/screenshots/contacts.png) | ![Fake call](docs/screenshots/fake-call.png) |

## Built with

- Kotlin, Jetpack Compose and Material Design 3
- Room (with KSP) for the local database, Kotlin Flow for live updates
- Fused Location Provider (Google Play services) and a foreground service for location
- Android Geocoder, with OpenStreetMap Nominatim as a backup, for checking places and finding addresses
- AlarmManager exact alarms, BroadcastReceiver and notifications for the check-in
- SensorManager (accelerometer) for shake detection
- SmsManager with delivery reports for the alert messages

## Install and run

**On a phone with a cable (recommended):** turn on USB debugging, connect the phone, open the project in Android Studio and press Run. Or install the signed APK from the Releases page with `adb install SafeTrail.apk`.

**From an APK file:** on Android 13 or newer, open App info for SafeTrail, tap the three-dot menu and choose "Allow restricted settings", otherwise the SMS permission stays blocked.

Add a trusted contact before starting a journey. SOS and alerts send real SMS messages, so use a number you control and expect normal SMS charges. The "1 min (demo)" option on the journey screen lets you try the whole alert flow quickly. On an emulator, set a fake location with `adb emu geo fix <longitude> <latitude>`.

## Permissions

| Permission | Why it is needed |
|------------|------------------|
| SEND_SMS | send alerts to trusted contacts |
| ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION | add your location to alerts |
| POST_NOTIFICATIONS | show the "Are you safe?" prompt |
| USE_EXACT_ALARM | fire the check-in at the exact ETA |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION | keep location fresh during a journey |
| INTERNET | check destinations and look up addresses |
| VIBRATE | vibrate during the fake call |

## Known limitations

- The location in an alert is a snapshot of your latest position, not a link that keeps updating on the receiver's screen.
- Shake-to-SOS works while the app is open. Shake detection in the background is planned.
- Google Play restricts the SMS and exact alarm permissions, so a public Play Store release would need a different way to deliver alerts.
- Alerts depend on mobile signal, SIM balance and the phone being switched on.

## Project docs

- [Work log](docs/worklog.md)
- [Privacy policy](docs/PRIVACY.md)

## Author

Kunal Chauhan, roll number N018. Mentor: Prof. Fahim Khan.
