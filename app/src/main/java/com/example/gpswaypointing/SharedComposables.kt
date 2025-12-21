package com.example.gpswaypointing

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpswaypointing.ui.theme.*
import kotlin.math.*

/**
 * Main screen composable that contains all UI elements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    compassState: CompassState,
    locationService: LocationService,
    waypointRepository: WaypointRepository
) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    }

    // Setup rotation vector sensor listener
    DisposableEffect(Unit) {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        val orientationValues = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        compassState.compassRotation = -azimuthDegrees
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationVectorSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Setup GPS tracking
    LaunchedEffect(compassState.isTracking) {
        if (compassState.isTracking) {
            locationService.startTracking { location ->
                compassState.currentLocation = location
                compassState.selectedWaypointIndex?.let { currentIndex ->
                    val distance = compassState.getDistanceToSelectedWaypoint()
                    if (distance != null && distance <= 10f && currentIndex > 0) {
                        compassState.selectPreviousWaypoint()
                    }
                }
            }
        } else {
            locationService.stopTracking()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Waypoint Navigator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Compass / Map View
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CompassView(
                        compassState = compassState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

            // Data Info Panel
            if (compassState.selectedWaypoint != null) {
                val distance = compassState.getDistanceToSelectedWaypoint()
                val bearing = compassState.getBearingToSelectedWaypoint()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoCard(label = "Distance", value = "${distance?.toInt() ?: 0} m")
                    InfoCard(label = "Bearing", value = "${bearing?.toInt() ?: 0}°")
                }
            }

            // Waypoint Selection
            if (compassState.waypoints.isNotEmpty()) {
                WaypointSelectionList(
                    waypoints = compassState.waypoints,
                    selectedIndex = compassState.selectedWaypointIndex,
                    onSelect = { index -> compassState.selectedWaypointIndex = index },
                    onClear = { compassState.showClearDialog = true }
                )
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { compassState.isTracking = !compassState.isTracking },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (compassState.isTracking) ErrorRed else PrimaryBlue
                    )
                ) {
                    Icon(
                        imageVector = if (compassState.isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (compassState.isTracking) "Stop Tracking" else "Start Tracking")
                }

                if (compassState.isTracking) {
                    Button(
                        onClick = {
                            compassState.currentLocation?.let { location ->
                                val newWaypoint = Waypoint(
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                                val updatedWaypoints = compassState.waypoints + newWaypoint
                                compassState.waypoints = updatedWaypoints
                                waypointRepository.saveWaypoints(updatedWaypoints)
                            }
                        },
                        enabled = compassState.currentLocation != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Waypoint")
                    }
                }
            }
        }

        if (compassState.showClearDialog) {
            AlertDialog(
                onDismissRequest = { compassState.showClearDialog = false },
                title = { Text("Clear All Waypoints") },
                text = { Text("Are you sure you want to delete all saved waypoints? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            compassState.waypoints = emptyList()
                            compassState.selectedWaypointIndex = null
                            waypointRepository.clearWaypoints()
                            compassState.showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { compassState.showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun InfoCard(label: String, value: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CompassView(
    compassState: CompassState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    handleCompassTap(tapOffset, compassState, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(compassState.scaleMeters) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newScale = (compassState.scaleMeters / zoom).coerceIn(500f, 2000f)
                    compassState.scaleMeters = newScale
                }
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(size.width, size.height) / 2
        val compassRadius = radius * 0.9f

        // Draw Compass Circle
        drawCircle(
            color = Color.LightGray,
            radius = compassRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Scale Text inside compass
        val scaleText = "Scale: ${compassState.scaleMeters.toInt()}m"
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 30f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            scaleText,
            size.width - 20f,
            size.height - 20f,
            textPaint
        )

        // Rotate for Compass Heading
        rotate(
            degrees = compassState.compassRotation,
            pivot = Offset(centerX, centerY)
        ) {
            // Draw Cardinal Directions
            val directions = listOf(
                "N" to Color.Red,
                "E" to Color.Black,
                "S" to Color.Black,
                "W" to Color.Black
            )
            val dirPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 20.sp.toPx()
                isFakeBoldText = true
            }

            directions.forEachIndexed { index, (label, color) ->
                val angle = index * 90f
                val angleRad = Math.toRadians(angle.toDouble())
                val textX = centerX + (compassRadius * 0.85f * sin(angleRad)).toFloat()
                val textY = centerY - (compassRadius * 0.85f * cos(angleRad)).toFloat()
                
                // Adjust Y for text baseline roughly
                val baselineY = textY + (dirPaint.textSize / 3)

                dirPaint.color = color.toArgb()
                drawContext.canvas.nativeCanvas.drawText(label, textX, baselineY, dirPaint)
            }

            // Draw Waypoints
            compassState.currentLocation?.let { location ->
                compassState.getWaypointsInRange().forEach { waypoint ->
                    val waypointLocation = Location("").apply {
                        latitude = waypoint.latitude
                        longitude = waypoint.longitude
                    }
                    val distance = location.distanceTo(waypointLocation)
                    val bearing = location.bearingTo(waypointLocation)
                    
                    val bearingRad = Math.toRadians(bearing.toDouble())
                    val relativeDistance = distance / compassState.scaleMeters
                    val waypointRadius = relativeDistance * compassRadius
                    
                    val waypointX = centerX + (waypointRadius * sin(bearingRad)).toFloat()
                    val waypointY = centerY - (waypointRadius * cos(bearingRad)).toFloat()

                    val isSelected = compassState.selectedWaypoint == waypoint
                    
                    // Draw selection highlight ring
                    if (isSelected) {
                        drawCircle(
                            color = SecondaryOrange,
                            radius = 12.dp.toPx(),
                            center = Offset(waypointX, waypointY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // Draw navigation line
                        drawLine(
                            color = SecondaryOrange.copy(alpha = 0.5f),
                            start = Offset(centerX, centerY),
                            end = Offset(waypointX, waypointY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Draw Waypoint Dot
                    drawCircle(
                        color = if (isSelected) SecondaryOrange else PrimaryBlue,
                        radius = 6.dp.toPx(),
                        center = Offset(waypointX, waypointY)
                    )
                }
            }
        }

        // Draw User Position (Center)
        drawCircle(
            color = PrimaryBlue,
            radius = 8.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

private fun handleCompassTap(
    tapOffset: Offset,
    compassState: CompassState,
    width: Float,
    height: Float
) {
    val centerX = width / 2
    val centerY = height / 2
    val minDimension = min(width, height) / 2
    val compassRadius = minDimension * 0.9f 

    compassState.currentLocation?.let { location ->
        compassState.getWaypointsInRange().forEach { waypoint ->
            val waypointLocation = Location("").apply {
                latitude = waypoint.latitude
                longitude = waypoint.longitude
            }
            val distance = location.distanceTo(waypointLocation)
            val bearing = location.bearingTo(waypointLocation)

            val bearingRad = Math.toRadians(bearing.toDouble())
            val waypointDist = (distance / compassState.scaleMeters) * compassRadius
            
            // Project relative to North
            val waypointX_North = centerX + (waypointDist * sin(bearingRad)).toFloat()
            val waypointY_North = centerY - (waypointDist * cos(bearingRad)).toFloat()
            
            // Rotate by compass heading
            val rotationRad = Math.toRadians(compassState.compassRotation.toDouble())
            val dx = waypointX_North - centerX
            val dy = waypointY_North - centerY
            
            val screenX = centerX + (dx * cos(rotationRad) - dy * sin(rotationRad)).toFloat()
            val screenY = centerY + (dx * sin(rotationRad) + dy * cos(rotationRad)).toFloat()
            
            val touchDist = sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
            
            // Hit target radius
            if (touchDist < 50f) { 
                val actualIndex = compassState.waypoints.indexOf(waypoint)
                if (actualIndex >= 0) {
                    compassState.selectedWaypointIndex = actualIndex
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointSelectionList(
    waypoints: List<Waypoint>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved Waypoints (${waypoints.size})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) {
                Text("Clear All", color = ErrorRed)
            }
        }
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(waypoints.size) { index ->
                val isSelected = selectedIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    label = { Text("Point ${index + 1}") },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryOrange,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}
