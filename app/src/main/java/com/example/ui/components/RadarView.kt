package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadarEcho
import com.example.model.RegionData
import kotlin.math.*

@Composable
fun InteractiveRadarDisplay(
    modifier: Modifier = Modifier,
    region: RegionData,
    timeOffset: Float,
    isPlaying: Boolean,
    overlayType: String, // "precipitation" (dBZ) or "temperature"
    precipScale: Float, // user interactive dial modifier
    tempOffset: Float // user interactive temperature offset modifier
) {
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var touchDetails by remember { mutableStateOf<String?>(null) }

    // Color definitions for DBZ scale
    val colorDbz15 = Color(0x334CAF50) // Soft Green
    val colorDbz30 = Color(0x884CAF50) // Rich Green
    val colorDbz40 = Color(0xAAFFEB3B) // Yellow
    val colorDbz50 = Color(0xCCFF9800) // Orange
    val colorDbz55 = Color(0xDDFF5722) // Red
    val colorDbz65 = Color(0xEE9C27B0) // Intense Purple (Severe Hail)

    // Color definitions for Temperature scale
    val tempColorCold = Color(0xFF00B0FF) // Blue
    val tempColorMild = Color(0xFF4CAF50) // Green
    val tempColorWarm = Color(0xFFFF9800) // Orange
    val tempColorHot = Color(0xFFF44336)  // Red

    // Concentric sweep scanning line glow angle
    val sweepAngleDegrees = (timeOffset * 360f) % 360f

    Box(
        modifier = modifier
            .testTag("radar_viewport")
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0F1D)) // Sophisticated deep radar slate
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(region) {
                    detectTapGestures(
                        onTap = { offset ->
                            touchOffset = offset
                        },
                        onPress = {
                            tryAwaitRelease()
                            // Clear on long intervals
                        }
                    )
                }
                .pointerInput(region) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            touchOffset = change.position
                        },
                        onDragEnd = {
                            touchOffset = null
                            touchDetails = null
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val maxRadius = min(width, height) / 2f

            // 1. Draw Radar Range Concentric Grid Lines
            val gridStroke = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Dynamic grid backing circles
            drawCircle(color = Color(0x0EFFFFFF), center = center, radius = maxRadius)
            drawCircle(color = Color(0x18FFFFFF), center = center, radius = maxRadius * 0.7f, style = gridStroke)
            drawCircle(color = Color(0x18FFFFFF), center = center, radius = maxRadius * 0.4f, style = gridStroke)

            // Draw axis lines crossing key areas
            drawLine(
                color = Color(0x12FFFFFF),
                start = Offset(0f, center.y),
                end = Offset(width, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x12FFFFFF),
                start = Offset(center.x, 0f),
                end = Offset(center.x, height),
                strokeWidth = 2f
            )

            // 2. Render overlays depending on selected mode
            if (overlayType == "precipitation") {
                // RENDER DYNAMIC WEATHER ECHOES
                val activeScale = 1.0f + precipScale

                region.radarEchoes.forEach { echo ->
                    // Drift echo location dynamically across its velocity loop profile
                    val currentDx = (echo.cx + echo.velocityX * timeOffset * 4f) % 1.0f
                    val currentDy = (echo.cy + echo.velocityY * timeOffset * 4f) % 1.0f

                    val echoCenter = Offset(currentDx * width, currentDy * height)
                    val baseRadius = echo.radius * maxRadius * activeScale

                    // Scale radar Dbz based on custom sliders
                    val adjustedDbz = (echo.dbz * activeScale).toInt().coerceIn(10, 75)

                    // Draw layered radial contours representing soft radar reflectivity blends
                    val echoColor = when {
                        adjustedDbz < 25 -> colorDbz15
                        adjustedDbz < 35 -> colorDbz30
                        adjustedDbz < 45 -> colorDbz40
                        adjustedDbz < 52 -> colorDbz50
                        adjustedDbz < 60 -> colorDbz55
                        else -> colorDbz65
                    }

                    // Draw outer aura
                    drawCircle(
                        color = echoColor.copy(alpha = echoColor.alpha * 0.15f),
                        center = echoCenter,
                        radius = baseRadius * 1.5f
                    )
                    // Regular core
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                echoColor.copy(alpha = 0.85f),
                                echoColor.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = echoCenter,
                            radius = baseRadius
                        ),
                        center = echoCenter,
                        radius = baseRadius
                    )
                    // Nested high-intensity peak storm nucleus
                    if (adjustedDbz >= 45) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            center = echoCenter,
                            radius = baseRadius * 0.25f
                        )
                    }
                }
            } else {
                // RENDER THERMAL PROFILE CONTOURS (ANIMATED TEMPERATURE PATTERNS)
                // Draw a beautiful soft regional thermal gradient that shifts with the user's thermometer slider
                val simulatedTemp = region.currentTemp + tempOffset
                val colorGradient = when {
                    simulatedTemp < 32f -> listOf(tempColorCold, tempColorCold.copy(alpha = 0.2f), Color.Transparent)
                    simulatedTemp < 65f -> listOf(tempColorMild, tempColorMild.copy(alpha = 0.2f), Color.Transparent)
                    simulatedTemp < 85f -> listOf(tempColorWarm, tempColorWarm.copy(alpha = 0.2f), Color.Transparent)
                    else -> listOf(tempColorHot, tempColorHot.copy(alpha = 0.2f), Color.Transparent)
                }

                // Wave pulse representing fluid heat fronts passing
                val pulseScale = 0.7f + 0.15f * sin(timeOffset * 2f * PI.toFloat())

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = colorGradient,
                        center = center,
                        radius = maxRadius * pulseScale
                    ),
                    center = center,
                    radius = maxRadius * pulseScale
                )
            }

            // 3. Draw Sweeping Circular Scan Line (The active beam!)
            val scanRadian = Math.toRadians(sweepAngleDegrees.toDouble()).toFloat()
            val sweepTarget = Offset(
                center.x + maxRadius * cos(scanRadian),
                center.y + maxRadius * sin(scanRadian)
            )

            // Sweep visual ray
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.55f),
                start = center,
                end = sweepTarget,
                strokeWidth = 3f
            )

            // Beautiful scan beam glowing tail path
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00FFCC).copy(alpha = 0.35f),
                        Color(0xFF00FFCC).copy(alpha = 0.05f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = sweepAngleDegrees - 40f,
                sweepAngle = 40f,
                useCenter = true
            )

            // Glow center focus
            drawCircle(
                color = Color(0xFF00FFCC),
                center = center,
                radius = 6f
            )
            drawCircle(
                color = Color(0xFF00FFCC).copy(alpha = 0.25f),
                center = center,
                radius = 12f,
                style = Stroke(2f)
            )

            // 4. Handle Touch-and-Drag Crosshairs tracking
            touchOffset?.let { touch ->
                // Ensure touch stays within constraints
                val dx = touch.x - center.x
                val dy = touch.y - center.y
                val dist = sqrt(dx * dx + dy * dy)

                val clampedTouch = if (dist <= maxRadius) {
                    touch
                } else {
                    Offset(
                        center.x + (dx / dist) * maxRadius,
                        center.y + (dy / dist) * maxRadius
                    )
                }

                // Draw high-tech horizontal & vertical cursor lines
                drawLine(
                    color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                    start = Offset(0f, clampedTouch.y),
                    end = Offset(width, clampedTouch.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                    start = Offset(clampedTouch.x, 0f),
                    end = Offset(clampedTouch.x, height),
                    strokeWidth = 1f
                )

                // Highlight touched spot
                drawCircle(
                    color = Color(0xFF00FFCC),
                    center = clampedTouch,
                    radius = 5f
                )
                drawCircle(
                    color = Color(0xFF00FFCC).copy(alpha = 0.3f),
                    center = clampedTouch,
                    radius = 14f,
                    style = Stroke(2f)
                )

                // Mathematically calculate mock radar response metrics based on location and selected echoes
                val relativeX = clampedTouch.x / width
                val relativeY = clampedTouch.y / height

                // Find if touch intersects any echo
                var highestDbz = 10
                if (overlayType == "precipitation") {
                    region.radarEchoes.forEach { echo ->
                        val currentDx = (echo.cx + echo.velocityX * timeOffset * 4f) % 1.0f
                        val currentDy = (echo.cy + echo.velocityY * timeOffset * 4f) % 1.0f
                        val ex = currentDx * width
                        val ey = currentDy * height
                        val tdx = clampedTouch.x - ex
                        val tdy = clampedTouch.y - ey
                        val tdist = sqrt(tdx * tdx + tdy * tdy)

                        val activeRadius = echo.radius * maxRadius * (1.0f + precipScale)
                        if (tdist <= activeRadius) {
                            val ratio = 1f - (tdist / activeRadius)
                            val localDbz = (10 + ratio * (echo.dbz * (1.0f + precipScale) - 10)).toInt()
                            if (localDbz > highestDbz) highestDbz = localDbz
                        }
                    }
                    val rainRate = if (highestDbz < 15) "No Rain"
                    else if (highestDbz < 30) "0.02 in/hr (Light)"
                    else if (highestDbz < 45) "0.15 in/hr (Mod)"
                    else if (highestDbz < 55) "0.55 in/hr (Heavy)"
                    else "1.80 in/hr (Extreme / Hail)"

                    touchDetails = "REFLECTIVITY: $highestDbz dBZ ($rainRate)"
                } else {
                    val distanceCenterFactor = 1f - (dist / maxRadius)
                    val locTemp = (region.currentTemp + tempOffset + (distanceCenterFactor * 8f) - 4f).toInt()
                    touchDetails = "THERMAL SENSOR: $locTemp°F"
                }
            }
        }

        // Radar Distance Grid Tags
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = "Live Radar Feed",
                color = Color(0xFF00FFCC),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopStart),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "150 mi",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            Text(
                text = "100 mi",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp)
            )

            // Active touched radar coordinates readout
            touchDetails?.let { details ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE60D1225))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Radar Info",
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = details,
                            color = Color.White,
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Bottom Radar DBZ Indicator Color Bar
            if (overlayType == "precipitation") {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD90D1225))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("dBZ: ", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                    Box(modifier = Modifier.size(8.dp).background(Color(0x334CAF50)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("20", color = Color.White, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xAAFFEB3B)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("35", color = Color.White, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xDDFF5722)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("50", color = Color.White, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xEE9C27B0)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("60+", color = Color.White, fontSize = 8.sp)
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD90D1225))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temp: ", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                    Box(modifier = Modifier.size(8.dp).background(tempColorCold))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Cold", color = Color.White, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(tempColorMild))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Mild", color = Color.White, fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).background(tempColorHot))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Hot", color = Color.White, fontSize = 8.sp)
                }
            }
        }
    }
}
