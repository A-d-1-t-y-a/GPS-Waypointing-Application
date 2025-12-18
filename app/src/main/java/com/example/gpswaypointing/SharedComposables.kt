package com.example.gpswaypointing

import android.app.Activity
import android.location.Location
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpswaypointing.data.Waypoint
import com.example.gpswaypointing.data.WaypointRepository
import com.example.gpswaypointing.domain.model.NavigationState
import com.example.gpswaypointing.domain.usecase.CalculateBearingUseCase
import com.example.gpswaypointing.domain.usecase.CalculateDistanceUseCase
import com.example.gpswaypointing.domain.usecase.FilterWaypointsUseCase
import com.example.gpswaypointing.service.LocationService
import com.example.gpswaypointing.service.SensorService
import com.example.gpswaypointing.theme.AppColors
import com.example.gpswaypointing.theme.AppTypography
import com.example.gpswaypointing.viewmodel.CompassViewModel
import com.example.gpswaypointing.viewmodel.NavigationViewModel
import com.example.gpswaypointing.viewmodel.WaypointViewModel
import kotlin.math.*

// ==========================================
// Theme
// ==========================================

/**
 * Custom light color scheme.
 */
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    tertiary = AppColors.Accent,
    onTertiary = Color.White,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    error = AppColors.Error,
    onError = Color.White
)

/**
 * Custom dark color scheme.
 */
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    tertiary = AppColors.Accent,
    onTertiary = Color.White,
    background = AppColors.SurfaceDark,
    onBackground = Color.White,
    surface = AppColors.SurfaceDark,
    onSurface = Color.White,
    error = AppColors.Error,
    onError = Color.White
)

/**
 * Custom theme for the GPS waypointing app.
 */
@Composable
fun GPSWaypointingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// ==========================================
// Screens
// ==========================================

/**
 * Main compass screen with all UI components integrated.
 */
