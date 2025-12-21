# GPS Waypointing Application - Technical Report

## Overview
The GPS waypoint navigation features in this program were created specifically for the purpose of allowing Hikers/Orienteers to navigate through different locations where paper maps are either not available or non-existent while out in the field. Waypoints are created at any location, and the user will always have a reliable way to find their way back via the app's compass functionality, which is always updating to provide real-time information.

## System Architecture

### Core Components
**MainActivity.kt**: The initial activity (`MainActivity.kt`) of the app serves as the initial point for the app's lifecycle and is the primary component for the Activity Result API (`ActivityResultContracts`) managing location permissions, setting up Compose, and coordinating between the sensor management system, GPS tracking system, and application state.

**SharedComposables.kt**: The `SharedComposables.kt` file contains the `WaypointApp` composable that represents the main user interface and full layout of the application. It displays the compass, status information, and control buttons. The `CompassView` composable takes care of rendering custom graphics in Canvas and responding to user gestures.

**WaypointData.kt**: `WaypointData.kt` defines the waypoint data class, which includes latitude and longitude coordinates of a GPS location along with the time and date (a timestamp) and a unique identifier (ID). `WaypointData` provides the ability to serialize (`toFileString`) or deserialize (`fromFileString`) waypoint data to/from CSV format.

**WaypointFileManager.kt**: `WaypointFileManager.kt` is used to manage storing waypoints permanently in the internal file storage area of the device. `WaypointFileManager` provides users with the ability to add, save, load, and clear waypoints from the `waypoints.txt` file located in the device's secure directory.

### Key Functionality

#### Sensor Management
A `SensorEventListener` captures device orientation using the `TYPE_ROTATION_VECTOR` sensor, which provides rotation data for creating a 4×4 Rotation matrix using `getRotationMatrixFromVector()` and `getOrientation()` calculates the azimuth, pitch, and roll values. The azimuth value corresponds to the heading in degrees (360°) obtained by converting the azimuth value from Radians on the compass and is computed using `SensorDelayUi` for smooth visual effects.

The GPS location tracking system utilizes the `LocationManager` with `GPS_PROVIDER` for receiving high-accuracy location information and requesting updates every five seconds with a minimum distance of zero meters. The `LocationListener` keeps track of where a user is currently located and will send the most recent location value to update the user's current position. The user interacts with the user interface controls to Start/Stop tracking.

#### Navigation Calculations
**Bearing**: To calculate Bearing, the app determines the direction between your current location and the waypoint. The application utilizes the Android `Location.distanceBetween` method, which performs high-accuracy calculations (typically using the WGS84 ellipsoid model) to determine the bearing in degrees (0 - 360 degrees) representing a compass direction.

**Distance**: Distance is calculated using the Android `Location.distanceBetween` method, which computes the accurate geodesic distance between two GPS coordinates (latitude and longitude). This method accounts for the earth's ellipsoid shape, providing a suitable method for identifying distance for navigation.

**Auto-Navigation**: The application has the ability to automatically navigate by selecting the last waypoint in the waypoints list when the user is approximately ten metres from a selected waypoint. Once the automatic navigation feature is activated by the user, they will be able to use the application to navigate back through their previously travelled path and to any chosen previous waypoint without having to do so manually.

### User Interface Components

**Compass Display**: Compass displays in full-screen (1:1 ratio) using `aspectRatio(1f)`. The four cardinal compass point directions are represented by a bold, highlighted "N" on the compass face in red, while the remaining cardinal direction letters on the compass face are represented in black/white. As the heading of the mobile device changes, the functional compass face rotates to correctly indicate North relative to the device's orientation. The distance scale is indicated by scale rings at the distances of 25%, 50%, and 75% of the maximum range.

**Waypoint Visualization**: All current waypoints are represented as colour-coded circles within the current scaling range. All waypoints not selected appear as blue circles and all selected waypoints appear as green circles, and display a stroke ring around the selected waypoint circle. The location of each circle is determined by the distance ratio of each waypoint relative to the radius of the compass as well as by the angle of each waypoint relative to the angle that the modern mobile device is pointing (bearing angle).

