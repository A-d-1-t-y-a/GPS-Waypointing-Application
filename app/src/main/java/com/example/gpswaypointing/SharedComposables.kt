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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpswaypointing.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

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

    // Warp Speed Background + Chaos UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SciFiBackground)
    ) {
        WarpSpeedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Glitchy Header
            GlitchText(
                text = "NEON CHAOS",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Radar HUD
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
                        .aspectRatio(1f)
                        .padding(8.dp)
                        .pulseBorder(NeonCyan) // Add pulse effect to the whole radar
                )
            }

            // Data Info Panel
            if (compassState.selectedWaypoint != null) {
                val distance = compassState.getDistanceToSelectedWaypoint()
                val bearing = compassState.getBearingToSelectedWaypoint()
                
                DataHudPanel(
                    distance = distance,
                    bearing = bearing
                )
            } else {
                Spacer(modifier = Modifier.height(100.dp))
            }

            // Waypoint Selection
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

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberButton(
                    text = if (compassState.isTracking) "ABORT" else "ENGAGE",
                    onClick = { compassState.isTracking = !compassState.isTracking },
                    isWarning = compassState.isTracking,
                    modifier = Modifier.weight(1f)
                )

                if (compassState.isTracking) {
                    CyberButton(
                        text = "MARK",
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

        if (compassState.showClearDialog) {
            CyberAlertDialog(
                title = "SYSTEM PURGE",
                text = "CONFIRM COMPLETE DATA WIPE.",
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

// --- ANIMATION COMPONENTS ---

/**
 * Starfield effect that moves stars outward from center to simulate warp speed.
 */
@Composable
fun WarpSpeedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "WarpTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "Time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Pseudo-random stars based on time to create movement "flow"
        // In a real game engine we'd use particle objects, here we use procedural generation in draw loop
        // We simulate particles effectively by hashing index + time
        
        val numStars = 150
        val maxRadius = sqrt(centerX.pow(2) + centerY.pow(2))
        
        for (i in 0 until numStars) {
            // Seed random with star index to keep direction constant
            val random = Random(i)
            val angle = random.nextFloat() * 2 * PI
            val speed = random.nextFloat() * 2f + 0.5f
            val offset = random.nextFloat() * maxRadius // Initial offset
            
            // Calculate current radius based on loops
            // We want (offset + time * speed) % maxRadius
            // currentRadius needs to grow
            
            // Using frame time directly for smooth animation
            val rawRadius = (offset + (System.currentTimeMillis() % 3000) * speed * 0.5f)
            val currentRadius = rawRadius % maxRadius
            
            val starX = centerX + (currentRadius * cos(angle)).toFloat()
            val starY = centerY + (currentRadius * sin(angle)).toFloat()
            
            // Star size grows as it gets closer
            val starSize = (currentRadius / maxRadius) * 4.dp.toPx()
            val starAlpha = (currentRadius / maxRadius).coerceIn(0f, 1f)

            drawCircle(
                color = if (i % 5 == 0) NeonMagenta else NeonCyan,
                radius = starSize,
                center = Offset(starX, starY),
                alpha = starAlpha
            )
            
            // Draw trails for warp effect
            if (currentRadius > 50f) {
                val trailLength = starSize * 10
                val trailEndX = centerX + ((currentRadius - trailLength) * cos(angle)).toFloat()
                val trailEndY = centerY + ((currentRadius - trailLength) * sin(angle)).toFloat()
                
                drawLine(
                    color = if (i % 5 == 0) NeonMagenta else NeonCyan,
                    start = Offset(starX, starY),
                    end = Offset(trailEndX, trailEndY),
                    strokeWidth = starSize / 2,
                    alpha = starAlpha * 0.5f
                )
            }
        }
    }
}

/**
 * Text that jitters and splits channels
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glitch")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlitchOffset"
    )
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorShift"
    )

    Box(modifier = modifier) {
        // Red Channel Clone
        Text(
            text = text,
            style = style.copy(color = NeonMagenta.copy(alpha = 0.7f)),
            modifier = Modifier.offset(x = offsetX.dp, y = 0.dp)
        )
        // Cyan Channel Clone
        Text(
            text = text,
            style = style.copy(color = NeonCyan.copy(alpha = 0.7f)),
            modifier = Modifier.offset(x = (-offsetX).dp, y = 0.dp)
        )
        // Main Text
        Text(
            text = text,
            style = style.copy(color = Color.White),
            modifier = Modifier.offset(x = 0.dp, y = (if (colorShift > 0.9) 1 else 0).dp)
        )
    }
}

/**
 * Modifier to add a pulsating neon border
 */
fun Modifier.pulseBorder(color: Color): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    val width by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseWidth"
    )

    this.border(width.dp, color.copy(alpha = alpha), CutCornerShape(8.dp))
}

// --- UPDATED CORE COMPONENTS ---

