package com.vintagenav.explorer

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
import com.vintagenav.explorer.data.LocationPoint
import com.vintagenav.explorer.data.LocationStorageManager
import com.vintagenav.explorer.service.GPSMonitor
import com.vintagenav.explorer.service.OrientationProvider
import com.vintagenav.explorer.theme.ExplorerPalette
import com.vintagenav.explorer.theme.ExpeditionTheme
import com.vintagenav.explorer.theme.LogbookFont
import com.vintagenav.explorer.utils.GeoMathUtils
import com.vintagenav.explorer.viewmodel.NavigatorViewModel
import com.vintagenav.explorer.viewmodel.OrientationViewModel
import com.vintagenav.explorer.viewmodel.TravelLogViewModel
import kotlin.math.*

// ==========================================
// Theme (Re-export for simple access)
// ==========================================
// ExpeditionTheme is in theme/ExpeditionTheme.kt, used by MainActivity.

// ==========================================
// Screens
// ==========================================

/**
 * Main Interface: "Explorer's Dashboard".
 */
@Composable
fun ExpeditionDashboard(
    locationTracker: GPSMonitor,
    compassSensor: OrientationProvider,
    dataStore: LocationStorageManager,
    uiMod: Modifier = Modifier
) {
    val compassVM: OrientationViewModel = viewModel { OrientationViewModel(compassSensor) }
    val journalVM: TravelLogViewModel = viewModel { TravelLogViewModel(dataStore) }
    val guideVM: NavigatorViewModel = viewModel { NavigatorViewModel() }

    val currHeading by compassVM.currHeading.collectAsState()
    val journalEntries by journalVM.journalEntries
    val myLoc by journalVM.myLoc
    val targetIdx by journalVM.targetIdx
    val viewScale by journalVM.viewScale
    val guidanceState by guideVM.guidanceState

    var trackingActive by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // GPS Logic
    LaunchedEffect(trackingActive) {
        if (trackingActive) {
            locationTracker.streamLocation().collect { loc ->
                journalVM.updateMyPosition(loc)
                guideVM.updateGuidance(loc, journalVM.getActiveTarget())
            }
        }
    }

    // Navigation Update Logic
    LaunchedEffect(targetIdx, myLoc) {
        myLoc?.let { 
            guideVM.updateGuidance(it, journalVM.getActiveTarget()) 
        }
    }

    Scaffold(
        modifier = uiMod.fillMaxSize(),
        containerColor = ExplorerPalette.MapCream,
        bottomBar = {
             Surface(
                 modifier = Modifier.fillMaxWidth(),
                 color = ExplorerPalette.MapBeige,
                 shadowElevation = 8.dp,
                 border = androidx.compose.foundation.BorderStroke(2.dp, ExplorerPalette.SaddleBrown)
             ) {
                 FieldControls(
                     trackingActive = trackingActive,
                     canMark = myLoc != null,
                     hasBriefs = journalEntries.isNotEmpty(),
                     onToggleRecord = { trackingActive = !trackingActive },
                     onMarkSpot = { myLoc?.let { journalVM.recordSpot(it) } },
                     onPurge = { confirmDiscard = true }
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
            StatusReadout(
                navData = guidanceState,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Main Map: Nautical Compass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(4.dp, ExplorerPalette.SaddleBrown)
                    .background(ExplorerPalette.MapBeige)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassRose(
                    bearing = currHeading,
                    selfLoc = myLoc,
                    sights = journalVM.getVisiblePoints(),
                    activePoint = journalVM.getActiveTarget(),
                    scaleMeters = viewScale,
                    onTapPoint = { pt ->
                        val idx = journalEntries.indexOf(pt)
                        if (idx != -1) journalVM.setTarget(idx)
                    },
                    onScaleChange = { journalVM.setScale(it) }
                )
                
                // Scale Legend
                Text(
                    text = "Scale: 1:${viewScale.toInt()}",
                    style = LogbookFont.labelLarge,
                    color = ExplorerPalette.Charcoal,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(ExplorerPalette.MapCream.copy(alpha = 0.8f))
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Footer: Field Notes (List)
            if (journalEntries.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    color = ExplorerPalette.MapCream,
                    border = androidx.compose.foundation.BorderStroke(2.dp, ExplorerPalette.DeepBlue)
                ) {
                    FieldJournal(
                        entries = journalEntries,
                        targetIdx = targetIdx,
                        userLoc = myLoc,
                        onSelect = { journalVM.setTarget(it) }
                    )
                }
            }
        }

        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                containerColor = ExplorerPalette.MapCream,
                title = { Text("Burn Field Notes?", style = LogbookFont.headlineMedium, color = ExplorerPalette.Crimson) },
                text = { Text("This will destroy all recorded coordinates forever.", style = LogbookFont.bodyLarge, color = ExplorerPalette.Charcoal) },
                confirmButton = {
                    Button(
                        onClick = { journalVM.clearJournal(); confirmDiscard = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ExplorerPalette.Crimson)
                    ) { Text("Burn", color = ExplorerPalette.MapCream) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDiscard = false }) {
                        Text("Keep", color = ExplorerPalette.DeepBlue)
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
fun CompassRose(
    bearing: Float,
    selfLoc: Location?,
    sights: List<LocationPoint>,
    activePoint: LocationPoint?,
    scaleMeters: Float,
    onTapPoint: (LocationPoint) -> Unit,
    onScaleChange: (Float) -> Unit,
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
                        sights.forEach { pt ->
                            val dist = GeoMathUtils.calcDist(me.latitude, me.longitude, pt.lat, pt.lng)
                            val bear = GeoMathUtils.calcBearing(me.latitude, me.longitude, pt.lat, pt.lng)
                            val r = (dist / scaleMeters) * (size.width / 2)
                            val rad = Math.toRadians(bear.toDouble())
                            
                            val bx = center.x + (r * sin(rad)).toFloat()
                            val by = center.y - (r * cos(rad)).toFloat()
                            
                            // Rotate touch
                            val ang = Math.toRadians(-bearing.toDouble())
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
            .pointerInput(scaleMeters) {
                detectTransformGestures { _, _, z, _ ->
                    onScaleChange((scaleMeters / z).coerceIn(500f, 2000f))
                }
            }
    ) {
        val radius = size.minDimension / 2
        
        rotate(bearing) {
            // Background Paper
            drawCircle(color = ExplorerPalette.MapCream, radius = radius)
            
            // Outer Ring
            drawCircle(
                color = ExplorerPalette.DeepBlue, 
                radius = radius, 
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Inner decorative rings
            drawCircle(
                color = ExplorerPalette.OldSepia.copy(alpha = 0.5f),
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
                        color = if (txt=="N") ExplorerPalette.Crimson.toArgb() else ExplorerPalette.Charcoal.toArgb()
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
                color = ExplorerPalette.SlateGray,
                start = Offset(center.x - radius * 0.5f, center.y),
                end = Offset(center.x + radius * 0.5f, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = ExplorerPalette.SlateGray,
                start = Offset(center.x, center.y - radius * 0.5f),
                end = Offset(center.x, center.y + radius * 0.5f),
                strokeWidth = 2f
            )

            // Marks
            selfLoc?.let { me ->
                sights.forEach { pt ->
                    val dist = GeoMathUtils.calcDist(me.latitude, me.longitude, pt.lat, pt.lng)
                    val bear = GeoMathUtils.calcBearing(me.latitude, me.longitude, pt.lat, pt.lng)
                    val r = (dist / scaleMeters) * radius
                    val rad = Math.toRadians(bear.toDouble())
                    
                    val bx = center.x + (r * sin(rad)).toFloat()
                    val by = center.y - (r * cos(rad)).toFloat()
                    
                    if (r <= radius) {
                        val isTarget = activePoint == pt
                        // "X" marks the spot
                        val s = if (isTarget) 15f else 10f
                        val c = if (isTarget) ExplorerPalette.Crimson else ExplorerPalette.DeepBlue
                        
                        drawLine(color = c, start = Offset(bx - s, by - s), end = Offset(bx + s, by + s), strokeWidth = 4f)
                        drawLine(color = c, start = Offset(bx + s, by - s), end = Offset(bx - s, by + s), strokeWidth = 4f)
                    }
                }
                
                activePoint?.let { tgt ->
                    // Navigation Line
                    val bear = GeoMathUtils.calcBearing(me.latitude, me.longitude, tgt.lat, tgt.lng)
                    val rad = Math.toRadians(bear.toDouble())
                    val len = radius * 0.7f
                    val ex = center.x + (len * sin(rad)).toFloat()
                    val ey = center.y - (len * cos(rad)).toFloat()
                    
                    drawLine(
                        color = ExplorerPalette.HunterGreen,
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
fun StatusReadout(
    navData: NavigatorViewModel.NavData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(ExplorerPalette.MapCream)
            .border(2.dp, ExplorerPalette.DeepBlue)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("FIELD POSITION", style = LogbookFont.labelLarge, color = ExplorerPalette.SlateGray)
            if (navData.isActive) {
                Text(
                    "DST: ${navData.dist.toInt()} m", 
                    style = LogbookFont.headlineMedium, 
                    color = ExplorerPalette.Charcoal
                )
            } else {
                Text("SCANNING...", style = LogbookFont.titleMedium, color = ExplorerPalette.OldSepia)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
             Text("BEARING", style = LogbookFont.labelLarge, color = ExplorerPalette.SlateGray)
             Text(
                 "${navData.azi.toInt()}°", 
                 style = LogbookFont.headlineMedium, 
                 color = ExplorerPalette.Crimson
             )
        }
    }
}

@Composable
fun FieldControls(
    trackingActive: Boolean,
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
                    containerColor = if (trackingActive) ExplorerPalette.SaddleBrown else ExplorerPalette.DeepBlue
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(if (trackingActive) Icons.Default.Close else Icons.Default.Explore, null)
                Spacer(Modifier.width(8.dp))
                Text(if (trackingActive) "HALT" else "SURVEY")
            }
            
            if (trackingActive) {
                 Button(
                    onClick = onMarkSpot,
                    enabled = canMark,
                    colors = ButtonDefaults.buttonColors(containerColor = ExplorerPalette.RustOrange),
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExplorerPalette.Crimson),
                border = androidx.compose.foundation.BorderStroke(1.dp, ExplorerPalette.Crimson)
            ) {
                Text("DISCARD ALL RECORDS")
            }
        }
    }
}

@Composable
fun FieldJournal(
    entries: List<LocationPoint>,
    targetIdx: Int?,
    userLoc: Location?,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            "FIELD NOTES", 
            style = LogbookFont.labelLarge, 
            modifier = Modifier.fillMaxWidth().background(ExplorerPalette.OldSepia).padding(8.dp),
            color = ExplorerPalette.MapCream
        )
        LazyColumn(contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(entries) { idx, item ->
                val isSel = idx == targetIdx
                val dist = userLoc?.let { 
                    GeoMathUtils.calcDist(it.latitude, it.longitude, item.lat, item.lng) 
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(if (isSel) ExplorerPalette.MapBeige else Color.Transparent)
                        .clickable { onSelect(idx) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Entry #${idx + 1}", 
                        style = LogbookFont.bodyLarge,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        if (dist != null) "${dist.toInt()} m" else "---",
                        style = LogbookFont.bodyMedium,
                        color = ExplorerPalette.SlateGray
                    )
                }
                Divider(color = ExplorerPalette.OldSepia.copy(alpha = 0.2f))
            }
        }
    }
}
