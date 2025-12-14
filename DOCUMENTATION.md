# GPS Waypointing Application - Code Documentation

## Project Overview

This GPS waypointing application is designed for hiking and orienteering activities where users need to track their location and create waypoints along their path. The application uses GPS sensors to track location and a custom canvas-based compass view to display waypoints and navigation information. Users can create waypoints while hiking and navigate back to them in reverse order if they get lost.

The application is built using Kotlin and Jetpack Compose, following a modern Android development approach with no XML layouts. All UI components are implemented using Compose composables, and the application uses only standard Android SDK libraries.

## Architecture

The application follows a modular architecture with clear separation of concerns:

- **MainActivity.kt**: Entry point that handles activity lifecycle and permission management
- **SharedComposables.kt**: Contains all UI composable functions
- **Waypoint.kt**: Data model for waypoint storage
- **WaypointRepository.kt**: Handles persistent storage of waypoints
- **LocationService.kt**: Manages GPS location tracking
- **CompassViewModel.kt**: Manages application state

## Method Documentation

### MainActivity.kt

#### `onCreate(savedInstanceState: Bundle?)`
Initializes the activity and sets up the core components of the application. This method is the starting point of the Android application lifecycle. It performs several critical initialization steps. First, it creates instances of LocationService and WaypointRepository, which are essential for the application's functionality. The LocationService is responsible for managing GPS interactions, while the WaypointRepository handles the persistent storage of waypoint data. 

After initializing these services, the method proceeds to load any previously saved waypoints from the internal storage by calling the `loadWaypoints()` method on the repository. This ensures that the user's data is preserved across application restarts. Following this, the method checks for the necessary location permissions. If the `ACCESS_FINE_LOCATION` permission has not been granted, it uses the registered activity result launcher to request this permission from the user. This is a crucial step to ensure that the application can access high-accuracy GPS data required for navigation.

Finally, the method sets up the user interface using Jetpack Compose. It calls `setContent` and applies the `GPSWaypointingTheme`. Inside the theme, a `Surface` container is created to hold the `MainScreen` composable, which is the primary UI component of the application. The `MainScreen` is passed the initialized `compassState`, `locationService`, and `waypointRepository` objects, effectively injecting these dependencies into the UI layer. This setup ensures that the UI has access to all necessary data and services to function correctly.

#### `onDestroy()`
Handles cleanup when the activity is destroyed. This method ensures that GPS tracking is properly stopped to prevent resource leaks and unnecessary battery consumption. It calls the stopTracking method on the LocationService to remove location listeners.

### Waypoint.kt

#### `toJson(): JSONObject`
Converts a Waypoint instance into a JSON object for serialization. This method creates a JSON object containing the waypoint's latitude, longitude, and timestamp values, enabling the waypoint to be saved to persistent storage in a structured format.

#### `fromJson(json: JSONObject): Waypoint`
Static factory method that reconstructs a Waypoint object from a JSON object loaded from storage. This method extracts the latitude, longitude, and timestamp values from the JSON object and creates a new Waypoint instance, enabling waypoints to be restored from saved data.

### WaypointRepository.kt

#### `saveWaypoints(waypoints: List<Waypoint>)`
Persists a list of waypoints to internal storage as a JSON file. This method immediately writes the waypoints to a file named "waypoints.json" in the application's internal storage directory. It converts each waypoint to JSON format and stores them in a JSON array. The method handles file I/O operations and catches any exceptions that may occur during the write process.

#### `loadWaypoints(): List<Waypoint>`
Loads all previously saved waypoints from internal storage. This method reads the "waypoints.json" file, parses the JSON content, and reconstructs a list of Waypoint objects. If the file does not exist or parsing fails, it returns an empty list. This method is called during application startup to restore waypoints from previous sessions.

#### `clearWaypoints()`
Removes all saved waypoints by deleting the storage file. This method deletes the "waypoints.json" file from internal storage, effectively clearing all persisted waypoint data. It is called when the user confirms they want to clear all waypoints through the UI.

### LocationService.kt

#### `hasLocationPermission(): Boolean`
Checks whether the application has been granted fine location permission. This method uses the Android permission system to verify that ACCESS_FINE_LOCATION permission has been granted, which is required for GPS tracking functionality.

#### `startTracking(onLocationUpdate: (Location) -> Unit)`
Initiates GPS location tracking with updates every 5 seconds. This method registers a LocationListener with the LocationManager to receive location updates at the specified interval. Each time a new location is received, the provided callback function is invoked with the updated Location object. The method verifies that location permissions are granted before starting tracking.

#### `stopTracking()`
Terminates GPS location tracking by unregistering the location listener. This method removes the LocationListener from the LocationManager, stopping all location updates. It is essential for conserving battery life and should be called when tracking is no longer needed.

#### `getLastKnownLocation(): Location?`
Retrieves the most recently known device location from the GPS provider. This method returns the last cached location if available, which can be useful for displaying an initial position before the first GPS update is received. Returns null if no location is available or permissions are not granted.

### CompassViewModel.kt (CompassState)

