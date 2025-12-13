package com.example.gpswaypointing

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Main screen composable that contains all UI elements.
 */
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
    LaunchedEffect(Unit) {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        val orientationValues = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        // Convert azimuth (radians) to degrees and adjust for compass
                        val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        compassState.compassRotation = -azimuthDegrees // Negative to rotate compass correctly
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationVectorSensor?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        // Cleanup on dispose
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Setup GPS tracking
    LaunchedEffect(compassState.isTracking) {
        if (compassState.isTracking) {
            locationService.startTracking { location ->
                compassState.currentLocation = location
                
                // Check if within 10m of selected waypoint and auto-select previous
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compass View
        CompassView(
            compassState = compassState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Force square
        )

        // Distance and bearing display
        if (compassState.selectedWaypoint != null) {
            val distance = compassState.getDistanceToSelectedWaypoint()
            val bearing = compassState.getBearingToSelectedWaypoint()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Distance: ${distance?.toInt() ?: 0}m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (bearing != null) {
                        Text(
                            text = "Bearing: ${bearing.toInt()}°",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    compassState.isTracking = !compassState.isTracking
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (compassState.isTracking) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
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
                    modifier = Modifier.weight(1f),
                    enabled = compassState.currentLocation != null
                ) {
                    Text("Add Waypoint")
                }
            }
        }

        // Waypoint selection
        if (compassState.waypoints.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Waypoint:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        compassState.waypoints.forEachIndexed { index, waypoint ->
                            FilterChip(
                                selected = compassState.selectedWaypointIndex == index,
                                onClick = {
                                    compassState.selectedWaypointIndex = index
                                },
                                label = { Text("WP${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            // Show confirmation dialog handled by state
                            compassState.showClearDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear Waypoints")
                    }
                }
            }
        }

        // Clear confirmation dialog
        if (compassState.showClearDialog) {
            AlertDialog(
                onDismissRequest = { compassState.showClearDialog = false },
                title = { Text("Clear Waypoints") },
                text = { Text("Are you sure you want to clear all waypoints? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            compassState.waypoints = emptyList()
                            compassState.selectedWaypointIndex = null
                            waypointRepository.clearWaypoints()
                            compassState.showClearDialog = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { compassState.showClearDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Custom compass view composable that displays a compass with N/S/E/W directions,
 * waypoints as colored circles, and navigation arrow to selected waypoint.
 * The view is forced to be square and supports touch input for waypoint selection
 * and pinch-to-zoom for scale adjustment.
 */
@Composable
fun CompassView(
    compassState: CompassState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val circleRadiusPx = with(density) { 25.dp.toPx() }
    val selectedCircleRadiusPx = with(density) { 25.dp.toPx() }
    val unselectedCircleRadiusPx = with(density) { 20.dp.toPx() }
    val borderWidthPx = with(density) { 3.dp.toPx() }
    val arrowStrokeWidthPx = with(density) { 4.dp.toPx() }
    val arrowHeadSizePx = with(density) { 15.dp.toPx() }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Canvas(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Handle tap to select waypoint
                        val size = size
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val maxRadius = min(size.width, size.height) / 2

                        compassState.currentLocation?.let { location ->
                            compassState.getWaypointsInRange().forEach { waypoint ->
                                val waypointLocation = Location("").apply {
                                    latitude = waypoint.latitude
                                    longitude = waypoint.longitude
                                }
                                val distance = location.distanceTo(waypointLocation)
                                val bearing = location.bearingTo(waypointLocation)

                                // Convert to canvas coordinates (accounting for compass rotation)
                                val bearingRad = Math.toRadians(bearing.toDouble())
                                val radius = (distance / compassState.scaleMeters) * maxRadius
                                
                                // Calculate waypoint position in rotated coordinate system
                                val waypointX = centerX + (radius * sin(bearingRad)).toFloat()
                                val waypointY = centerY - (radius * cos(bearingRad)).toFloat()

                                // Rotate tap coordinates back to match waypoint coordinates
                                val angleRad = Math.toRadians(-compassState.compassRotation.toDouble())
                                val dx = tapOffset.x - centerX
                                val dy = tapOffset.y - centerY
                                val rotatedX = centerX + (dx * cos(angleRad) - dy * sin(angleRad)).toFloat()
                                val rotatedY = centerY + (dx * sin(angleRad) + dy * cos(angleRad)).toFloat()

                                // Check if tap is within waypoint circle
                                val distanceToWaypoint = sqrt(
                                    (rotatedX - waypointX).pow(2) + 
                                    (rotatedY - waypointY).pow(2)
                                )

                                if (distanceToWaypoint <= circleRadiusPx) {
                                    // Find the actual index in full waypoints list
                                    val actualIndex = compassState.waypoints.indexOf(waypoint)
                                    if (actualIndex >= 0) {
                                        compassState.selectedWaypointIndex = actualIndex
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(compassState.scaleMeters) {
                    detectTransformGestures { _, _, zoom, _ ->
                        // Handle pinch to zoom
                        val newScale = (compassState.scaleMeters / zoom).coerceIn(500f, 2000f)
                        compassState.scaleMeters = newScale
                    }
                }
        ) {
            val size = size
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = min(size.width, size.height) / 2

            // Save canvas state
            save()

            // Rotate canvas based on device orientation
            rotate(
                degrees = compassState.compassRotation,
                pivot = Offset(centerX, centerY)
            ) {
                // Draw compass background circle
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )

                // Draw compass directions
                val directions = listOf(
                    "N" to 0f,
                    "E" to 90f,
                    "S" to 180f,
                    "W" to 270f
                )

                directions.forEach { (label, angle) ->
                    val angleRad = Math.toRadians(angle.toDouble())
                    val textX = centerX + (radius * 0.85f * sin(angleRad)).toFloat()
                    val textY = centerY - (radius * 0.85f * cos(angleRad)).toFloat()

                    val color = if (label == "N") {
                        Color.Red // Highlight North
                    } else {
                        Color.Black
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        translate(textX, textY)
                        android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 32.sp.toPx()
                            this.color = color.toArgb()
                            drawText(label, 0f, 0f, this)
                        }
                        restore()
                    }
                }

                // Draw waypoints
                compassState.currentLocation?.let { location ->
                    compassState.getWaypointsInRange().forEach { waypoint ->
                        val waypointLocation = Location("").apply {
                            latitude = waypoint.latitude
                            longitude = waypoint.longitude
                        }
                        val distance = location.distanceTo(waypointLocation)
                        val bearing = location.bearingTo(waypointLocation)

                        // Convert to canvas coordinates
                        val bearingRad = Math.toRadians(bearing.toDouble())
                        val waypointRadius = (distance / compassState.scaleMeters) * radius
                        val waypointX = centerX + (waypointRadius * sin(bearingRad)).toFloat()
                        val waypointY = centerY - (waypointRadius * cos(bearingRad)).toFloat()

                        // Check if this is the selected waypoint
                        val isSelected = compassState.selectedWaypoint == waypoint
                        val circleColor = if (isSelected) Color.Red else Color.Blue
                        val circleRadius = if (isSelected) selectedCircleRadiusPx else unselectedCircleRadiusPx

                        // Draw waypoint circle
                        drawCircle(
                            color = circleColor,
                            radius = circleRadius,
                            center = Offset(waypointX, waypointY)
                        )

                        // Draw border for selected waypoint
                        if (isSelected) {
                            val borderRadiusPx = with(density) { (circleRadius + 5.dp.toPx()) }
                            drawCircle(
                                color = Color.Yellow,
                                radius = borderRadiusPx,
                                style = Stroke(width = borderWidthPx)
                            )
                        }
                    }

                    // Draw navigation arrow to selected waypoint
                    compassState.selectedWaypoint?.let { selectedWaypoint ->
                        val selectedLocation = Location("").apply {
                            latitude = selectedWaypoint.latitude
                            longitude = selectedWaypoint.longitude
                        }
                        val bearing = location.bearingTo(selectedLocation)
                        val bearingRad = Math.toRadians(bearing.toDouble())

                        // Draw arrow from center pointing to waypoint
                        val arrowLength = radius * 0.7f
                        val arrowEndX = centerX + (arrowLength * sin(bearingRad)).toFloat()
                        val arrowEndY = centerY - (arrowLength * cos(bearingRad)).toFloat()

                        // Draw arrow line
                        drawLine(
                            color = Color.Green,
                            start = Offset(centerX, centerY),
                            end = Offset(arrowEndX, arrowEndY),
                            strokeWidth = arrowStrokeWidthPx
                        )

                        // Draw arrowhead
                        val angle1 = bearingRad + Math.PI - Math.PI / 6
                        val angle2 = bearingRad + Math.PI + Math.PI / 6

                        val path = Path().apply {
                            moveTo(arrowEndX, arrowEndY)
                            lineTo(
                                arrowEndX + (arrowHeadSizePx * sin(angle1)).toFloat(),
                                arrowEndY - (arrowHeadSizePx * cos(angle1)).toFloat()
                            )
                            lineTo(
                                arrowEndX + (arrowHeadSizePx * sin(angle2)).toFloat(),
                                arrowEndY - (arrowHeadSizePx * cos(angle2)).toFloat()
                            )
                            close()
                        }
                        drawPath(path, color = Color.Green)
                    }
                }
            }

            // Restore canvas state
            restore()
        }
    }
}

