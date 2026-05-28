package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertSeverity
import com.example.model.RegionData
import com.example.model.WeatherCondition
import com.example.ui.components.AtmosphericParticleLayer
import com.example.ui.components.InteractiveRadarDisplay
import com.example.ui.theme.*
import com.example.viewmodel.WeatherViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboard(
    viewModel: WeatherViewModel
) {
    val regions by viewModel.regions.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val radarPlaying by viewModel.radarPlaying.collectAsState()
    val radarTimeOffset by viewModel.radarTimeOffset.collectAsState()
    val activeOverlay by viewModel.activeOverlay.collectAsState()
    val aiBriefing by viewModel.aiBriefing.collectAsState()
    val isLoadingAi by viewModel.isLoadingAi.collectAsState()

    val customTempSlider by viewModel.customTempSlider.collectAsState()
    val customPrecipSlider by viewModel.customPrecipSlider.collectAsState()

    // Highly polished backdrop supporting active state tints with elegant dark `#0F1113` base layers
    val currentThemeGradient = remember(selectedRegion, customTempSlider) {
        val region = selectedRegion ?: return@remember Brush.verticalGradient(listOf(ElegantDarkBg, ElegantDarkSurface))
        val simulatedTemp = region.currentTemp + customTempSlider

        when {
            simulatedTemp < 32f -> {
                // Icy pristine silver/teal theme
                Brush.verticalGradient(listOf(ElegantDarkBg, Color(0xFF0F1E36)))
            }
            region.condition == WeatherCondition.STORMY -> {
                // Severe deep dark indigo stormy theme
                Brush.verticalGradient(listOf(ElegantDarkBg, Color(0xFF260002)))
            }
            region.condition == WeatherCondition.RAINY -> {
                // Sleek moody oceanic slate blues
                Brush.verticalGradient(listOf(ElegantDarkBg, Color(0xFF0A1526)))
            }
            region.condition == WeatherCondition.CLOUDY -> {
                // Soft elegant moody silver-grays
                Brush.verticalGradient(listOf(ElegantDarkBg, Color(0xFF16181A)))
            }
            else -> {
                // Beautiful sunlit dynamic warm amber values
                Brush.verticalGradient(listOf(ElegantDarkBg, Color(0xFF161C24)))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentThemeGradient)
    ) {
        // Dynamic Ambient Particles Background Renderer (Rain, Snow, Cloud drifting)
        selectedRegion?.let { region ->
            val simulatedTemp = region.currentTemp + customTempSlider
            val activeCondition = when {
                simulatedTemp < 32f && region.condition == WeatherCondition.RAINY -> WeatherCondition.SNOWY
                simulatedTemp > 80f && customPrecipSlider > 0.4f -> WeatherCondition.STORMY
                else -> region.condition
            }

            AtmosphericParticleLayer(
                modifier = Modifier.fillMaxSize(),
                condition = activeCondition,
                intensityScale = 1.0f + customPrecipSlider
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "METEOROLOGY RADAR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElegantDarkAccent
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ElegantDarkBg.copy(alpha = 0.85f),
                        titleContentColor = ElegantDarkText,
                        navigationIconContentColor = ElegantDarkAccent,
                        actionIconContentColor = ElegantDarkText
                    ),
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Weather Icon",
                            tint = ElegantDarkAccent,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshBriefing() },
                            modifier = Modifier.testTag("action_refresh_ai")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Refresh Advisory",
                                tint = ElegantDarkAccent
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            selectedRegion?.let { region ->
                val simulatedTemp = (region.currentTemp + customTempSlider).toInt()
                val currentConditionText = when {
                    simulatedTemp < 32f && region.condition == WeatherCondition.RAINY -> "Inclement Snow"
                    simulatedTemp > 82f && region.condition == WeatherCondition.STORMY -> "Severe Thunderstorms"
                    else -> when (region.condition) {
                        WeatherCondition.SUNNY -> "Clear Skies"
                        WeatherCondition.CLOUDY -> "Heavy Overcast"
                        WeatherCondition.RAINY -> "Showers"
                        WeatherCondition.SNOWY -> "Snowfall"
                        WeatherCondition.STORMY -> "Convective Storms"
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // 1. Regional Switcher (Dynamic Tab Navigation Row)
                    item {
                        Column {
                            Text(
                                "SELECT MONITORING ZONE",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElegantDarkMuted,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(regions) { r ->
                                    val isSelected = r.id == region.id
                                    val tabBg = if (isSelected) ElegantDarkAccent else ElegantDarkSurfaceSubtle
                                    val tabText = if (isSelected) ElegantDarkLiveText else ElegantDarkText
                                    val borderStroke = if (isSelected) null else BorderStroke(1.dp, ElegantDarkSurfaceBorder)

                                    Box(
                                        modifier = Modifier
                                            .testTag("region_selector_${r.id}")
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(tabBg)
                                            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(20.dp)) else Modifier)
                                            .clickable { viewModel.selectRegion(r) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (r.condition) {
                                                    WeatherCondition.SUNNY -> Icons.Default.WbSunny
                                                    WeatherCondition.CLOUDY -> Icons.Default.CloudQueue
                                                    WeatherCondition.RAINY -> Icons.Default.WaterDrop
                                                    WeatherCondition.SNOWY -> Icons.Default.AcUnit
                                                    WeatherCondition.STORMY -> Icons.Default.Thunderstorm
                                                },
                                                contentDescription = r.name,
                                                tint = tabText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = r.name,
                                                color = tabText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Active Region Metrics Header Board
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                            border = BorderStroke(1.dp, ElegantDarkSurfaceSubtle),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = region.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ElegantDarkText
                                        )
                                        Text(
                                            text = currentConditionText.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElegantDarkAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "$simulatedTemp°",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Light,
                                        color = ElegantDarkText
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = ElegantDarkSurfaceSubtle, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WeatherMetricItem(
                                        icon = Icons.Default.Air,
                                        label = "WIND",
                                        value = "${region.windSpeed} mph",
                                        accent = ElegantDarkAccent,
                                        progress = (region.windSpeed.toFloat() / 30f).coerceIn(0f, 1f),
                                        isWind = true
                                    )
                                    WeatherMetricItem(
                                        icon = Icons.Default.Water,
                                        label = "HUMIDITY",
                                        value = "${region.humidity}%",
                                        accent = ElegantDarkAccent,
                                        progress = region.humidity.toFloat() / 100f,
                                        isWind = false
                                    )
                                    WeatherMetricItem(
                                        icon = Icons.Default.Compress,
                                        label = "UV INDEX",
                                        value = "3 Low",
                                        accent = ElegantDarkAccent,
                                        progress = 0.3f,
                                        isWind = false
                                    )
                                }
                            }
                        }
                    }

                    // 3. Central Interactive Live Radar Screen Component
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                            border = BorderStroke(1.dp, ElegantDarkSurfaceSubtle),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                InteractiveRadarDisplay(
                                    modifier = Modifier.fillMaxWidth(),
                                    region = region,
                                    timeOffset = radarTimeOffset,
                                    isPlaying = radarPlaying,
                                    overlayType = activeOverlay,
                                    precipScale = customPrecipSlider,
                                    tempOffset = customTempSlider
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Controls overlay configuration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play/Pause caps
                                    Button(
                                        onClick = { viewModel.toggleRadarPlaying() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (radarPlaying) ElegantDarkSurfaceSubtle else ElegantDarkAccent,
                                            contentColor = if (radarPlaying) ElegantDarkAccent else ElegantDarkLiveText
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("radar_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (radarPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (radarPlaying) "Pause Radar" else "Play Radar"
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (radarPlaying) "SWEEPING..." else "RESUME",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Switch Layers
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ElegantDarkSurfaceSubtle)
                                            .border(1.dp, ElegantDarkSurfaceBorder, RoundedCornerShape(10.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clickable { viewModel.setActiveOverlay("precipitation") }
                                                .background(if (activeOverlay == "precipitation") ElegantDarkAccentSecondary else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "PRECIP",
                                                color = if (activeOverlay == "precipitation") ElegantDarkAccent else ElegantDarkMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clickable { viewModel.setActiveOverlay("temperature") }
                                                .background(if (activeOverlay == "temperature") ElegantDarkAccentSecondary else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "THERMAL",
                                                color = if (activeOverlay == "temperature") ElegantDarkAccent else ElegantDarkMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Interactive Atmospheric Parameters Customizer
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                            border = BorderStroke(1.dp, ElegantDarkSurfaceSubtle),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ATMOSPHERIC SIMULATION DIALS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElegantDarkAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Temperature Shift Slider
                                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Thermal Shift: ${if (customTempSlider >= 0) "+" else ""}${customTempSlider.toInt()}°F",
                                            color = ElegantDarkText,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            "Result: ${simulatedTemp}°F",
                                            color = ElegantDarkMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Slider(
                                        value = customTempSlider,
                                        onValueChange = { viewModel.adjustSimulationParams(it, customPrecipSlider) },
                                        valueRange = -30f..30f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElegantDarkAccent,
                                            activeTrackColor = ElegantDarkAccent,
                                            inactiveTrackColor = ElegantDarkSurfaceSubtle
                                        ),
                                        modifier = Modifier
                                            .height(24.dp)
                                            .testTag("temp_slider")
                                    )
                                }

                                // Precipitation Intensity Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Reflectivity Gain: x${"%.1f".format(1f + customPrecipSlider)}",
                                            color = ElegantDarkText,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            "Adjust Density",
                                            color = ElegantDarkMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Slider(
                                        value = customPrecipSlider,
                                        onValueChange = { viewModel.adjustSimulationParams(customTempSlider, it) },
                                        valueRange = -0.5f..1.5f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElegantDarkAccent,
                                            activeTrackColor = ElegantDarkAccent,
                                            inactiveTrackColor = ElegantDarkSurfaceSubtle
                                        ),
                                        modifier = Modifier
                                            .height(24.dp)
                                            .testTag("precip_slider")
                                    )
                                }
                            }
                        }
                    }

                    // 5. Active Scientific Severe Weather Alerts Segment
                    if (region.alerts.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "ACTIVE LIVE SEVERE THREAT METADATA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElegantDarkWarningText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                region.alerts.forEach { alert ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = ElegantDarkWarningBg),
                                        border = BorderStroke(1.dp, ElegantDarkWarningBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(ElegantDarkWarningText),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Alert Symbol",
                                                    tint = Color(0xFF690005),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = alert.title,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElegantDarkWarningText,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = alert.message,
                                                    color = ElegantDarkWarningSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Read Alert Details",
                                                tint = ElegantDarkWarningSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Gemini-powered Chief Meteorologist Warning Advisory Briefing
                    item {
                        Column {
                            Text(
                                "GEMINI METEOROLOGICAL BRIEFING",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantDarkAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_briefing_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                                border = BorderStroke(1.dp, ElegantDarkAccentSecondary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "AI Action",
                                                tint = ElegantDarkAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Severe Command Analysis",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ElegantDarkText
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ElegantDarkLiveBg)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isLoadingAi) "ANALYZING" else "LIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElegantDarkLiveText,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isLoadingAi) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(
                                                color = ElegantDarkAccent,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Polling high-definition radar arrays...",
                                                color = ElegantDarkMuted,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        val briefing = aiBriefing
                                        if (briefing != null) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                AiSectionItem(
                                                    title = "CONVECTIVE SUMMARY & RADAR SCAN",
                                                    text = briefing.localizedSummary,
                                                    icon = Icons.Default.Radar,
                                                    iconColor = ElegantDarkAccent
                                                )
                                                AiSectionItem(
                                                    title = "METEOROLOGICAL RISK RATING",
                                                    text = briefing.severeThreatAssessment,
                                                    icon = Icons.Default.Whatshot,
                                                    iconColor = ElegantDarkWarningText
                                                )
                                                AiSectionItem(
                                                    title = "TACTICAL SAFETY PROTOCOLS",
                                                    text = briefing.regionalImpactAdvice,
                                                    icon = Icons.Default.Shield,
                                                    iconColor = ElegantDarkAccent
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Briefing network offline. Press the refresh magic star icon above to prompt severe commanding systems.",
                                                color = ElegantDarkMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Regional Temperature Prediction (Hourly forecast scroll)
                    item {
                        Column {
                            Text(
                                "TEMPORAL FORECAST BARWAYS",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElegantDarkMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(region.hourlyForecasts) { f ->
                                    val simulatedHourlyTemp = (f.temp + customTempSlider).toInt()

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceSubtle),
                                        border = BorderStroke(1.dp, ElegantDarkSurfaceBorder),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                                .width(55.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(f.hour, fontSize = 11.sp, color = ElegantDarkMuted)
                                            Icon(
                                                imageVector = when (f.condition) {
                                                    WeatherCondition.SUNNY -> Icons.Default.WbSunny
                                                    WeatherCondition.CLOUDY -> Icons.Default.Cloud
                                                    WeatherCondition.RAINY -> Icons.Default.WaterDrop
                                                    WeatherCondition.SNOWY -> Icons.Default.AcUnit
                                                    WeatherCondition.STORMY -> Icons.Default.Thunderstorm
                                                },
                                                contentDescription = f.condition.name,
                                                tint = ElegantDarkAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "$simulatedHourlyTemp°",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ElegantDarkText
                                            )
                                            
                                            if (f.precipProb > 0) {
                                                Text(
                                                    text = "${f.precipProb}%",
                                                    fontSize = 9.sp,
                                                    color = ElegantDarkAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    progress: Float,
    isWind: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ElegantDarkSurfaceSubtle)
            .border(1.dp, ElegantDarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            color = ElegantDarkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = ElegantDarkText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isWind) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Direction",
                    tint = ElegantDarkAccent,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "NW",
                    color = ElegantDarkAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(ElegantDarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(ElegantDarkAccent)
                )
            }
        }
    }
}

@Composable
fun AiSectionItem(
    title: String,
    text: String,
    icon: ImageVector,
    iconColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = iconColor,
                letterSpacing = 0.8.sp
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            color = ElegantDarkText,
            lineHeight = 15.sp
        )
    }
}