@Composable
fun CompassScreen(
    locationService: LocationService,
    sensorService: SensorService,
    waypointRepository: WaypointRepository,
    modifier: Modifier = Modifier
) {
    val compassViewModel: CompassViewModel = viewModel {
        CompassViewModel(sensorService)
    }
    val waypointViewModel: WaypointViewModel = viewModel {
        WaypointViewModel(
            waypointRepository,
            CalculateDistanceUseCase(),
            FilterWaypointsUseCase()
        )
    }
    val navigationViewModel: NavigationViewModel = viewModel {
        NavigationViewModel(
            CalculateDistanceUseCase(),
            CalculateBearingUseCase()
        )
    }

    val compassRotation by compassViewModel.compassRotation.collectAsState(initial = 0f)
    val waypoints by waypointViewModel.waypoints.collectAsState()
    val selectedIndex by waypointViewModel.selectedWaypointIndex.collectAsState()
    val currentLocation by waypointViewModel.currentLocation.collectAsState()
    val scaleMeters by waypointViewModel.scaleMeters.collectAsState()
    val navigationState by navigationViewModel.navigationState.collectAsState()

    var isTracking by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Setup location tracking
    LaunchedEffect(isTracking) {
        if (isTracking) {
            locationService.getLocationUpdates().collect { location ->
                waypointViewModel.updateLocation(location)
                val selectedWaypoint = waypointViewModel.selectedWaypoint
                navigationViewModel.updateNavigation(location, selectedWaypoint)
            }
        }
    }

    // Update navigation when selection changes
    LaunchedEffect(selectedIndex, currentLocation) {
        currentLocation?.let { location ->
            val selectedWaypoint = waypointViewModel.selectedWaypoint
            navigationViewModel.updateNavigation(location, selectedWaypoint)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                ControlBottomSheet(
                    isTracking = isTracking,
                    canAddWaypoint = currentLocation != null,
                    hasWaypoints = waypoints.isNotEmpty(),
                    onStartTracking = { isTracking = true },
                    onStopTracking = { isTracking = false },
                    onAddWaypoint = {
                        currentLocation?.let { waypointViewModel.addWaypoint(it) }
                    },
                    onClearWaypoints = { showClearDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation info panel
            NavigationInfo(
                navigationState = navigationState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Compass view
            CompassView(
                compassRotation = compassRotation,
                currentLocation = currentLocation,
                waypoints = waypointViewModel.getWaypointsInRange(),
                selectedWaypoint = waypointViewModel.selectedWaypoint,
                scaleMeters = scaleMeters,
                onWaypointTapped = { waypoint ->
                    val index = waypoints.indexOf(waypoint)
                    if (index >= 0) {
                        waypointViewModel.selectWaypoint(index)
                    }
                },
                onScaleChanged = { newScale ->
                    waypointViewModel.updateScale(newScale)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

            // Waypoint list
            if (waypoints.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    WaypointList(
                        waypoints = waypoints,
                        selectedIndex = selectedIndex,
                        currentLocation = currentLocation,
                        onWaypointSelected = { index ->
                            waypointViewModel.selectWaypoint(index)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Clear confirmation dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Waypoints") },
                text = { Text("Are you sure you want to clear all waypoints? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            waypointViewModel.clearWaypoints()
                            showClearDialog = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ==========================================
// Views / Components
// ==========================================

/**
 * Enhanced compass view with distance rings, grid lines, and improved visual design.
 */
@Composable
fun CompassView(
    compassRotation: Float,
    currentLocation: Location?,
    waypoints: List<Waypoint>,
    selectedWaypoint: Waypoint?,
    scaleMeters: Float,
    onWaypointTapped: (Waypoint) -> Unit,
    onScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Pre-calculate pixel values
    val waypointRadiusPx = remember { with(density) { 20.dp.toPx() } }
    val selectedWaypointRadiusPx = remember { with(density) { 28.dp.toPx() } }
    val borderWidthPx = remember { with(density) { 3.dp.toPx() } }
    val arrowStrokeWidthPx = remember { with(density) { 5.dp.toPx() } }
    val arrowHeadSizePx = remember { with(density) { 18.dp.toPx() } }
    val compassTextSizePx = remember { with(density) { 36.sp.toPx() } }
    val gridLineWidthPx = remember { with(density) { 1.dp.toPx() } }
    val ringStrokeWidthPx = remember { with(density) { 2.dp.toPx() } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    currentLocation?.let { location ->
                        waypoints.forEach { waypoint ->
                            val waypointLocation = Location("").apply {
                                latitude = waypoint.latitude
                                longitude = waypoint.longitude
                            }
                            val distance = location.distanceTo(waypointLocation)
                            val bearing = location.bearingTo(waypointLocation)

                            val bearingRad = Math.toRadians(bearing.toDouble())
                            val size = size
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val maxRadius = min(size.width, size.height) / 2
                            val radius = (distance / scaleMeters) * maxRadius
                            
                            val waypointX = centerX + (radius * sin(bearingRad)).toFloat()
                            val waypointY = centerY - (radius * cos(bearingRad)).toFloat()

                            // Rotate tap coordinates back
                            val angleRad = Math.toRadians(-compassRotation.toDouble())
                            val dx = tapOffset.x - centerX
                            val dy = tapOffset.y - centerY
                            val rotatedX = centerX + (dx * cos(angleRad) - dy * sin(angleRad)).toFloat()
                            val rotatedY = centerY + (dx * sin(angleRad) + dy * cos(angleRad)).toFloat()

                            val distanceToWaypoint = sqrt(
                                (rotatedX - waypointX).pow(2) + 
                                (rotatedY - waypointY).pow(2)
                            )

                            val circleRadius = if (selectedWaypoint == waypoint) selectedWaypointRadiusPx else waypointRadiusPx
                            if (distanceToWaypoint <= circleRadius) {
                                onWaypointTapped(waypoint)
                            }
                        }
                    }
                }
            }
            .pointerInput(scaleMeters) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newScale = (scaleMeters / zoom).coerceIn(500f, 2000f)
                    onScaleChanged(newScale)
                }
            }
    ) {
        val size = size
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(size.width, size.height) / 2

        // Rotate canvas based on device orientation
        rotate(
            degrees = compassRotation,
            pivot = Offset(centerX, centerY)
        ) {
            // Draw compass background with gradient effect
            drawCircle(
                color = AppColors.CompassBackground.copy(alpha = 0.4f),
                radius = radius,
                center = Offset(centerX, centerY)
            )

            // Draw distance rings (concentric circles)
            val ringCount = 5
            for (i in 1..ringCount) {
                val ringRadius = (radius * i) / ringCount
                drawCircle(
                    color = AppColors.CompassRing.copy(alpha = 0.3f),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = ringStrokeWidthPx)
                )
            }

            // Draw grid lines (crosshair)
            drawLine(
                color = AppColors.CompassGrid.copy(alpha = 0.5f),
                start = Offset(centerX - radius, centerY),
                end = Offset(centerX + radius, centerY),
                strokeWidth = gridLineWidthPx
            )
            drawLine(
                color = AppColors.CompassGrid.copy(alpha = 0.5f),
                start = Offset(centerX, centerY - radius),
                end = Offset(centerX, centerY + radius),
                strokeWidth = gridLineWidthPx
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
                val textX = centerX + (radius * 0.88f * sin(angleRad)).toFloat()
                val textY = centerY - (radius * 0.88f * cos(angleRad)).toFloat()

                val color = if (label == "N") {
                    AppColors.NorthColor
                } else {
                    Color.Black
                }

                drawContext.canvas.drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = compassTextSizePx
                        this.color = color.toArgb()
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(textX, textY)
                    canvas.nativeCanvas.drawText(label, 0f, 0f, paint)
                    canvas.nativeCanvas.restore()
                }
            }

            // Draw waypoints
            currentLocation?.let { location ->
                waypoints.forEach { waypoint ->
                    val waypointLocation = Location("").apply {
                        latitude = waypoint.latitude
                        longitude = waypoint.longitude
                    }
                    val distance = location.distanceTo(waypointLocation)
                    val bearing = location.bearingTo(waypointLocation)

                    val bearingRad = Math.toRadians(bearing.toDouble())
                    val waypointRadius = (distance / scaleMeters) * radius
                    val waypointX = centerX + (waypointRadius * sin(bearingRad)).toFloat()
                    val waypointY = centerY - (waypointRadius * cos(bearingRad)).toFloat()

                    val isSelected = selectedWaypoint == waypoint
                    val circleColor = if (isSelected) AppColors.SelectedWaypointColor else AppColors.WaypointColor
                    val circleRadius = if (isSelected) selectedWaypointRadiusPx else waypointRadiusPx

                    // Draw waypoint circle
                    drawCircle(
                        color = circleColor,
                        radius = circleRadius,
                        center = Offset(waypointX, waypointY)
                    )

                    // Draw border for selected waypoint
                    if (isSelected) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = circleRadius + 6.dp.toPx(),
                            style = Stroke(width = borderWidthPx)
                        )
                    }
                }

                // Draw navigation arrow to selected waypoint
                selectedWaypoint?.let { selected ->
                    val selectedLocation = Location("").apply {
                        latitude = selected.latitude
                        longitude = selected.longitude
                    }
                    val bearing = location.bearingTo(selectedLocation)
                    val bearingRad = Math.toRadians(bearing.toDouble())

                    val arrowLength = radius * 0.75f
                    val arrowEndX = centerX + (arrowLength * sin(bearingRad)).toFloat()
                    val arrowEndY = centerY - (arrowLength * cos(bearingRad)).toFloat()

                    // Draw arrow line
                    drawLine(
                        color = AppColors.NavigationArrow,
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
                    drawPath(path, color = AppColors.NavigationArrow)
                }
            }
        }
    }
}

/**
 * Bottom sheet containing tracking controls and waypoint management.
 */
@Composable
fun ControlBottomSheet(
    isTracking: Boolean,
    canAddWaypoint: Boolean,
    hasWaypoints: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onAddWaypoint: () -> Unit,
    onClearWaypoints: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tracking control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = if (isTracking) onStopTracking else onStartTracking,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTracking) "Stop Tracking" else "Start Tracking")
            }

            if (isTracking) {
                FloatingActionButton(
                    onClick = onAddWaypoint,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Waypoint",
                        tint = if (canAddWaypoint) 
                            MaterialTheme.colorScheme.onSecondary 
                        else 
                            MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Clear waypoints button
        if (hasWaypoints) {
            OutlinedButton(
                onClick = onClearWaypoints,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Waypoints", fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Floating navigation info panel showing distance and bearing.
 */
@Composable
fun NavigationInfo(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = navigationState is NavigationState.Navigating,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (navigationState is NavigationState.Navigating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Distance: ${navigationState.distance.toInt()}m",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bearing: ${navigationState.bearing.toInt()}°",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Scrollable list of waypoints with distance indicators.
 */
@Composable
fun WaypointList(
    waypoints: List<Waypoint>,
    selectedIndex: Int?,
    currentLocation: Location?,
    onWaypointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val calculateDistance = CalculateDistanceUseCase()

    if (waypoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No waypoints yet.\nStart tracking and add waypoints!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(waypoints) { index, waypoint ->
                val distance = currentLocation?.let { 
                    calculateDistance(it, waypoint) 
                }
                WaypointItem(
                    waypoint = waypoint,
                    index = index,
                    distance = distance,
                    isSelected = selectedIndex == index,
                    onClick = { onWaypointSelected(index) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Individual waypoint item in the scrollable list.
 */
@Composable
fun WaypointItem(
    waypoint: Waypoint,
    index: Int,
    distance: Float?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Waypoint ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                if (distance != null) {
                    Text(
                        text = "${distance.toInt()}m away",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Selected",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