#### `getDistanceToSelectedWaypoint(): Float?`
Calculates the straight-line distance in meters from the user's current location to the currently selected waypoint. This method uses the Android Location class's distanceTo method to compute the distance between two geographic coordinates. Returns null if no location or waypoint is available.

#### `getBearingToSelectedWaypoint(): Float?`
Computes the compass bearing in degrees from the current location toward the selected waypoint. The bearing represents the direction the user should travel to reach the waypoint, with 0 degrees representing north. This method uses the Location class's bearingTo method for accurate calculation. Returns null if location or waypoint data is unavailable.

#### `getWaypointsInRange(): List<Waypoint>`
Filters the waypoint list to include only those within the current scale range of the compass view. This method calculates the distance to each waypoint and returns only those that are within the current scaleMeters distance from the user's location. This is used to display only relevant waypoints on the compass canvas.

#### `selectPreviousWaypoint()`
Automatically selects the previous waypoint in the list when the user reaches the current waypoint. This method decrements the selectedWaypointIndex if it is greater than zero, enabling automatic progression through waypoints during navigation. It is called automatically when the user gets within 10 meters of the current waypoint.

### SharedComposables.kt

#### `MainScreen(compassState, locationService, waypointRepository)`
The primary UI composable that orchestrates all screen elements. This function sets up sensor listeners for device orientation, manages GPS tracking lifecycle, and displays the complete user interface including the compass view, control buttons, waypoint selection UI, and information displays. It handles the integration between sensor data, location services, and user interactions.

#### `CompassView(compassState, modifier)`
Custom composable that renders the interactive compass display using a Canvas. This function is the core visualization component of the application. It creates a custom drawing surface where the compass and waypoints are rendered. The view is designed to be square, ensuring a correct aspect ratio for the compass.

Inside the canvas, the drawing operations are performed relative to the center of the view. The function first calculates the dimensions and center point of the canvas. It then saves the current canvas state and applies a rotation transformation based on the device's current compass heading. This ensures that the compass always points North relative to the user's orientation.

The function draws the compass background and the four cardinal directions (N, S, E, W). The 'N' label is drawn in red to clearly indicate North, while the other directions are drawn in black. This visual distinction helps users quickly orient themselves.

Crucially, the function iterates through the list of waypoints and draws them as colored circles on the canvas. The position of each waypoint is calculated based on its distance and bearing from the user's current location, scaled according to the current zoom level. Waypoints that are currently selected are highlighted with a distinct red color and a yellow border, providing visual feedback to the user.

A green navigation arrow is also drawn, pointing from the center of the compass towards the currently selected waypoint. This arrow guides the user in the correct direction. The function also handles touch input, detecting tap gestures to select waypoints and pinch gestures to zoom the view in and out. This interactivity is essential for a smooth user experience.

## Data Flow

The application follows a unidirectional data flow pattern:

1. User interactions trigger state changes in CompassState
2. State changes cause UI recomposition in Compose
3. Location updates from GPS are processed and update CompassState
4. Sensor data from the rotation vector sensor updates compass rotation
5. Waypoint operations (add, select, clear) update both state and persistent storage
6. The compass view renders based on current state, showing waypoints, navigation arrows, and compass directions

## Key Features Implementation

### GPS Tracking
The application uses Android's LocationManager to receive GPS updates every 5 seconds. The tracking is started and stopped based on user interaction with the tracking button, and location updates are immediately reflected in the UI and used for distance and bearing calculations.

### Waypoint Storage
Waypoints are stored as JSON in the application's internal storage directory. Each waypoint includes latitude, longitude, and timestamp. The storage is updated immediately when a new waypoint is created, ensuring data persistence across application restarts.

### Compass Display
The compass uses the device's rotation vector sensor to determine orientation. The sensor data is processed to calculate the device's azimuth (direction the device is facing), which is then used to rotate the compass display so that North always points in the correct direction relative to the user's orientation.

### Touch Interactions
The compass view supports two types of touch interactions: tap gestures for selecting waypoints by touching their circles on the canvas, and pinch gestures for adjusting the scale between 500 meters and 2 kilometers. Touch coordinates are transformed to account for compass rotation to ensure accurate waypoint selection.

### Auto-Navigation
The application features an intelligent auto-navigation system designed to simplify the user's journey. When the user gets within 10 meters of the currently selected waypoint, the application automatically selects the previous waypoint in the list. This logic is implemented within the location update listener. It continuously checks the distance between the user's current location and the selected waypoint. If this distance falls below the 10-meter threshold, and there is a previous waypoint available (i.e., the current waypoint is not the first one), the system updates the selection to the preceding waypoint. This enables a seamless navigation experience, allowing the user to retrace their steps back to the starting point without needing to manually interact with the device at each waypoint. This "hands-free" progression is particularly useful during hiking or orienteering activities where the user's hands might be occupied.

## Conclusion

This GPS waypointing application provides a complete solution for hiking and orienteering navigation. All methods are designed with clear responsibilities, proper error handling, and efficient resource management. The codebase follows Android best practices and uses only standard SDK components, ensuring compatibility and maintainability.

