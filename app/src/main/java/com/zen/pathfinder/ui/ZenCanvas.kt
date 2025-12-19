package com.zen.pathfinder.ui

import android.location.Location
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zen.pathfinder.core.GuideStone
import com.zen.pathfinder.core.StoneArchive
import com.zen.pathfinder.core.ZenMath
import com.zen.pathfinder.system.CompassSpirit
import com.zen.pathfinder.system.PathSense
import com.zen.pathfinder.ui.theme.ZenPalette
import kotlin.math.*

@Composable
fun ZenCanvas(
    sense: PathSense,
    spirit: CompassSpirit,
    store: StoneArchive
) {
    val model: HarmonyModel = viewModel { HarmonyModel(store) }
    
    val align by spirit.spirit().collectAsState(initial = 0f)
    var walking by remember { mutableStateOf(false) }
    
    LaunchedEffect(walking) {
        if (walking) {
            sense.flow().collect { l -> model.presence(l) }
        }
    }
    
    Scaffold(
        containerColor = ZenPalette.PaperWhite,
        floatingActionButton = {
            if (walking && model.self.value != null) {
                FloatingActionButton(
                    onClick = { model.self.value?.let { model.placeStone(it) } },
                    containerColor = ZenPalette.BambooGreen,
                    contentColor = ZenPalette.PaperWhite,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },
        bottomBar = {
            ZenControls(
                active = walking,
                onClick = { walking = !walking },
                onClear = { model.clearMind() },
                hasStones = model.stones.value.isNotEmpty()
            )
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .fillMaxSize()
        ) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "P A T H F I N D E R",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            // Circle of Balance
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircleOfBalance(
                    azimuth = align,
                    stones = model.stones.value,
                    me = model.self.value,
                    focus = model.focus.value,
                    range = model.aura.value,
                    onTap = { s -> model.medidateOn(model.stones.value.indexOf(s)) },
                    onZoom = { model.expandAura(it) }
                )
                
                // Focus Text
                model.focus.value?.let { idx ->
                    val s = model.stones.value.getOrNull(idx)
                    val m = model.self.value
                    if (s != null && m != null) {
                        val d = ZenMath.gap(m, s)
                        val v = ZenMath.vector(m, s)
                        Column(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${d.toInt()} meters", style = MaterialTheme.typography.displayLarge)
                            Text("${v.toInt()}° alignment", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            
            // Log (Scroll)
            if (model.stones.value.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .background(ZenPalette.Mist.copy(0.3f))
                ) {
                    itemsIndexed(model.stones.value) { i, s ->
                        val m = model.self.value
                        val d = if (m != null) ZenMath.gap(m, s).toInt().toString() + "m" else "-"
                        val active = model.focus.value == i
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { model.medidateOn(i) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Mark ${i+1}", 
                                color = if (active) ZenPalette.BambooGreen else ZenPalette.InkBlack,
                                fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                            Text(d, color = ZenPalette.StoneGrey)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircleOfBalance(
    azimuth: Float,
    stones: List<GuideStone>,
    me: Location?,
    focus: Int?,
    range: Float,
    onTap: (GuideStone) -> Unit,
    onZoom: (Float) -> Unit
) {
    Canvas(
        Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                val c = Offset(size.width / 2f, size.height / 2f)
                detectTapGestures { o ->
                    me?.let { m ->
                        stones.forEach { s ->
                            val r = (ZenMath.gap(m, s) / range) * (size.width / 2)
                            val b = Math.toRadians(ZenMath.vector(m, s).toDouble())
                            val bx = c.x + (r * sin(b)).toFloat()
                            val by = c.y - (r * cos(b)).toFloat()
                            
                            // Rotate tap check
                            val ang = Math.toRadians(-azimuth.toDouble())
                            val dx = o.x - c.x
                            val dy = o.y - c.y
                            val rx = c.x + (dx * cos(ang) - dy * sin(ang)).toFloat()
                            val ry = c.y + (dx * sin(ang) + dy * cos(ang)).toFloat()
                            
                            if (hypot(rx-bx, ry-by) < 60f) onTap(s)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
               detectTransformGestures { _, _, z, _ -> onZoom(range / z) }
            }
    ) {
        val c = center
        val rad = size.minDimension / 2
        
        rotate(azimuth) {
            // Enso Circle
            drawCircle(
                color = ZenPalette.StoneGrey,
                style = Stroke(width = 4f),
                radius = rad * 0.95f,
                center = c
            )
            
            // North
            drawLine(
                color = ZenPalette.KoiOrange,
                start = c,
                end = Offset(c.x, c.y - 40f),
                strokeWidth = 4f
            )
            
            me?.let { m ->
                stones.forEachIndexed { i, s ->
                    val d = ZenMath.gap(m, s)
                    val v = Math.toRadians(ZenMath.vector(m, s).toDouble())
                    val pr = (d / range) * rad
                    
                    if (pr <= rad) {
                        val px = c.x + (pr * sin(v)).toFloat()
                        val py = c.y - (pr * cos(v)).toFloat()
                        val active = focus == i
                        
                        drawCircle(
                            color = if (active) ZenPalette.BambooGreen else ZenPalette.StoneGrey,
                            radius = if (active) 12f else 8f,
                            center = Offset(px, py)
                        )
                        
                        if (active) {
                            drawCircle(
                                color = ZenPalette.BambooGreen,
                                radius = 20f,
                                style = Stroke(2f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }
                
                // Guide Line
                focus?.let { i ->
                    stones.getOrNull(i)?.let { s ->
                         val v = Math.toRadians(ZenMath.vector(m, s).toDouble())
                         val ex = c.x + (rad * 0.8f * sin(v)).toFloat()
                         val ey = c.y - (rad * 0.8f * cos(v)).toFloat()
                         
                         drawLine(
                             color = ZenPalette.BambooGreen.copy(0.5f),
                             start = c,
                             end = Offset(ex, ey),
                             strokeWidth = 2f
                         )
                    }
                }
            }
        }
    }
}

@Composable
fun ZenControls(
    active: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    hasStones: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasStones) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, null, tint = ZenPalette.StoneGrey)
            }
            Spacer(Modifier.width(32.dp))
        }
        
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) ZenPalette.StoneGrey else ZenPalette.InkBlack
            ),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (active) "P A U S E" else "W A L K")
        }
    }
}