@Composable
fun RadarHudView(
    compassState: CompassState,
    modifier: Modifier = Modifier
) {
    // Faster scan for chaos mode
    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing) // Faster scan
        ),
        label = "ScanAngle"
    )
    
    // Jitter rotation for 'malfunction' effect
    val jitter by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Jitter"
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

        // Draw Static Noise (Random lines)
        repeat(5) {
            val startX = Random.nextFloat() * size.width
            val startY = Random.nextFloat() * size.height
            val endX = startX + Random.nextFloat() * 20 - 10
            drawCircle(
                color = NeonCyan.copy(alpha = 0.2f),
                radius = 1f,
                center = Offset(startX, startY)
            )
        }

        // Draw Dynamic Rings
        drawCircle(
            color = NeonCyan,
            radius = radarRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        // Pulsing inner ring
        val pulseScale = (sin(System.currentTimeMillis() / 200.0) * 0.05 + 0.6).toFloat()
        drawCircle(
            color = NeonMagenta.copy(alpha = 0.5f),
            radius = radarRadius * pulseScale,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        // Rotate for Compass Heading + Jitter
        rotate(
            degrees = compassState.compassRotation + jitter,
            pivot = Offset(centerX, centerY)
        ) {
            // Draw Cardinal Directions (Technical Style)
            val directions = listOf("N", "E", "S", "W")
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 24.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }

            directions.forEachIndexed { index, label ->
                val angle = index * 90f
                val angleRad = Math.toRadians(angle.toDouble())
                val textX = centerX + (radarRadius * 1.1f * sin(angleRad)).toFloat()
                val textY = centerY - (radarRadius * 1.1f * cos(angleRad)).toFloat()

                // Glitchy text color
                paint.color = if (label == "N") NeonMagenta.toArgb() else NeonCyan.toArgb()
                if (Random.nextFloat() > 0.95) paint.color = Color.White.toArgb() // Random white flash

                drawContext.canvas.nativeCanvas.drawText(label, textX, textY + 10f, paint)
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
                    val blipDistance = scanScale * radarRadius
                    
                    val blipX = centerX + (blipDistance * sin(bearingRad)).toFloat()
                    val blipY = centerY - (blipDistance * cos(bearingRad)).toFloat()

                    val isSelected = compassState.selectedWaypoint == waypoint
                    val blipColor = if (isSelected) NeonMagenta else NeonCyan
                    
                    // Draw Blip
                    drawCircle(color = blipColor, radius = 6.dp.toPx(), center = Offset(blipX, blipY))

                    // Draw connecting line to center
                    if (isSelected) {
                        drawLine(
                            color = NeonMagenta.copy(alpha = 0.5f),
                            start = Offset(centerX, centerY),
                            end = Offset(blipX, blipY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
        
        // Draw Scanning Sweep
        rotate(scanAngle, pivot = Offset(centerX, centerY)) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1.0f to NeonCyan.copy(alpha = 0.8f),
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

// Helper for Radar Taps
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
            
            // Project waypoint relative to North (up)
            val waypointX_North = centerX + (blipDistance * sin(bearingRad)).toFloat()
            val waypointY_North = centerY - (blipDistance * cos(bearingRad)).toFloat()
            
            // Rotate around center by compass Rotation to match screen space
            val rotationRad = Math.toRadians(compassState.compassRotation.toDouble())
            val dx = waypointX_North - centerX
            val dy = waypointY_North - centerY
            
            val screenX = centerX + (dx * cos(rotationRad) - dy * sin(rotationRad)).toFloat()
            val screenY = centerY + (dx * sin(rotationRad) + dy * cos(rotationRad)).toFloat()
            
            val touchDist = sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
            
            if (touchDist < 50f) { 
                val actualIndex = compassState.waypoints.indexOf(waypoint)
                if (actualIndex >= 0) {
                    compassState.selectedWaypointIndex = actualIndex
                }
            }
        }
    }
}

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
            .pulseBorder(mainColor) // Add Pulse Border
            .clip(CutCornerShape(8.dp))
            .background(mainColor.copy(alpha = 0.1f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlitchText(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (enabled) mainColor else Color.Gray
            )
        )
    }
}

@Composable
fun DataHudPanel(distance: Float?, bearing: Float?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pulseBorder(NeonCyan)
            .background(DarkGlass)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PROXIMITY", color = TextUnselected, style = MaterialTheme.typography.labelSmall)
            GlitchText(
                text = "${distance?.toInt() ?: 0} M",
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VECTOR", color = TextUnselected, style = MaterialTheme.typography.labelSmall)
            GlitchText(
                text = "${bearing?.toInt() ?: 0}°",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TARGET LIST [${waypoints.size}]", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
            Text(
                text = "PURGE",
                color = NeonMagenta,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable { onClear() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

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
        title = { GlitchText(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, color = NeonCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
        confirmButton = {
            CyberButton(
                text = "EXECUTE",
                onClick = onConfirm,
                isWarning = true,
                modifier = Modifier.width(120.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ABORT", color = TextUnselected) }
        },
        shape = CutCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NeonMagenta, CutCornerShape(16.dp))
    )
}
