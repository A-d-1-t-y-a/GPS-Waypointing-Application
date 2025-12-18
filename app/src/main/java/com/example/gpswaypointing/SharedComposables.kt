package com.example.gpswaypointing

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gpswaypointing.data.LocationPoint
import com.example.gpswaypointing.data.LocationStorageManager
import com.example.gpswaypointing.service.GPSMonitor
import com.example.gpswaypointing.service.OrientationProvider
import com.example.gpswaypointing.theme.VintageColors
import com.example.gpswaypointing.theme.VintageMapTheme
import com.example.gpswaypointing.theme.VintageTypography
import com.example.gpswaypointing.utils.GeoMathUtils
import com.example.gpswaypointing.viewmodel.NavigatorViewModel
import com.example.gpswaypointing.viewmodel.OrientationViewModel
import com.example.gpswaypointing.viewmodel.TravelLogViewModel
import kotlin.math.*

// ==========================================
// Theme (Re-export for simple access)
// ==========================================
// VintageMapTheme is in theme/Theme.kt, used by MainActivity.

// ==========================================
// Screens
// ==========================================

/**
 * Main Interface: "Explorer's Dashboard".
 */
@Composable
fun MainExplorerInterface(
    gpsMonitor: GPSMonitor,
    orientationProvider: OrientationProvider,
    storageManager: LocationStorageManager,
    modifier: Modifier = Modifier
) {
    val orientationVM: OrientationViewModel = viewModel { OrientationViewModel(orientationProvider) }
    val travelLogVM: TravelLogViewModel = viewModel { TravelLogViewModel(storageManager) }
    val navigatorVM: NavigatorViewModel = viewModel { NavigatorViewModel() }

    val azimuth by orientationVM.azimuth.collectAsState()
    val logEntries by travelLogVM.logEntries
    val userLoc by travelLogVM.userLocation
    val targetIndex by travelLogVM.activeTargetIndex
    val zoom by travelLogVM.zoomLevel
    val navData by navigatorVM.navStatus

    var isRecording by remember { mutableStateOf(false) }
    var showingPurgeConfirm by remember { mutableStateOf(false) }

    // GPS Logic
    LaunchedEffect(isRecording) {
        if (isRecording) {
            gpsMonitor.requestLocationUpdates().collect { loc ->
                travelLogVM.updateUserPosition(loc)
                navigatorVM.refreshNavigation(loc, travelLogVM.getTargetPoint())
            }
        }
    }

    // Navigation Update Logic
    LaunchedEffect(targetIndex, userLoc) {
        userLoc?.let { 
            navigatorVM.refreshNavigation(it, travelLogVM.getTargetPoint()) 
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VintageColors.Parchment,
        bottomBar = {
             Surface(
                 modifier = Modifier.fillMaxWidth(),
                 color = VintageColors.ParchmentDark,
                 shadowElevation = 8.dp,
                 border = androidx.compose.foundation.BorderStroke(2.dp, VintageColors.LeatherBrown)
             ) {
                 ActionPanel(
                     isRecording = isRecording,
                     canMark = userLoc != null,
                     hasBriefs = logEntries.isNotEmpty(),
                     onToggleRecord = { isRecording = !isRecording },
                     onMarkSpot = { userLoc?.let { travelLogVM.recordLocation(it) } },
                     onPurge = { showingPurgeConfirm = true }
                 )
             }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header: Map Title / Nav Info
            MapHeader(
                navData = navData,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Main Map: Nautical Compass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(4.dp, VintageColors.LeatherBrown)
                    .background(VintageColors.ParchmentDark)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                NauticalCompass(
                    rotationDesc = azimuth,
                    selfLoc = userLoc,
                    points = travelLogVM.getPointsInView(),
                    activePoint = travelLogVM.getTargetPoint(),
                    zoomMeters = zoom,
                    onTapPoint = { pt ->
                        val idx = logEntries.indexOf(pt)
                        if (idx != -1) travelLogVM.setTarget(idx)
                    },
                    onZoomChange = { travelLogVM.setZoom(it) }
                )
                
                // Scale Legend
                Text(
                    text = "Scale: 1:${zoom.toInt()}",
                    style = VintageTypography.labelLarge,
                    color = VintageColors.InkBlack,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(VintageColors.Parchment.copy(alpha = 0.8f))
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Footer: Field Notes (List)
            if (logEntries.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    color = VintageColors.Parchment,
                    border = androidx.compose.foundation.BorderStroke(2.dp, VintageColors.InkBlue)
                ) {
                    TravelLog(
                        entries = logEntries,
                        targetIndex = targetIndex,
                        userLoc = userLoc,
                        onSelect = { travelLogVM.setTarget(it) }
                    )
                }
            }
        }

        if (showingPurgeConfirm) {
            AlertDialog(
                onDismissRequest = { showingPurgeConfirm = false },
                containerColor = VintageColors.Parchment,
                title = { Text("Burn Field Notes?", style = VintageTypography.headlineMedium, color = VintageColors.OldRed) },
                text = { Text("This will destroy all recorded coordinates forever.", style = VintageTypography.bodyLarge, color = VintageColors.InkBlack) },
                confirmButton = {
                    Button(
                        onClick = { travelLogVM.clearLog(); showingPurgeConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VintageColors.OldRed)
                    ) { Text("Burn", color = VintageColors.Parchment) }
                },
                dismissButton = {
                    TextButton(onClick = { showingPurgeConfirm = false }) {
                        Text("Keep", color = VintageColors.InkBlue)
                    }
                }
            )
        }
    }
}

// ==========================================
// Views
// ==========================================

/**
 * Styled Nautical Compass.
 */
@Composable
fun NauticalCompass(
    rotationDesc: Float,
    selfLoc: Location?,
    points: List<LocationPoint>,
    activePoint: LocationPoint?,
    zoomMeters: Float,
    onTapPoint: (LocationPoint) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val compassFontSize = remember { with(density) { 18.sp.toPx() } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                val center = Offset(size.width / 2f, size.height / 2f)
                detectTapGestures { tap ->
                    selfLoc?.let { me ->
                        points.forEach { pt ->
                            val dist = GeoMathUtils.computeDistance(me.latitude, me.longitude, pt.lat, pt.lng)
                            val bear = GeoMathUtils.computeBearing(me.latitude, me.longitude, pt.lat, pt.lng)
                            val r = (dist / zoomMeters) * (size.width / 2)
                            val rad = Math.toRadians(bear.toDouble())
                            
                            val bx = center.x + (r * sin(rad)).toFloat()
                            val by = center.y - (r * cos(rad)).toFloat()
                            
                            // Rotate touch
                            val ang = Math.toRadians(-rotationDesc.toDouble())
                            val dx = tap.x - center.x
                            val dy = tap.y - center.y
                            val rx = center.x + (dx * cos(ang) - dy * sin(ang)).toFloat()
                            val ry = center.y + (dx * sin(ang) + dy * cos(ang)).toFloat()
                            
                            if (sqrt((rx - bx).pow(2) + (ry - by).pow(2)) < 50f) {
                                onTapPoint(pt)
                            }
                        }
                    }
                }
            }
            .pointerInput(zoomMeters) {
                detectTransformGestures { _, _, z, _ ->
                    onZoomChange((zoomMeters / z).coerceIn(500f, 2000f))
                }
            }
    ) {
        val radius = size.minDimension / 2
        
        rotate(rotationDesc) {
            // Background Paper
            drawCircle(color = VintageColors.Parchment, radius = radius)
            
            // Outer Ring
            drawCircle(
                color = VintageColors.InkBlue, 
                radius = radius, 
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Inner decorative rings
            drawCircle(
                color = VintageColors.Sepia.copy(alpha = 0.5f),
                radius = radius * 0.8f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Cardinals
            val dirs = listOf("N", "E", "S", "W")
            dirs.forEachIndexed { i, txt ->
                val ang = i * 90f
                val rad = Math.toRadians(ang.toDouble())
                val tx = center.x + (radius * 0.9f * sin(rad)).toFloat()
                val ty = center.y - (radius * 0.9f * cos(rad)).toFloat()
                
                drawIntoCanvas { 
                    val p = android.graphics.Paint().apply {
                        color = if (txt=="N") VintageColors.OldRed.toArgb() else VintageColors.InkBlack.toArgb()
                        textSize = compassFontSize
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.SERIF
                        isFakeBoldText = true
                    }
                    it.nativeCanvas.drawText(txt, tx, ty + 10f, p)
                }
            }
            
            // Crosshairs
            drawLine(
                color = VintageColors.FadedInk,
                start = Offset(center.x - radius * 0.5f, center.y),
                end = Offset(center.x + radius * 0.5f, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = VintageColors.FadedInk,
                start = Offset(center.x, center.y - radius * 0.5f),
                end = Offset(center.x, center.y + radius * 0.5f),
                strokeWidth = 2f
            )

            // Marks
            selfLoc?.let { me ->
                points.forEach { pt ->
                    val dist = GeoMathUtils.computeDistance(me.latitude, me.longitude, pt.lat, pt.lng)
                    val bear = GeoMathUtils.computeBearing(me.latitude, me.longitude, pt.lat, pt.lng)
                    val r = (dist / zoomMeters) * radius
                    val rad = Math.toRadians(bear.toDouble())
                    
                    val bx = center.x + (r * sin(rad)).toFloat()
                    val by = center.y - (r * cos(rad)).toFloat()
                    
                    if (r <= radius) {
                        val isTarget = activePoint == pt
                        // "X" marks the spot
                        val s = if (isTarget) 15f else 10f
                        val c = if (isTarget) VintageColors.OldRed else VintageColors.InkBlue
                        
                        drawLine(color = c, start = Offset(bx - s, by - s), end = Offset(bx + s, by + s), strokeWidth = 4f)
                        drawLine(color = c, start = Offset(bx + s, by - s), end = Offset(bx - s, by + s), strokeWidth = 4f)
                    }
                }
                
                activePoint?.let { tgt ->
                    // Navigation Line
                    val bear = GeoMathUtils.computeBearing(me.latitude, me.longitude, tgt.lat, tgt.lng)
                    val rad = Math.toRadians(bear.toDouble())
                    val len = radius * 0.7f
                    val ex = center.x + (len * sin(rad)).toFloat()
                    val ey = center.y - (len * cos(rad)).toFloat()
                    
                    drawLine(
                        color = VintageColors.ForestGreen,
                        start = center, 
                        end = Offset(ex, ey),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun MapHeader(
    navData: NavigatorViewModel.NavData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(VintageColors.Parchment)
            .border(2.dp, VintageColors.InkBlue)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("FIELD POSITION", style = VintageTypography.labelLarge, color = VintageColors.FadedInk)
            if (navData.isActive) {
                Text(
                    "DST: ${navData.dist.toInt()} m", 
                    style = VintageTypography.headlineMedium, 
                    color = VintageColors.InkBlack
                )
            } else {
                Text("SCANNING...", style = VintageTypography.titleMedium, color = VintageColors.Sepia)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
             Text("BEARING", style = VintageTypography.labelLarge, color = VintageColors.FadedInk)
             Text(
                 "${navData.azi.toInt()}°", 
                 style = VintageTypography.headlineMedium, 
                 color = VintageColors.OldRed
             )
        }
    }
}

@Composable
fun ActionPanel(
    isRecording: Boolean,
    canMark: Boolean,
    hasBriefs: Boolean,
    onToggleRecord: () -> Unit,
    onMarkSpot: () -> Unit,
    onPurge: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onToggleRecord,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) VintageColors.LeatherBrown else VintageColors.InkBlue
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(if (isRecording) Icons.Default.Close else Icons.Default.Explore, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRecording) "HALT" else "SURVEY")
            }
            
            if (isRecording) {
                 Button(
                    onClick = onMarkSpot,
                    enabled = canMark,
                    colors = ButtonDefaults.buttonColors(containerColor = VintageColors.BurntOrange),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null)
                    Text("MARK")
                }
            }
        }
        
        if (hasBriefs) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPurge, 
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VintageColors.OldRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, VintageColors.OldRed)
            ) {
                Text("DISCARD ALL RECORDS")
            }
        }
    }
}

@Composable
fun TravelLog(
    entries: List<LocationPoint>,
    targetIndex: Int?,
    userLoc: Location?,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            "FIELD NOTES", 
            style = VintageTypography.labelLarge, 
            modifier = Modifier.fillMaxWidth().background(VintageColors.Sepia).padding(8.dp),
            color = VintageColors.Parchment
        )
        LazyColumn(contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(entries) { idx, item ->
                val isSel = idx == targetIndex
                val dist = userLoc?.let { 
                    GeoMathUtils.computeDistance(it.latitude, it.longitude, item.lat, item.lng) 
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(if (isSel) VintageColors.ParchmentDark else Color.Transparent)
                        .clickable { onSelect(idx) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Entry #${idx + 1}", 
                        style = VintageTypography.bodyLarge,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        if (dist != null) "${dist.toInt()} m" else "---",
                        style = VintageTypography.bodyMedium,
                        color = VintageColors.FadedInk
                    )
                }
                Divider(color = VintageColors.Sepia.copy(alpha = 0.2f))
            }
        }
    }
}
