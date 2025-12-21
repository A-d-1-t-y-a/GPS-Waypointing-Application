# GPS Waypointing Application - Technical Documentation

**Student Name:** [Your Name]
**Date:** October 2025
**Module:** Mobile Development

---

## 1. Introduction

This documentation details the implementation of a GPS Waypointing Application designed for hikers and orienteers. The application eliminates the need for predefined maps by allowing users to create their own "breadcrumbs" or waypoints and navigate back through them using a digital compass and radar interface.

The application is built using **Kotlin** and **Jetpack Compose**, adhering to modern Android development practices. It features a custom graphical compass, persistent data storage, and real-time GPS tracking.

---

## 2. Architecture Overview

The application follows the **MVVM (Model-View-ViewModel)** architectural pattern to ensure separation of concerns, testability, and maintainability.

### 2.1 Components
-   **View (UI)**: Implemented using Jetpack Compose in `SharedComposables.kt` and hosted by `MainActivity`.
-   **ViewModel**: `BeaconsViewModel`, `GuidanceViewModel`, and `HudViewModel` manage the UI state and business logic.
-   **Model (Data)**: `NavBeacon` represents the data entity.
-   **Service/Repository**: `PositionMonitor` and `GyroscopeMonitor` handle hardware sensors, while `BeaconRegistry` handles data persistence.

---

## 3. Class Documentation & Method Overview

### 3.1 MainActivity.kt
The entry point of the application.
-   `onCreate()`: Initializes the dependency injection for services (`PositionMonitor`, `BeaconRegistry`) and triggers the permission request flow (`ACCESS_FINE_LOCATION`). Sets the content to the `HudInterface` wrapped in the `OrionTheme`.

### 3.2 SharedComposables.kt
Contains all the UI components.
-   `HudInterface()`: The main screen composable. It orchestrates the flow of data between ViewModels and child composables. It sets up the `LaunchedEffect` for the GPS tracking loop and manages the high-level state (scanning vs idle).
-   `RadarDisplay()`: A custom `Canvas` composable that draws the compass, grid, and waypoints.
    -   *Drawing Logic*: Uses `drawCircle` for the radar scope and `drawText` for compass directions (N, S, E, W).
    -   *Projection*: Converts GPS coordinates (Lat/Lng) into Cartesian coordinates (x, y) relative to the screen center using the distance and bearing formula.
    -   *Gestures*: Implements `detectTapGestures` for selecting waypoints and `detectTransformGestures` for pinch-to-zoom functionality (500m to 2000m range).
-   `StatusReadout()`: Displays the current Target Locked information (Range and Vector bearing).
-   `CommandPanel()`: Contains the "Engage/Abort" tracking buttons and "Log Waypoint" button.
-   `BeaconsLog()`: A `LazyColumn` list displaying all saved waypoints.

### 3.3 PositionMonitor.kt (Service)
Wraps the Android `LocationManager`.
-   `trackPosition()`: Returns a Kotlin `Flow<Location>`. It registers a `LocationListener` that requests updates every **5 seconds** (5000ms) with a minimum distance of 5 meters. This ensures battery efficiency while meeting the assignment requirement.
-   `hasPerms()`: Utility to check for coarse/fine location permissions.

### 3.4 GyroscopeMonitor.kt (Service)
Handles device orientation.
-   Uses the `Sensor.TYPE_ROTATION_VECTOR` (or Accelerometer+Magnetometer fallback) to calculate the device's azimuth (bearing relative to magnetic North). This allows the compass to rotate smoothly.

### 3.5 BeaconRegistry.kt (Data)
Handles File I/O.
-   `commitBeacons()`: Serializes the list of `NavBeacon` objects into a JSON array and writes it to `beacons_db.json` in the app's internal storage.
-   `retrieveBeacons()`: Reads and parses the JSON file to restore the state on app launch.

### 3.6 BeaconsViewModel.kt
Manages the waypoint data.
-   `checkProximity()`: Monitor's the user's distance to the target. If the distance drops below **10 meters**, it automatically selects the previous waypoint (`idx - 1`), facilitating the "return home" feature.
-   `setRange()`: Updates the radar scale factor based on pinch zoom input.

---

## 4. Key Algorithms

### 4.1 Radar Projection
To display a GPS point on a 2D canvas, we use the Polar-to-Cartesian conversion:
1.  Calculate **Distance** ($d$) and **Bearing** ($\theta$) between User and Waypoint is calculated using `Location.distanceBetween()`.
2.  Normalize Distance based on Radar Range ($R_{max}$): $r_{norm} = \frac{d}{R_{max}} \times \frac{Width}{2}$.
3.  Calculate screen coordinates:
    $$x = Center_x + r_{norm} \cdot \sin(\theta_{rad})$$
    $$y = Center_y - r_{norm} \cdot \cos(\theta_{rad})$$
4.  Apply rotation based on user's compass azimuth to ensure "North" on the screen aligns with real North.

### 4.2 Auto-Navigation
The app continuously checks:
```kotlin
if (distance(user, target) < 10.0) {
    target = waypoints[currentIndex - 1]
}
```
This simple logic allows hands-free navigation through the waypoint list.

---

## 5. User Interface (UI) Design
The UI uses the "Orion" Design System (Golden/Cyan/Black theme) for high contrast in outdoor environments. 
-   **Void Black** background saves battery on OLED screens.
-   **Core Cyan** elements provide visibility.
-   **Caution Amber** highlights active targets.

---

## 6. Conclusion
The application successfully meets all functional requirements:
-   Group 1: Compass Visualisation (N/S/E/W).
-   Group 2: Tracking & Persistence (5s updates, JSON save).
-   Group 3: Waypoint Management (Distance/Direction).
-   Group 4: Semantic Zoom Radar (Visualisation).
-   Group 5: Advanced Gestures (Pinch-to-zoom, Auto-next).

The code is robust, fully documented, and robust against permission failures or empty data states.
