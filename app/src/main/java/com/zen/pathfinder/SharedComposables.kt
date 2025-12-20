package com.zen.pathfinder

import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Golden Color Palette
object GoldenColors {
    val Primary = Color(0xFFD4AF37)        // Classic Gold
    val PrimaryDark = Color(0xFFB8860B)    // Dark Goldenrod
    val Accent = Color(0xFFFFD700)         // Gold
    val Background = Color(0xFFFFFBF0)     // Cream White
    val Surface = Color(0xFFFFF8DC)        // Cornsilk
    val OnPrimary = Color(0xFF1A1A1A)      // Near Black
    val OnBackground = Color(0xFF3D3D3D)   // Dark Gray
    val CompassRing = Color(0xFFB8860B)    // Dark Goldenrod
    val North = Color(0xFFCC0000)          // Red for North
    val CardinalText = Color(0xFF8B4513)   // Saddle Brown
    val WaypointDefault = Color(0xFF4169E1) // Royal Blue
    val WaypointSelected = Color(0xFF228B22) // Forest Green
    val ScaleRing = Color(0xFFDAA520)      // Goldenrod
}

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
        containerColor = GoldenColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zen Pathfinder", color = GoldenColors.OnPrimary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GoldenColors.Primary
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = GoldenColors.Surface) {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { isTracking = !isTracking },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenColors.Primary)
                ) {
                    Text(if (isTracking) "Stop Tracking" else "Start Tracking", color = GoldenColors.OnPrimary)
                }
                Spacer(Modifier.width(16.dp))
                if (isTracking) {
                    val isLocationReady = currentLocation != null
                    Button(
                        onClick = { currentLocation?.let { viewModel.addWaypoint(it) } },
                        enabled = isLocationReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocationReady) GoldenColors.PrimaryDark else Color.Gray
                        )
                    ) {
                        Text(if (isLocationReady) "Add Waypoint" else "Waiting for GPS...", color = Color.White)
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
                .background(GoldenColors.Background)
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
                
                // Display current range
                Text(
                    text = "Range: ${compassRange.toInt()}m",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = GoldenColors.OnBackground,
                    fontWeight = FontWeight.Bold
                )

                if (currentLocation == null && isTracking) {
                     Text(
                        text = "Waiting for GPS location...",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(GoldenColors.Surface.copy(alpha = 0.8f))
                            .padding(8.dp),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Info / Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenColors.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Waypoints: ${waypoints.size}", color = GoldenColors.OnBackground)
                    
                    // Dropdown for Waypoint Selection
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenColors.Primary)
                        ) {
                            Text("Select Waypoint", color = GoldenColors.OnPrimary)
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

                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenColors.PrimaryDark)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                }
                
                if (selectedIndex != null && selectedIndex!! in waypoints.indices) {
                    val waypoint = waypoints[selectedIndex!!]
                    val dist = currentLocation?.let { GeoUtils.calculateDistance(it, waypoint.latitude, waypoint.longitude) } ?: 0f
                    val bearing = currentLocation?.let { GeoUtils.calculateBearing(it, waypoint.latitude, waypoint.longitude) } ?: 0f
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldenColors.Primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Selected: Waypoint ${selectedIndex!! + 1}", fontWeight = FontWeight.Bold, color = GoldenColors.OnBackground)
                            Text("Distance: ${dist.toInt()} m", color = GoldenColors.OnBackground)
                            Text("Bearing: ${bearing.toInt()}°", color = GoldenColors.OnBackground)
                            Text("Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(waypoint.timestamp))}", color = GoldenColors.OnBackground)
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = GoldenColors.Surface,
            title = { Text("Confirm Clear", color = GoldenColors.OnBackground) },
            text = { Text("Are you sure you want to delete all waypoints?", color = GoldenColors.OnBackground) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWaypoints()
                    showClearDialog = false
                }) {
                    Text("Delete", color = GoldenColors.PrimaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = GoldenColors.OnBackground)
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
    // Use State to hold a mutable range that can be updated by gestures
    var currentRangeState by remember { mutableStateOf(range) }
    
    // Sync external range changes
    LaunchedEffect(range) {
        currentRangeState = range
    }
    
    val currentHeading by rememberUpdatedState(heading)
    val currentWaypoints by rememberUpdatedState(waypoints)
    val currentLocationState by rememberUpdatedState(currentLocation)
    val onSelect by rememberUpdatedState(onWaypointSelect)
    val onChange by rememberUpdatedState(onRangeChange)

    // Transformable state for pinch-to-zoom
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        // Standard zoom logic: zoomChange > 1 means zoom in (reduce range)
        // zoomChange < 1 means zoom out (increase range)
        val newRange = (currentRangeState / zoomChange).coerceIn(500f, 2000f)
        currentRangeState = newRange
        onChange(newRange)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            // Tap gestures should be handled first
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                     val center = Offset(size.width / 2f, size.height / 2f)
                     val maxRadius = kotlin.math.min(size.width, size.height) / 2f
                     
                     val loc = currentLocationState
                     if (loc != null) {
                        currentWaypoints.forEachIndexed { index, wp ->
                            val distance = GeoUtils.calculateDistance(loc, wp.latitude, wp.longitude)
                            if (distance <= currentRangeState) {
                                val bearing = GeoUtils.calculateBearing(loc, wp.latitude, wp.longitude)
                                val angleRad = Math.toRadians((bearing - currentHeading).toDouble())
                                val r = (distance / currentRangeState) * maxRadius
                                
                                val x = center.x + (r * sin(angleRad)).toFloat()
                                val y = center.y - (r * cos(angleRad)).toFloat()
                                
                                val dx = tapOffset.x - x
                                val dy = tapOffset.y - y
                                if (dx*dx + dy*dy < 50 * 50) {
                                    onSelect(index)
                                }
                            }
                        }
                     }
                }
            }
            // Zoom gesture
            .transformable(state = transformableState)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = center
            val maxRadius = size.minDimension / 2
            
            // Draw background circle with golden theme
            drawCircle(
                color = GoldenColors.CompassRing,
                radius = maxRadius,
                style = Stroke(width = 4f)
            )
            
            // Scale Rings (25%, 50%, 75%)
            listOf(0.25f, 0.50f, 0.75f).forEach { fraction ->
                drawCircle(
                    color = GoldenColors.ScaleRing.copy(alpha = 0.4f),
                    radius = maxRadius * fraction,
                    style = Stroke(width = 2f)
                )
            }
            
            // Rotate for cardinal directions and waypoints
            rotate(-currentHeading, center) {
                // Draw Cardinals
                val textPaint = Paint().asFrameworkPaint().apply {
                    textSize = 40f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                
                // N - Highlighted in Red
                textPaint.color = GoldenColors.North.toArgb()
                drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - maxRadius + 50, textPaint)
                
                // S, E, W in Cardinal color
                textPaint.color = GoldenColors.CardinalText.toArgb()
                drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + maxRadius - 20, textPaint)
                drawContext.canvas.nativeCanvas.drawText("E", center.x + maxRadius - 30, center.y + 10, textPaint)
                drawContext.canvas.nativeCanvas.drawText("W", center.x - maxRadius + 30, center.y + 10, textPaint)

                if (currentLocationState != null) {
                    val loc = currentLocationState!!
                    currentWaypoints.forEachIndexed { index, wp ->
                        val distance = GeoUtils.calculateDistance(loc, wp.latitude, wp.longitude)
                        if (distance <= currentRangeState) {
                            val bearing = GeoUtils.calculateBearing(loc, wp.latitude, wp.longitude)
                            val r = (distance / currentRangeState) * maxRadius
                            val angleRad = Math.toRadians(bearing.toDouble()) 
                            
                            val x = center.x + (r * sin(angleRad)).toFloat()
                            val y = center.y - (r * cos(angleRad)).toFloat()
                            
                            val isSelected = index == selectedWaypointIndex
                            val color = if (isSelected) GoldenColors.WaypointSelected else GoldenColors.WaypointDefault
                            
                            drawCircle(
                                color = color,
                                radius = 15f,
                                center = Offset(x, y)
                            )
                            
                            if (isSelected) {
                                drawCircle(
                                    color = GoldenColors.WaypointSelected,
                                    radius = 25f,
                                    style = Stroke(width = 3f),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
