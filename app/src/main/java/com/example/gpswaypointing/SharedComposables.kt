package com.example.gpswaypointing

import android.app.Activity
import android.location.Location
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
 * Sci-Fi Dark Color Scheme.
 * Forces dark mode aesthetics.
 */
private val SciFiColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.Black,
    primaryContainer = AppColors.PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    tertiary = AppColors.Accent,
    onTertiary = Color.Black,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    error = AppColors.Error,
    onError = Color.Black,
    surfaceVariant = AppColors.SurfaceDark,
    onSurfaceVariant = AppColors.TextSecondary
)

/**
 * Custom theme for the GPS waypointing app.
 * Enforces Sci-Fi Dark Mode.
 */
@Composable
fun GPSWaypointingTheme(
    darkTheme: Boolean = true, // Force Dark Theme
    dynamicColor: Boolean = false, // Disable dynamic color to maintain aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = SciFiColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColors.Background.toArgb()
            window.navigationBarColor = AppColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
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
 * Main compass screen with Sci-Fi HUD UI components.
 */
@Composable
fun CompassScreen(
    locationService: LocationService,
    sensorService: SensorService,
    waypointRepository: WaypointRepository,
    modifier: Modifier = Modifier
) {
    val compassViewModel: CompassViewModel = viewModel { CompassViewModel(sensorService) }
    val waypointViewModel: WaypointViewModel = viewModel {
        WaypointViewModel(waypointRepository, CalculateDistanceUseCase(), FilterWaypointsUseCase())
    }
    val navigationViewModel: NavigationViewModel = viewModel {
        NavigationViewModel(CalculateDistanceUseCase(), CalculateBearingUseCase())
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
                navigationViewModel.updateNavigation(location, waypointViewModel.selectedWaypoint)
            }
        }
    }

    // Update navigation when selection changes
    LaunchedEffect(selectedIndex, currentLocation) {
        currentLocation?.let { location ->
            navigationViewModel.updateNavigation(location, waypointViewModel.selectedWaypoint)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        bottomBar = {
            // Glassmorphic Control Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Surface.copy(alpha = 0.9f),
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppColors.Background, AppColors.SurfaceDark)
                    )
                )
        ) {
            // HUD Top Panel
            NavigationInfo(
                navigationState = navigationState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Main Radar Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassView(
                    compassRotation = compassRotation,
                    currentLocation = currentLocation,
                    waypoints = waypointViewModel.getWaypointsInRange(),
                    selectedWaypoint = waypointViewModel.selectedWaypoint,
                    scaleMeters = scaleMeters,
                    onWaypointTapped = { waypoint ->
                        val index = waypoints.indexOf(waypoint)
                        if (index >= 0) waypointViewModel.selectWaypoint(index)
                    },
                    onScaleChanged = { waypointViewModel.updateScale(it) }
                )
                
                // Scale Indicator Overlay
                Text(
                    text = "RADAR SCALE: ${scaleMeters.toInt()}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .border(1.dp, AppColors.Primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Data List Panel
            if (waypoints.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.TextSecondary.copy(alpha = 0.3f))
                ) {
                    Column {
                        Text(
                            text = "WAYPOINT DATA LOG",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.Surface.copy(alpha = 0.5f))
                                .padding(8.dp)
                        )
                        WaypointList(
                            waypoints = waypoints,
                            selectedIndex = selectedIndex,
                            currentLocation = currentLocation,
                            onWaypointSelected = { waypointViewModel.selectWaypoint(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Cyberpunk Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = AppColors.Surface,
                title = { Text("PURGE DATA?", color = AppColors.Error) },
                text = { Text("Confirm deletion of all waypoint coordinates. This action is irreversible.", color = AppColors.TextPrimary) },
                confirmButton = {
                    Button(
                        onClick = {
                            waypointViewModel.clearWaypoints()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
                        shape = CutCornerShape(8.dp)
                    ) {
                        Text("PURGE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("CANCEL", color = AppColors.Primary)
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
 * Sci-Fi Radar Compass with infinite sweep animation.
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
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Layout
    val waypointRadiusPx = remember { with(density) { 6.dp.toPx() } } // Small blips
    val selectedRadiusPx = remember { with(density) { 12.dp.toPx() } }
    val compassFontSize = remember { with(density) { 24.sp.toPx() } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                val center = Offset(size.width / 2f, size.height / 2f)
                detectTapGestures { tapOffset ->
                    currentLocation?.let { location ->
                        waypoints.forEach { waypoint ->
                            val dist = location.distanceTo(Location("").apply { 
                                latitude = waypoint.latitude; longitude = waypoint.longitude 
                            })
                            val bearing = location.bearingTo(Location("").apply { 
                                latitude = waypoint.latitude; longitude = waypoint.longitude 
                            })
                            val radius = (dist / scaleMeters) * (size.width / 2)
                            val bx = center.x + (radius * sin(Math.toRadians(bearing.toDouble()))).toFloat()
                            val by = center.y - (radius * cos(Math.toRadians(bearing.toDouble()))).toFloat()

                            // Rotate tap
                            val angle = Math.toRadians(-compassRotation.toDouble())
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val rx = center.x + (dx * cos(angle) - dy * sin(angle)).toFloat()
                            val ry = center.y + (dx * sin(angle) + dy * cos(angle)).toFloat()

                            if (sqrt((rx - bx).pow(2) + (ry - by).pow(2)) <= 40.dp.toPx()) {
                                onWaypointTapped(waypoint)
                            }
                        }
                    }
                }
            }
            .pointerInput(scaleMeters) {
                detectTransformGestures { _, _, zoom, _ ->
                    onScaleChanged((scaleMeters / zoom).coerceIn(500f, 2000f))
                }
            }
    ) {
        val radius = size.minDimension / 2
        
        rotate(compassRotation) {
            // Radar Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AppColors.Surface, AppColors.Background),
                    radius = radius
                ),
                radius = radius
            )

            // Grid Lines & Rings
            for (i in 1..4) {
                val r = radius * (i / 4f)
                drawCircle(
                    color = AppColors.CompassRing.copy(alpha = 0.3f),
                    radius = r,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawLine(
                color = AppColors.CompassGrid.copy(alpha = 0.2f),
                start = center.copy(x = center.x - radius),
                end = center.copy(x = center.x + radius)
            )
            drawLine(
                color = AppColors.CompassGrid.copy(alpha = 0.2f),
                start = center.copy(y = center.y - radius),
                end = center.copy(y = center.y + radius)
            )

            // Cardinals
            val cardinals = listOf("N", "E", "S", "W")
            cardinals.forEachIndexed { index, label ->
                val angle = index * 90f
                val rad = Math.toRadians(angle.toDouble())
                val tx = center.x + (radius * 0.9f * sin(rad)).toFloat()
                val ty = center.y - (radius * 0.9f * cos(rad)).toFloat()
                
                drawIntoCanvas { 
                    val paint = android.graphics.Paint().apply {
                        color = if (label == "N") AppColors.Error.toArgb() else AppColors.Primary.toArgb()
                        textSize = compassFontSize
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                        this.isFakeBoldText = true
                    }
                    it.nativeCanvas.drawText(label, tx, ty + compassFontSize/3, paint)
                }
            }

            // Radar Sweep
            rotate(sweepAngle) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        1f to AppColors.Primary.copy(alpha = 0.5f)
                    ),
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = size.copy(width = radius * 2, height = radius * 2)
                )
            }

            // Waypoints (Blips)
            currentLocation?.let { loc ->
                waypoints.forEach { wp ->
                    val dist = loc.distanceTo(Location("").apply { 
                        latitude = wp.latitude; longitude = wp.longitude 
                    })
                    val bearing = loc.bearingTo(Location("").apply { 
                        latitude = wp.latitude; longitude = wp.longitude 
                    })
                    val r = (dist / scaleMeters) * radius
                    val bx = center.x + (r * sin(Math.toRadians(bearing.toDouble()))).toFloat()
                    val by = center.y - (r * cos(Math.toRadians(bearing.toDouble()))).toFloat()

                    if (r <= radius) {
                        val isSel = selectedWaypoint == wp
                        // Blip Glow
                        drawCircle(
                            color = (if (isSel) AppColors.Warning else AppColors.Primary).copy(alpha = pulseAlpha),
                            radius = if (isSel) selectedRadiusPx * 1.5f else waypointRadiusPx * 2f,
                            center = Offset(bx, by)
                        )
                        // Blip Core
                        drawCircle(
                            color = if (isSel) AppColors.Warning else AppColors.Primary,
                            radius = if (isSel) selectedRadiusPx else waypointRadiusPx,
                            center = Offset(bx, by)
                        )
                        
                        if (isSel) {
                            // Target Reticle
                            drawCircle(
                                color = AppColors.Warning,
                                radius = selectedRadiusPx + 4.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Nav Arrow
                selectedWaypoint?.let { wp ->
                    val bearing = loc.bearingTo(Location("").apply { 
                        latitude = wp.latitude; longitude = wp.longitude 
                    })
                    val rad = Math.toRadians(bearing.toDouble())
                    val arrowLen = radius * 0.8f
                    val ax = center.x + (arrowLen * sin(rad)).toFloat()
                    val ay = center.y - (arrowLen * cos(rad)).toFloat()
                    
                    drawLine(
                        color = AppColors.Success,
                        start = center,
                        end = Offset(ax, ay),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }
    }
}

/**
 * HUD Navigation Panel.
 */
@Composable
fun NavigationInfo(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = navigationState is NavigationState.Navigating,
        enter = slideInVertically() + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (navigationState is NavigationState.Navigating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppColors.Primary, CutCornerShape(bottomEnd = 16.dp))
                    .background(AppColors.Surface.copy(alpha = 0.8f), CutCornerShape(bottomEnd = 16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TARGET ACQUIRED", style = MaterialTheme.typography.labelSmall, color = AppColors.Warning)
                    Text(
                        "${navigationState.distance.toInt()}m", 
                        style = MaterialTheme.typography.displayMedium, 
                        color = AppColors.Primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BEARING", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                    Text(
                        "${navigationState.bearing.toInt()}°", 
                        style = MaterialTheme.typography.headlineLarge, 
                        color = AppColors.Success
                    )
                }
            }
        }
    }
}

/**
 * Cyberpunk Control Bottom Sheet.
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
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Tracking Button
            Button(
                onClick = if (isTracking) onStopTracking else onStartTracking,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) AppColors.Error.copy(alpha = 0.2f) else AppColors.Primary.copy(alpha = 0.2f),
                    contentColor = if (isTracking) AppColors.Error else AppColors.Primary
                ),
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isTracking) AppColors.Error else AppColors.Primary)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.LocationOn,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isTracking) "ABORT TRACK" else "INITIATE TRACK")
            }

            // ADD Button
            if (isTracking) {
                Button(
                    onClick = onAddWaypoint,
                    enabled = canAddWaypoint,
                    modifier = Modifier.size(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Warning,
                        contentColor = Color.Black
                    ),
                    shape = CutCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }

        if (hasWaypoints) {
            OutlinedButton(
                onClick = onClearWaypoints,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
                shape = CutCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("PURGE ALL DATA")
            }
        }
    }
}

@Composable
fun WaypointList(
    waypoints: List<Waypoint>,
    selectedIndex: Int?,
    currentLocation: Location?,
    onWaypointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val calculateDistance = CalculateDistanceUseCase()
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(8.dp)) {
        itemsIndexed(waypoints) { index, waypoint ->
            val isSelected = selectedIndex == index
            val dist = currentLocation?.let { calculateDistance(it, waypoint) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onWaypointSelected(index) }
                    .background(
                        if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                        CutCornerShape(topEnd = 8.dp)
                    )
                    .border(
                        1.dp, 
                        if (isSelected) AppColors.Primary else AppColors.TextSecondary.copy(alpha = 0.2f),
                        CutCornerShape(topEnd = 8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WP-${index + 1}",
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (dist != null) "${dist.toInt()}m" else "--",
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