**Gesture Handling**: All selections of a waypoint are accomplished via touch detection (tap gesture) using the `detectTapGestures` function. The coordinates of touch are checked against the locations of the waypoint circle positions, allowing for a tolerance of up to 50 pixels. The scaling of the compass is via a pinch gesture, which is accomplished via `detectTransformGestures` function. The scaling of the compass will be from 500 metres to 2000 metres.

**Dropdown Menu**: A selected waypoint can be manually selected with the use of a Button which triggers a `DropdownMenu`. Each waypoint will contain a respective timestamp to identify it. A selected waypoint will update the distance counter and navigation arrow.

### File Operations
The waypoint persistence is managed through a CSV file format made up of lines of data structured with an ID, latitude, longitude, and timestamp for each row. When the `loadWaypoints` method is called, it checks if a waypoints file exists. If so, it loads each line of text and converts it into waypoint objects through the `fromFileString` method.

**State Management**: The app uses Compose's state management system, leveraging `remember`, `mutableStateOf`, and `ViewModel` state flows to develop a reactive user interface. The app also employs `DisposableEffect` (via `callbackFlow` awaitClose) to automatically clean up any associated resources when sensors or location listeners are not being utilized. Finally, `LaunchedEffect` is utilized throughout the app to manage side effects including subscription to location updates.

### Permission Handling
The `RequestMultiplePermissions` contract utilizes `registerForActivityResult()` to request access to location permissions (fine and coarse). The permission will not be checked until after the app registers with the system and gains permission from the user. The app checks the availability of these permissions before performing GPS operations.

## Method Reference

### MainActivity.kt
*   **onCreate(savedInstanceState: Bundle?)**: Initializes the activity, requests location permissions, and sets up the Jetpack Compose user interface with the `GPSWaypointTheme`.

### SharedComposables.kt
*   **WaypointApp(viewModel, gpsManager, compassSensor)**: The main Composable function that structures the app's UI, including the top bar, bottom bar, and the central content area. It manages high-level state (tracking, dialogs) and observes data from the ViewModel and sensors.
*   **CompassView(heading, ...)**: Renders the custom compass UI using a Canvas. It draws the compass rose, waypoints, navigation indicators, and handles touch gestures for selection and zooming.

### AppViewModel.kt
*   **addWaypoint(location: Location)**: Creates a new `WaypointData` object from the given location and adds it to the list, triggering a save to storage.
*   **deleteWaypoints()**: Clears all waypoints from the list and persistent storage.
*   **selectWaypoint(index: Int)**: Sets the currently selected waypoint index for navigation and info display.
*   **updateLocation(location: Location)**: Updates the current user location state and checks for auto-navigation triggers.
*   **updateRange(newRange: Float)**: Updates the compass display range (scale), ensuring it stays within the 500m-2000m limits.
*   **checkAutoNavigation(location: Location)**: (Private) Logic to automatically switch to the previous waypoint if the user comes within 10 meters of the current target.

### WaypointData.kt
*   **toFileString(): String**: Serializes the waypoint object into a CSV-formatted string.
*   **fromFileString(str: String): WaypointData?**: (Companion) Parses a CSV string to create a `WaypointData` object.

### WaypointFileManager.kt
*   **saveWaypoints(waypoints: List<WaypointData>)**: Writes the list of waypoints to the internal `waypoints.txt` file.
*   **loadWaypoints(): MutableList<WaypointData>**: Reads and parses waypoints from the file storage.
*   **clearWaypoints()**: Deletes the persistent storage file.

### GPSManager.kt
*   **getLocationUpdates(): Flow<Location>**: Returns a Flow that emits location updates from the GPS provider every 5 seconds.
*   **hasLocationPermission(context: Context): Boolean**: Checks if the required location permissions are granted.

### CompassSensor.kt
*   **getCompassOrientation(): Flow<Float>**: Returns a Flow that emits the device's compass heading (azimuth) derived from the Rotation Vector sensor.

### GeoUtils.kt
*   **calculateDistance(loc: Location, lat: Double, lon: Double): Float**: Calculates the distance in meters between the current location and a target coordinate.
*   **calculateBearing(loc: Location, lat: Double, lon: Double): Float**: Computes the bearing in degrees from the current location to a target coordinate.
