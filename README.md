# 🛡️ Resistance Timer

An elite, offline-first Android application designed to wage war against mindless doomscrolling. Heavily inspired by Steven Pressfield's philosophical work *The War of Art*, the app leverages cognitive friction, interactive warning systems, and real-time app intercepts to block distraction loops and reclaim your time.

---

## 🚀 The Core Philosophy: Fighting "Resistance"
> *"Resistance is always lying and always full of shit." — Steven Pressfield*

In *The War of Art*, Resistance is the force that stops you from doing your creative work. In the modern era, Resistance takes the form of infinite scrolling algorithms designed to hook your attention. 

**Resistance Timer** intercepts your screen when your pre-set limits run out, forcing you to look at hard-hitting quotes from the book. If you choose to extend your time, you must actively confront a cylinder-wrapped 3D picker drum that changes the entire app theme to dark burgundy, symbolizing your "Surrender" to Resistance.

---

## ✨ Features

- 🕵️ **Real-Time Foreground Tracking**: Completely offline background service tracking active applications using native Android usage statistics APIs.
- 🚧 **Full-Screen Intercept Overlay**: An overlay window that intercepts target apps (Instagram, Reddit, YouTube, etc.) immediately when your daily scroll limit expires.
- 🎛️ **Obsidian Glassmorphic UI**: Beautiful, dark aesthetics inspired by premium design patterns (obsidian gradients, neon colors, glassmorphic borders).
- 🎡 **3D Resistance Wheel (VerticalPager)**: A native physics-snapped scroll picker with dynamic cylinder wrapping (rotations, scales, and opacity offsets linked to drag gesture).
- 🌡️ **Dynamic Warnings & Gradients**: Visual warnings that change behavior, quotes, and colors (Mild Orange 😬 → Moderate Red 😔 → High Crimson 😤 → Deep Burgundy 💀) the longer you choose to extend scrolling time.
- 📊 **Circular Dial Progress Metrics**: Sleek dashboard tracking daily screen allowance usage, remaining time, and active app counts.
- 🎨 **Adaptive Launcher Icon**: Custom vector-crafted crest combining a defensive shield and a stopwatch face on a pitch-black background.

---

## 🛠️ Technology Stack & Architecture

- **Core Platform**: Native Android (Kotlin, Min SDK 26, Target SDK 34)
- **UI Framework**: Jetpack Compose & Material 3 (100% declarative UI with fluid spring animations)
- **Local DB**: Room Database (Offline storage tracking app limits, daily used seconds, and session logs)
- **Architecture**: MVVM (Model-View-ViewModel) utilizing Kotlin StateFlows for real-time reactivity
- **Navigation**: Compose Navigation (Seamless transitions between Home Dashboard and detailed Stats)
- **Background Engine**: Android Foreground Service + UsageStatsManager + SystemAlertWindow API

---

## 📂 Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── java/com/resistancetimer/
│   │   │   ├── MainActivity.kt               # App entrypoint & permission routers
│   │   │   ├── ResistanceApp.kt              # App initializations
│   │   │   ├── ResistanceAlertActivity.kt    # Intercept overlay containing picker drum UI
│   │   │   ├── BootReceiver.kt               # Restarts service on phone reboot
│   │   │   ├── data/                         # Room DB definitions
│   │   │   │   ├── AppDatabase.kt            # Room database constructor
│   │   │   │   ├── AppLimit.kt               # App limit data model
│   │   │   │   ├── AppLimitDao.kt            # Dao for limits transactions
│   │   │   │   └── UsageSession.kt           # Session tracking logs
│   │   │   ├── service/                      # Watcher service
│   │   │   │   └── AppWatcherService.kt      # Foreground polling watcher loop
│   │   │   └── ui/                           # UI Package
│   │   │       ├── MainViewModel.kt          # Shares business logic and states
│   │   │       └── screens/                  # Compose layouts
│   │   │           ├── HomeScreen.kt         # Limits dashboard & custom limit dialog
│   │   │           ├── StatsScreen.kt        # Daily scrolling usage charts
│   │   │           └── PermissionsScreen.kt  # Onboarding walkthrough
│   │   └── res/                              # Layout resources, vectors, and icons
```

---

## ⚙️ How It Works Under the Hood

### 1. Foreground Detection Service
`AppWatcherService` runs as a native Android Foreground Service. Every **1 second**, it polls the active task using `UsageStatsManager.queryEvents`. This method runs fully on-device, respects user privacy, and does not perform any network requests.

### 2. Time-Allowance Calculation
The service retrieves active limits from the `AppDatabase`. If the active foreground package matches a monitored app:
- It increments the active usage time inside the database.
- It calculates remaining time: `Allowance = LimitSeconds + ExtraSecondsEarned`.

### 3. Screen Intercept Overlay
When usage exceeds the allowance:
- The service starts `ResistanceAlertActivity` with flag `Intent.FLAG_ACTIVITY_NEW_TASK`.
- The activity draws a floating window over the screen utilizing `SYSTEM_ALERT_WINDOW` permission, locking out the user from interacting with the target app.

### 4. Interactive Snapped Drum Picker
Inside the alert overlay, users can:
- **Lose (Victory)**: Click *"I'm done. Resistance loses. 💪"*, which closes the app and returns them to the Android Home screen.
- **Surrender (Extend)**: Drag the 3D physics-snapped selector drum to select a scroll time extension (from 1m to 60m). The extension selection dynamically shifts the background dimming, quotes, and glow colors to indicate how deep they are surrendering to the scroll addiction.

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio Koala / Ladybug or newer
- JDK 17
- Android Device running Android 8.0 (API level 26) or higher

### Build Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/ogbhaiyu/resistance-timer.git
   ```
2. Open the project in Android Studio.
3. Wait for the Gradle project sync to complete.
4. Run the `:app` configuration on your physical device or emulator.

### Permissions Required
- **Usage Access**: Enables background tracking of active packages.
- **Display Over Other Apps**: Allows the blocking overlay card to render on top of doomscroll apps.
