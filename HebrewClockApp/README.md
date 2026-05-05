# שעון שלי — Hebrew Clock Toddler App

An interactive Hebrew-language clock app designed for toddlers (age 2+).

## Features

- **Live analog clock** — shows the real current time
- **Draggable hands** — touch and drag the hour (red) or minute (blue) hand to explore different times
- **Hebrew time display** — the full time is spoken in natural Hebrew ("שעה שלוש וחצי")
- **Large Hebrew hour name** — big, bold text perfect for little eyes
- **Animated background** — background color shifts with the time of day (sunrise yellows, afternoon greens, night blues)
- **Star burst celebration** — a sparkle animation plays every time the child moves a clock hand
- **"What time is it now?" button** — snaps back to real time with a smooth animation
- **RTL layout** — fully right-to-left, Hebrew-first design

## Building

1. Open the `HebrewClockApp` folder in Android Studio
2. Let Gradle sync
3. Run on a device or emulator (API 24+)

Or build from command line:
```bash
cd HebrewClockApp
./gradlew assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Clock interaction for toddlers

| Action | Result |
|--------|--------|
| Drag red hand | Move the hour |
| Drag blue hand | Move the minutes |
| Tap Hebrew text | Bouncy animation |
| Tap "מה השעה עכשיו?" | Jump to real time |

## Hebrew hours used

| Number | Hebrew |
|--------|--------|
| 1 | אחת |
| 2 | שתיים |
| 3 | שלוש |
| 4 | ארבע |
| 5 | חמש |
| 6 | שש |
| 7 | שבע |
| 8 | שמונה |
| 9 | תשע |
| 10 | עשר |
| 11 | אחת עשרה |
| 12 | שתים עשרה |
