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
import androidx.compose.foundation.shape.CutCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

    // Main Container with Starfield Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SciFiBackground)
    ) {
        // Decorative Grid Background
        GridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "NEON PATHFINDER",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonCyan,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Radar HUD (Compass)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                 RadarHudView(
                    compassState = compassState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Force square
                        .padding(8.dp)
                )
            }

            // Data Info Panel (Distance/Bearing)
            if (compassState.selectedWaypoint != null) {
                val distance = compassState.getDistanceToSelectedWaypoint()
                val bearing = compassState.getBearingToSelectedWaypoint()
                
                DataHudPanel(
                    distance = distance,
                    bearing = bearing
                )
            } else {
                // Spacer to keep layout stable
                Spacer(modifier = Modifier.height(100.dp))
            }

            // Waypoint Selection Grid
            if (compassState.waypoints.isNotEmpty()) {
                WaypointSelectionGrid(
                    waypoints = compassState.waypoints,
                    selectedIndex = compassState.selectedWaypointIndex,
                    onSelect = { index -> compassState.selectedWaypointIndex = index },
                    onClear = { compassState.showClearDialog = true }
                )
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberButton(
                    text = if (compassState.isTracking) "ABORT TRACKING" else "INITIATE TRACK",
                    onClick = { compassState.isTracking = !compassState.isTracking },
                    isWarning = compassState.isTracking,
                    modifier = Modifier.weight(1f)
                )

                if (compassState.isTracking) {
                    CyberButton(
                        text = "MARK WAYPOINT",
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Clear confirmation dialog
        if (compassState.showClearDialog) {
            CyberAlertDialog(
                title = "PURGE DATA?",
                text = "CONFIRM DELETION OF ALL WAYPOINT COORDINATES. THIS ACTION IS IRREVERSIBLE.",
                onConfirm = {
                    compassState.waypoints = emptyList()
                    compassState.selectedWaypointIndex = null
                    waypointRepository.clearWaypoints()
                    compassState.showClearDialog = false
                },
                onDismiss = { compassState.showClearDialog = false }
            )
        }
    }
}

/**
 * Custom Radar HUD View (Replaces CompassView)
 */
@Composable
fun RadarHudView(
    compassState: CompassState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Animation for scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "ScanAngle"
    )

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    handleRadarTap(tapOffset, compassState, size.width.toFloat(), size.height.toFloat())
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
        val radarRadius = radius * 0.9f

        // Draw Outer Ring
        drawCircle(
            color = NeonCyan,
            radius = radarRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw Inner Rings
        drawCircle(
            color = NeonCyan.copy(alpha = 0.5f),
            radius = radarRadius * 0.66f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = NeonCyan.copy(alpha = 0.3f),
            radius = radarRadius * 0.33f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw Crosshairs
        drawLine(
            color = NeonCyan.copy(alpha = 0.3f),
            start = Offset(centerX - radarRadius, centerY),
            end = Offset(centerX + radarRadius, centerY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = NeonCyan.copy(alpha = 0.3f),
            start = Offset(centerX, centerY - radarRadius),
            end = Offset(centerX, centerY + radarRadius),
            strokeWidth = 1.dp.toPx()
        )

        // Save canvas for rotation
        drawContext.canvas.save()

        // Rotate for Compass Heading
        rotate(
            degrees = compassState.compassRotation,
            pivot = Offset(centerX, centerY)
        ) {
            // Draw Cardinal Directions (Technical Style)
            val directions = listOf(
                "N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f
            )
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 24.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }

            directions.forEach { (label, angle) ->
                val angleRad = Math.toRadians(angle.toDouble())
                val textX = centerX + (radarRadius * 1.1f * sin(angleRad)).toFloat()
                val textY = centerY - (radarRadius * 1.1f * cos(angleRad)).toFloat()

                paint.color = if (label == "N") NeonMagenta.toArgb() else NeonCyan.toArgb()
                
                drawContext.canvas.nativeCanvas.drawText(label, textX, textY + 10f, paint) // +10f for vertical centering approximation
            }
            
            // Draw Degree Ticks
            for (i in 0 until 360 step 30) {
                if (i % 90 == 0) continue // Skip cardinals
                val angleRad = Math.toRadians(i.toDouble())
                val startX = centerX + (radarRadius * 0.95f * sin(angleRad)).toFloat()
                val startY = centerY - (radarRadius * 0.95f * cos(angleRad)).toFloat()
                val endX = centerX + (radarRadius * 1.0f * sin(angleRad)).toFloat()
                val endY = centerY - (radarRadius * 1.0f * cos(angleRad)).toFloat()
                
                drawLine(
                    color = NeonCyan.copy(alpha = 0.6f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draw Waypoints (Blips)
            compassState.currentLocation?.let { location ->
                compassState.getWaypointsInRange().forEach { waypoint ->
                    val waypointLocation = Location("").apply {
                        latitude = waypoint.latitude
                        longitude = waypoint.longitude
                    }
                    val distance = location.distanceTo(waypointLocation)
                    val bearing = location.bearingTo(waypointLocation)
                    
                    val bearingRad = Math.toRadians(bearing.toDouble())
                    val scanScale = distance / compassState.scaleMeters
                    val blipDistance = scanScale * radarRadius // Map distance to radius inside radar
                    
                    val blipX = centerX + (blipDistance * sin(bearingRad)).toFloat()
                    val blipY = centerY - (blipDistance * cos(bearingRad)).toFloat()

                    // Pulse effect for selected
                    val isSelected = compassState.selectedWaypoint == waypoint
                    val blipColor = if (isSelected) NeonMagenta else NeonCyan
                    val blipRadius = if (isSelected) 8.dp.toPx() else 5.dp.toPx()

                    drawCircle(
                        color = blipColor,
                        radius = blipRadius,
                        center = Offset(blipX, blipY)
                    )
                    
                    if (isSelected) {
                        drawCircle(
                            color = blipColor,
                            radius = blipRadius * 1.5f,
                            center = Offset(blipX, blipY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // Draw navigation line to blip
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(NeonMagenta.copy(alpha = 0f), NeonMagenta),
                                start = Offset(centerX, centerY),
                                end = Offset(blipX, blipY)
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(blipX, blipY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
        
        drawContext.canvas.restore()

        // Draw Player Centroid
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        
        // Draw Scanning Sweep
        rotate(scanAngle, pivot = Offset(centerX, centerY)) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1.0f to NeonCyan.copy(alpha = 0.5f),
                    center = Offset(centerX, centerY)
                ),
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(centerX - radarRadius, centerY - radarRadius),
                size = androidx.compose.ui.geometry.Size(radarRadius * 2, radarRadius * 2)
            )
        }
    }
}

// Logic extract for handling taps (similar to previous CompassView)
private fun handleRadarTap(
    tapOffset: Offset,
    compassState: CompassState,
    width: Float,
    height: Float
) {
    val centerX = width / 2
    val centerY = height / 2
    val minDimension = min(width, height) / 2
    val radarRadius = minDimension * 0.9f 

    compassState.currentLocation?.let { location ->
        compassState.getWaypointsInRange().forEach { waypoint ->
            val waypointLocation = Location("").apply {
                latitude = waypoint.latitude
                longitude = waypoint.longitude
            }
            val distance = location.distanceTo(waypointLocation)
            val bearing = location.bearingTo(waypointLocation)

            val bearingRad = Math.toRadians(bearing.toDouble())
            val blipDistance = (distance / compassState.scaleMeters) * radarRadius
            
            // Calculate waypoint position in rotated coordinate system (screen relative)
            // But wait! We need to account for compass rotation to map screen tap to world bearing
            // Or simpler: project waypoint to screen coordinates like we do in Draw
            
            // Projection logic derived from Draw Loop:
            // 1. Waypoint relative to North
            val waypointX_North = centerX + (blipDistance * sin(bearingRad)).toFloat()
            val waypointY_North = centerY - (blipDistance * cos(bearingRad)).toFloat()
            
            // 2. Rotate this point around center by compassRotation to get Screen Coordinates
            val rotationRad = Math.toRadians(compassState.compassRotation.toDouble())
            val dx = waypointX_North - centerX
            val dy = waypointY_North - centerY
            
            val screenX = centerX + (dx * cos(rotationRad) - dy * sin(rotationRad)).toFloat()
            val screenY = centerY + (dx * sin(rotationRad) + dy * cos(rotationRad)).toFloat()
            
            // Check distance from tap
            val touchDist = sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
            
            // 30dp touch target roughly
            if (touchDist < 50f) { 
                val actualIndex = compassState.waypoints.indexOf(waypoint)
                if (actualIndex >= 0) {
                    compassState.selectedWaypointIndex = actualIndex
                }
            }
        }
    }
}

/**
 * Futuristic Button Component
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    enabled: Boolean = true
) {
    val mainColor = if (isWarning) NeonMagenta else NeonCyan
    
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
            .background(mainColor.copy(alpha = 0.1f))
            .border(1.dp, mainColor, CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) mainColor else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Data Display Panel
 */
@Composable
fun DataHudPanel(
    distance: Float?,
    bearing: Float?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan, CutCornerShape(4.dp))
            .background(DarkGlass)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "DISTANCE", style = MaterialTheme.typography.labelSmall, color = TextUnselected)
            Text(
                text = "${distance?.toInt() ?: 0} M",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonCyan
            )
        }
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(NeonCyan.copy(alpha = 0.5f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "BEARING", style = MaterialTheme.typography.labelSmall, color = TextUnselected)
            Text(
                text = "${bearing?.toInt() ?: 0}°",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonMagenta
            )
        }
    }
}

/**
 * Grid Selection for Waypoints
 */
@Composable
fun WaypointSelectionGrid(
    waypoints: List<Waypoint>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLine, CutCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "TARGETS DETECTED: ${waypoints.size}", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
            Text(
                text = "CLEAR ALL",
                color = NeonMagenta,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable { onClear() }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Horizontal scrollable row for chips if many, or grid. 
        // For simplicity with variable width, using a FlowRow-like structure or just a simple Row with weight
        // Since we don't have FlowRow in older material3 stable easily, let's use a Row with horizontal scroll
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(waypoints.size) { index ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CutCornerShape(4.dp))
                        .background(if (isSelected) NeonCyan else GridLine)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (isSelected) Color.Black else NeonCyan,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Custom Alert Dialog
 */
@Composable
fun CyberAlertDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepBlack,
        title = { Text(title, color = NeonMagenta, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
        text = { Text(text, color = NeonCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
        confirmButton = {
            CyberButton(
                text = "CONFIRM",
                onClick = onConfirm,
                isWarning = true,
                modifier = Modifier.width(100.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextUnselected)
            }
        },
        shape = CutCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NeonMagenta, CutCornerShape(16.dp))
    )
}

/**
 * Decorative Grid Background
 */
@Composable
fun GridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        val width = size.width
        val height = size.height
        
        for (x in 0..width.toInt() step step.toInt()) {
            drawLine(
                color = GridLine,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), height),
                strokeWidth = 1f
            )
        }
        
        for (y in 0..height.toInt() step step.toInt()) {
            drawLine(
                color = GridLine,
                start = Offset(0f, y.toFloat()),
                end = Offset(width, y.toFloat()),
                strokeWidth = 1f
            )
        }
    }
}
