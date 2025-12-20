package com.zen.pathfinder

import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointApp(
    viewModel: AppViewModel,
    gpsManager: GPSManager,
    compassSensor: CompassSensor
) {
    // State collection
    val waypoints by viewModel.waypoints
    val currentLocation by viewModel.currentLocation
    val compassRange by viewModel.compassRange
    val selectedIndex by viewModel.selectedWaypointIndex
    
    val orientation by compassSensor.getCompassOrientation().collectAsState(initial = 0f)
    var isTracking by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // GPS Tracking effect
    LaunchedEffect(isTracking) {
        if (isTracking) {
            gpsManager.getLocationUpdates().collect { loc ->
                viewModel.updateLocation(loc)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GPS Waypoint") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(Modifier.weight(1f))
                Button(onClick = { isTracking = !isTracking }) {
                    Text(if (isTracking) "Stop Tracking" else "Start Tracking")
                }
                Spacer(Modifier.width(16.dp))
                if (isTracking) {
                    Button(onClick = { currentLocation?.let { viewModel.addWaypoint(it) } }) {
                        Text("Add Waypoint")
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Compass View (Custom Composable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassView(
                    heading = orientation,
                    waypoints = waypoints,
                    currentLocation = currentLocation,
                    range = compassRange,
                    selectedWaypointIndex = selectedIndex,
                    onWaypointSelect = { viewModel.selectWaypoint(it) },
                    onRangeChange = { viewModel.updateRange(it) }
                )
            }
            
            // Info / Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Waypoints: ${waypoints.size}")
                    
                    // Dropdown for Waypoint Selection
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        Button(onClick = { expanded = true }) {
                            Text("Select Waypoint")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    viewModel.selectWaypoint(-1)
                                    expanded = false
                                }
                            )
                            waypoints.forEachIndexed { index, wp ->
                                DropdownMenuItem(
                                    text = { Text("Waypoint ${index + 1} (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(wp.timestamp))})") },
                                    onClick = { 
                                        viewModel.selectWaypoint(index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(onClick = { showClearDialog = true }) {
                        Text("Clear All")
                    }
                }
                
                if (selectedIndex != null && selectedIndex!! in waypoints.indices) {

                    val waypoint = waypoints[selectedIndex!!]
                    val dist = currentLocation?.let { GeoUtils.calculateDistance(it, waypoint.latitude, waypoint.longitude) } ?: 0f
                    val bearing = currentLocation?.let { GeoUtils.calculateBearing(it, waypoint.latitude, waypoint.longitude) } ?: 0f
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Selected: Waypoint ${selectedIndex!! + 1}", fontWeight = FontWeight.Bold)
                            Text("Distance: ${dist.toInt()} m")
                            Text("Bearing: ${bearing.toInt()}°")
                            Text("Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(waypoint.timestamp))}")
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm Clear") },
            text = { Text("Are you sure you want to delete all waypoints?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWaypoints()
                    showClearDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CompassView(
    heading: Float,
    waypoints: List<WaypointData>,
    currentLocation: Location?,
    range: Float,
    selectedWaypointIndex: Int?,
    onWaypointSelect: (Int) -> Unit,
    onRangeChange: (Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = kotlin.math.min(size.width, size.height) / 2f
                    
                    if (currentLocation == null) return@detectTapGestures
                    
                    // Check taps on waypoints
                    // We need to simulate the drawing logic to find hit targets
                    waypoints.forEachIndexed { index, wp ->
                        val distance = GeoUtils.calculateDistance(currentLocation, wp.latitude, wp.longitude)
                        if (distance <= range) {
                            val bearing = GeoUtils.calculateBearing(currentLocation, wp.latitude, wp.longitude)
                            // Angle on screen: Bearing - Heading
                            // 0 is UP. 
                            val angleRad = Math.toRadians((bearing - heading).toDouble())
                            val r = (distance / range) * maxRadius
                            
                            val x = center.x + (r * sin(angleRad)).toFloat()
                            val y = center.y - (r * cos(angleRad)).toFloat()
                            
                            // Hit test (approx 50px radius)
                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            if (dx*dx + dy*dy < 50 * 50) { // 50px tolerance
                                onWaypointSelect(index)
                                return@detectTapGestures
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onRangeChange(range / zoom)
                }
            }
    ) {
        val center = center
        val maxRadius = size.minDimension / 2
        
        // Draw Compass Rose (N/S/E/W)
        // We rotate the canvas opposite to heading so North is at 'Heading' degrees?
        // Wait, earlier logic: "East ... point to top".
        // `heading` is Azimuth.
        // We rotate by -heading.
        
        // Draw background circle
        drawCircle(
            color = Color.LightGray,
            radius = maxRadius,
            style = Stroke(width = 4f)
        )
        // Scale Rings (25%, 50%, 75%)
        listOf(0.25f, 0.50f, 0.75f).forEach { fraction ->
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.5f),
                radius = maxRadius * fraction,
                style = Stroke(width = 2f) // Thinner lines
            )
        }
        
        // Rotate for cardinal directions and waypoints
        rotate(-heading, center) {
            // Draw Cardinals
            val textPaint = Paint().asFrameworkPaint().apply {
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                color = android.graphics.Color.BLACK
            }
            
            // N
            textPaint.color = android.graphics.Color.RED
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - maxRadius + 50, textPaint)
            
            // S
            textPaint.color = android.graphics.Color.BLACK
            drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + maxRadius - 20, textPaint)
            
            // E
            drawContext.canvas.nativeCanvas.drawText("E", center.x + maxRadius - 30, center.y + 10, textPaint)
            
            // W
            drawContext.canvas.nativeCanvas.drawText("W", center.x - maxRadius + 30, center.y + 10, textPaint)

            if (currentLocation != null) {
                waypoints.forEachIndexed { index, wp ->
                    val distance = GeoUtils.calculateDistance(currentLocation, wp.latitude, wp.longitude)
                    if (distance <= range) {
                        val bearing = GeoUtils.calculateBearing(currentLocation, wp.latitude, wp.longitude)
                        val r = (distance / range) * maxRadius
                        val angleRad = Math.toRadians(bearing.toDouble()) 
                        
                        val x = center.x + (r * sin(angleRad)).toFloat()
                        val y = center.y - (r * cos(angleRad)).toFloat()
                        
                        val isSelected = index == selectedWaypointIndex
                        val color = if (isSelected) Color.Green else Color.Blue
                        
                        drawCircle(
                            color = color,
                            radius = 15f,
                            center = Offset(x, y)
                        )
                        
                        if (isSelected) {
                            drawCircle(
                                color = Color.Green,
                                radius = 25f,
                                style = Stroke(width = 2f),
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }
        
        // Draw Heading Marker (Triangle at top?) - Optional but good.
        // "North highlighted" - we did that.
    }
}
