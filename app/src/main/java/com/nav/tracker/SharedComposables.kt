package com.nav.tracker

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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nav.tracker.data.BeaconRegistry
import com.nav.tracker.data.NavBeacon
import com.nav.tracker.domain.model.GuidanceStatus
import com.nav.tracker.service.GyroscopeMonitor
import com.nav.tracker.service.PositionMonitor
import com.nav.tracker.theme.NavPalette
import com.nav.tracker.viewmodel.BeaconsViewModel
import com.nav.tracker.viewmodel.GuidanceViewModel
import com.nav.tracker.viewmodel.HudViewModel
import kotlin.math.*

/**
 * Main Interface Composable that orchestrates the entire UI.
 * Connects ViewModels and Services.
 * @param posMon The PositionMonitor service.
 * @param gyroMon The GyroscopeMonitor service.
 * @param reg The BeaconRegistry for data persistence.
 * @param mod Modifier for layout.
 */
@Composable
fun HudInterface(
    posMon: PositionMonitor,
    gyroMon: GyroscopeMonitor,
    reg: BeaconRegistry,
    mod: Modifier = Modifier
) {
    val bVM: BeaconsViewModel = viewModel { BeaconsViewModel(reg) }
    val gVM: GuidanceViewModel = viewModel { GuidanceViewModel() }
    val hVM: HudViewModel = viewModel { HudViewModel(gyroMon) }

    val azimuth by hVM.azimuth.collectAsState(initial = 0f)
    val beacons by bVM.beaconList.collectAsState()
    val activeBeacon by bVM.activeIdx.collectAsState()
    val currPos by bVM.currPos.collectAsState()
    val range by bVM.radarRange.collectAsState()
    val status by gVM.status.collectAsState()

    var scanning by remember { mutableStateOf(false) }
    var confirmPurge by remember { mutableStateOf(false) }

    LaunchedEffect(scanning) {
        if (scanning) {
            posMon.trackPosition().collect { loc ->
                bVM.updatePos(loc)
                gVM.refresh(loc, bVM.activeBeacon)
            }
        }
    }

    LaunchedEffect(activeBeacon, currPos) {
        currPos?.let { gVM.refresh(it, bVM.activeBeacon) }
    }

    Scaffold(
        modifier = mod.fillMaxSize(),
        containerColor = NavPalette.VoidBlack,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavPalette.VoidGrey.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavPalette.GridLine)
            ) {
                CommandPanel(
                    active = scanning,
                    canLog = currPos != null,
                    hasData = beacons.isNotEmpty(),
                    onToggle = { scanning = !scanning },
                    onLog = { currPos?.let { bVM.deployBeacon(it) } },
                    onPurge = { confirmPurge = true }
                )
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .background(Brush.verticalGradient(listOf(NavPalette.VoidBlack, NavPalette.VoidGrey)))
        ) {
            StatusReadout(status, Modifier.padding(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarDisplay(
                    azi = azimuth,
                    self = currPos,
                    pts = bVM.getVisibleBeacons(),
                    tgt = bVM.activeBeacon,
                    rng = range,
                    onTap = { b ->
                        val idx = beacons.indexOf(b)
                        if (idx >= 0) bVM.selectBeacon(idx)
                    },
                    onZoom = { bVM.setRange(it) }
                )
                
                Text(
                    "RNG: ${range.toInt()}m",
                    color = NavPalette.CoreCyan,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .border(1.dp, NavPalette.CoreCyan)
                        .padding(4.dp)
                )
            }

            if (beacons.isNotEmpty()) {
                BeaconsLog(
                    list = beacons,
                    sel = activeBeacon,
                    self = currPos,
                    onSel = { bVM.selectBeacon(it) },
                    mod = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
        
        if (confirmPurge) {
            AlertDialog(
                onDismissRequest = { confirmPurge = false },
                containerColor = NavPalette.VoidGrey,
                title = { Text("SYSTEM PURGE", color = NavPalette.SysError) },
                text = { Text("Wipe all navigational data?", color = NavPalette.TextMain) },
                confirmButton = {
                    Button(
                        onClick = { bVM.purgeAll(); confirmPurge = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NavPalette.SysError)
                    ) { Text("EXECUTE") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmPurge = false }) { Text("ABORT", color = NavPalette.CoreCyan) }
                }
            )
        }
    }
}

/**
 * Custom Canvas Compass Display.
 * Handles visualisation of waypoints, own position, direction, and gestures.
 */
@Composable
fun RadarDisplay(
    azi: Float,
    self: Location?,
    pts: List<NavBeacon>,
    tgt: NavBeacon?,
    rng: Float,
    onTap: (NavBeacon) -> Unit,
    onZoom: (Float) -> Unit,
    mod: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sweepAnim = rememberInfiniteTransition(label = "Sweep").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )

    Canvas(
        modifier = mod
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                val c = Offset(size.width / 2f, size.height / 2f)
                detectTapGestures { tap ->
                    self?.let { me ->
                        pts.forEach { b ->
                            val res = FloatArray(2)
                            Location.distanceBetween(me.latitude, me.longitude, b.lat, b.lng, res)
                            val d = res[0]; val bear = res[1]
                            val r = (d / rng) * (size.width / 2)
                            val rad = Math.toRadians(bear.toDouble())
                            val bx = c.x + (r * sin(rad)).toFloat()
                            val by = c.y - (r * cos(rad)).toFloat()
                            
                            // Invert the canvas rotation (-azi) to map tap to world space
                            val ang = Math.toRadians(azi.toDouble())
                            val dx = tap.x - c.x
                            val dy = tap.y - c.y
                            val rx = c.x + (dx * cos(ang) - dy * sin(ang)).toFloat()
                            val ry = c.y + (dx * sin(ang) + dy * cos(ang)).toFloat()
                            
                            if (hypot(rx - bx, ry - by) <= 50f) onTap(b)
                        }
                    }
                }
            }
            .pointerInput(rng) {
                detectTransformGestures { _, _, z, _ ->
                    onZoom((rng / z).coerceIn(500f, 2000f))
                }
            }
    ) {
        val r = size.minDimension / 2
        val gridColor = NavPalette.GridLine.copy(alpha = 0.3f)
        val gridDim = NavPalette.GridLine.copy(alpha = 0.2f)
        
        rotate(-azi) {
            drawCircle(
                color = NavPalette.VoidGrey,
                radius = r,
                center = center
            )
            
            // Grid
            for (i in 1..4) {
                drawCircle(
                    color = NavPalette.GridLine,
                    alpha = 0.3f,
                    radius = r * (i / 4f),
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
            drawLine(
                color = NavPalette.GridLine,
                alpha = 0.2f,
                start = center - Offset(r, 0f),
                end = center + Offset(r, 0f)
            )
            drawLine(
                color = NavPalette.GridLine,
                alpha = 0.2f,
                start = center - Offset(0f, r),
                end = center + Offset(0f, r)
            )

            // Sweep
            rotate(sweepAnim.value) {
                drawArc(
                    Brush.sweepGradient(0f to Color.Transparent, 1f to NavPalette.CoreCyan.copy(0.3f)),
                    -90f, 90f, true, topLeft = Offset(center.x - r, center.y - r), size = size
                )
            }

            // Blips
            self?.let { me ->
                pts.forEach { b ->
                    val res = FloatArray(2)
                    Location.distanceBetween(me.latitude, me.longitude, b.lat, b.lng, res)
                    val d = res[0]; val bear = res[1]
                    val distR = (d / rng) * r
                    val rad = Math.toRadians(bear.toDouble())
                    val bx = center.x + (distR * sin(rad)).toFloat()
                    val by = center.y - (distR * cos(rad)).toFloat()

                    if (distR <= r) {
                        val isTgt = tgt == b
                        drawCircle(
                            color = if (isTgt) NavPalette.CautionAmber else NavPalette.CoreCyan,
                            radius = 8f,
                            center = Offset(bx, by)
                        )
                        if (isTgt) {
                            drawCircle(
                                color = NavPalette.CautionAmber,
                                radius = 15f,
                                center = Offset(bx, by),
                                style = Stroke(2f)
                            )
                        }
                    }
                }
                
                tgt?.let { t ->
                    val res = FloatArray(2)
                    Location.distanceBetween(me.latitude, me.longitude, t.lat, t.lng, res)
                    val bear = res[1]
                    val rad = Math.toRadians(bear.toDouble())
                    val arrowR = r * 0.9f
                    val ax = center.x + (arrowR * sin(rad)).toFloat()
                    val ay = center.y - (arrowR * cos(rad)).toFloat()
                    
                    drawLine(
                        color = NavPalette.BioGreen,
                        start = center,
                        end = Offset(ax, ay),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }
        
        // Compass Directions
        rotate(-azi) {
            drawIntoCanvas { 
                val p = android.graphics.Paint().apply { 
                    textSize = 40f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true 
                }
                
                // North
                p.color = NavPalette.AlertRed.toArgb()
                it.nativeCanvas.drawText("N", center.x, center.y - r + 50f, p)
                
                // Other directions
                p.color = NavPalette.TextDim.toArgb()
                it.nativeCanvas.drawText("S", center.x, center.y + r - 30f, p)
                it.nativeCanvas.drawText("E", center.x + r - 30f, center.y, p)
                it.nativeCanvas.drawText("W", center.x - r + 30f, center.y, p)
            }
        }
    }
}

/**
 * Displays status information like Distance and Bearing.
 */
@Composable
fun StatusReadout(st: GuidanceStatus, mod: Modifier) {
    if (st is GuidanceStatus.Active) {
        Row(mod.fillMaxWidth().border(1.dp, NavPalette.CoreCyan).background(NavPalette.VoidGrey.copy(0.8f)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TARGET LOCKED", color = NavPalette.CautionAmber, style = MaterialTheme.typography.labelSmall)
                Text("${st.range.toInt()}m", color = NavPalette.CoreCyan, style = MaterialTheme.typography.displayLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("VECTOR", color = NavPalette.TextDim, style = MaterialTheme.typography.labelSmall)
                Text("${st.azimuth.toInt()}°", color = NavPalette.BioGreen, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

/**
 * Control panel for Start/Stop tracking and logic.
 */
@Composable
fun CommandPanel(
    active: Boolean,
    canLog: Boolean, 
    hasData: Boolean,
    onToggle: () -> Unit,
    onLog: () -> Unit,
    onPurge: () -> Unit
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onToggle, Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (active) NavPalette.SysError.copy(0.2f) else NavPalette.CoreCyan.copy(0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) NavPalette.SysError else NavPalette.CoreCyan),
                shape = CutCornerShape(12.dp)
            ) {
                Icon(if (active) Icons.Default.Stop else Icons.Default.LocationOn, null, tint = if (active) NavPalette.SysError else NavPalette.CoreCyan)
                Spacer(Modifier.width(8.dp))
                Text(if (active) "ABORT" else "ENGAGE", color = if (active) NavPalette.SysError else NavPalette.CoreCyan)
            }
            
            if (active) {
                Button(onClick = onLog, enabled = canLog, modifier = Modifier.size(56.dp), shape = CutCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NavPalette.CautionAmber)) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                }
            }
        }
        
        if (hasData) {
            OutlinedButton(onClick = onPurge, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = NavPalette.SysError), border = androidx.compose.foundation.BorderStroke(1.dp, NavPalette.SysError)) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("SYSTEM PURGE")
            }
        }
    }
}

/**
 * List of waypoints.
 */
@Composable
fun BeaconsLog(
    list: List<NavBeacon>,
    sel: Int?,
    self: Location?,
    onSel: (Int) -> Unit,
    mod: Modifier
) {
    LazyColumn(mod, contentPadding = PaddingValues(8.dp)) {
        itemsIndexed(list) { i, b ->
            val isSel = sel == i
            val dist = self?.let { 
                val r = FloatArray(1); Location.distanceBetween(it.latitude, it.longitude, b.lat, b.lng, r); r[0] 
            }
            
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSel(i) }
                    .background(if (isSel) NavPalette.CoreCyan.copy(0.1f) else Color.Transparent)
                    .border(1.dp, if (isSel) NavPalette.CoreCyan else NavPalette.TextDim.copy(0.2f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BCN-${i+1}", color = if (isSel) NavPalette.CoreCyan else NavPalette.TextMain)
                Spacer(Modifier.weight(1f))
                Text(if (dist !=null) "${dist.toInt()}m" else "--", color = NavPalette.TextDim)
            }
        }
    }
}
