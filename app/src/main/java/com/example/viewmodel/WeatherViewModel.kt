package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.WeatherAiService
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val _regions = MutableStateFlow<List<RegionData>>(emptyList())
    val regions: StateFlow<List<RegionData>> = _regions.asStateFlow()

    private val _selectedRegion = MutableStateFlow<RegionData?>(null)
    val selectedRegion: StateFlow<RegionData?> = _selectedRegion.asStateFlow()

    private val _radarPlaying = MutableStateFlow(true)
    val radarPlaying: StateFlow<Boolean> = _radarPlaying.asStateFlow()

    private val _radarTimeOffset = MutableStateFlow(0f)
    val radarTimeOffset: StateFlow<Float> = _radarTimeOffset.asStateFlow()

    private val _activeOverlay = MutableStateFlow("precipitation") // "precipitation" or "temperature"
    val activeOverlay: StateFlow<String> = _activeOverlay.asStateFlow()

    private val _aiBriefing = MutableStateFlow<AiWeatherBriefing?>(null)
    val aiBriefing: StateFlow<AiWeatherBriefing?> = _aiBriefing.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    // Real-time custom simulation controls for user interaction
    private val _customTempSlider = MutableStateFlow(0f) // Offset to add/subtract
    val customTempSlider: StateFlow<Float> = _customTempSlider.asStateFlow()

    private val _customPrecipSlider = MutableStateFlow(0f) // Multiplier offset
    val customPrecipSlider: StateFlow<Float> = _customPrecipSlider.asStateFlow()

    init {
        initializeData()
        // Automatically start the time-progression simulation loop for the radar sweeper
        startRadarEngine()
    }

    private fun initializeData() {
        val defaultRegions = listOf(
            RegionData(
                id = "pac_northwest",
                name = "Pacific Northwest",
                centerLatitude = 47.6062,
                centerLongitude = -122.3321,
                currentTemp = 52f,
                condition = WeatherCondition.RAINY,
                humidity = 94,
                windSpeed = 12.5f,
                pressure = 1008,
                radarEchoes = listOf(
                    RadarEcho("pnw_1", 0.4f, 0.45f, 0.15f, 35, 0.02f, 0.01f),
                    RadarEcho("pnw_2", 0.62f, 0.35f, 0.22f, 25, 0.015f, -0.01f),
                    RadarEcho("pnw_3", 0.35f, 0.65f, 0.08f, 42, 0.01f, 0.02f)
                ),
                alerts = listOf(
                    WeatherAlert(
                        id = "alert_pnw",
                        severity = AlertSeverity.INFO,
                        title = "Coastal Flood Advisory",
                        message = "High tide cycle alongside persistent onshore moisture flows may trigger minor tidal pool flooding across lowland lanes.",
                        affectedRegion = "Puget Sound Shorelines",
                        issuedTime = "1:15 PM"
                    )
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("12:00", 50f, WeatherCondition.RAINY, 90),
                    HourlyForecast("13:00", 52f, WeatherCondition.RAINY, 85),
                    HourlyForecast("14:00", 52f, WeatherCondition.CLOUDY, 60),
                    HourlyForecast("15:00", 53f, WeatherCondition.CLOUDY, 40),
                    HourlyForecast("16:00", 54f, WeatherCondition.RAINY, 70),
                    HourlyForecast("17:00", 52f, WeatherCondition.RAINY, 95)
                )
            ),
            RegionData(
                id = "gulf_coast",
                name = "Gulf Coast Stormway",
                centerLatitude = 29.7604,
                centerLongitude = -95.3698,
                currentTemp = 86f,
                condition = WeatherCondition.STORMY,
                humidity = 88,
                windSpeed = 24.0f,
                pressure = 998,
                radarEchoes = listOf(
                    RadarEcho("gulf_1", 0.5f, 0.5f, 0.18f, 58, 0.03f, 0.03f), // Heavy storm core
                    RadarEcho("gulf_2", 0.55f, 0.42f, 0.28f, 48, 0.02f, 0.04f),
                    RadarEcho("gulf_3", 0.35f, 0.58f, 0.12f, 62, 0.04f, 0.02f) // Severe hail cell
                ),
                alerts = listOf(
                    WeatherAlert(
                        id = "alert_gulf_severe",
                        severity = AlertSeverity.SEVERE,
                        title = "Severe Thunderstorm Warning",
                        message = "Dynamic supercell storm carrying destructive windgusts up to 65 mph, intense lightning, and localized high-density hail.",
                        affectedRegion = "Galveston & Houston Metro",
                        issuedTime = "4:45 PM"
                    ),
                    WeatherAlert(
                        id = "alert_gulf_marine",
                        severity = AlertSeverity.WARNING,
                        title = "Special Marine Warning",
                        message = "Severe squall lines producing dangerous water spouts, churning surf conditions, and extremely low navigation visibilities.",
                        affectedRegion = "Coastal Offshore Waters",
                        issuedTime = "4:50 PM"
                    )
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("12:00", 84f, WeatherCondition.STORMY, 95),
                    HourlyForecast("13:00", 86f, WeatherCondition.STORMY, 90),
                    HourlyForecast("14:00", 85f, WeatherCondition.RAINY, 80),
                    HourlyForecast("15:00", 82f, WeatherCondition.CLOUDY, 50),
                    HourlyForecast("16:00", 80f, WeatherCondition.STORMY, 85),
                    HourlyForecast("17:00", 79f, WeatherCondition.STORMY, 90)
                )
            ),
            RegionData(
                id = "midwest_plains",
                name = "Midwest Plains",
                centerLatitude = 39.0997,
                centerLongitude = -94.5786,
                currentTemp = 74f,
                condition = WeatherCondition.RAINY,
                humidity = 82,
                windSpeed = 18.2f,
                pressure = 1004,
                radarEchoes = listOf(
                    RadarEcho("mwp_1", 0.48f, 0.40f, 0.25f, 45, -0.01f, 0.03f),
                    RadarEcho("mwp_2", 0.25f, 0.30f, 0.15f, 38, -0.01f, 0.02f)
                ),
                alerts = listOf(
                    WeatherAlert(
                        id = "alert_mwp_flood",
                        severity = AlertSeverity.WARNING,
                        title = "Flash Flood Warning",
                        message = "Continuous convective rain trains across saturated soils are sparking immediate stream overflow and lowland water ponding.",
                        affectedRegion = "Shawnee County Basin",
                        issuedTime = "3:30 PM"
                    )
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("12:00", 72f, WeatherCondition.CLOUDY, 45),
                    HourlyForecast("13:00", 74f, WeatherCondition.RAINY, 80),
                    HourlyForecast("14:00", 75f, WeatherCondition.RAINY, 95),
                    HourlyForecast("15:00", 73f, WeatherCondition.RAINY, 90),
                    HourlyForecast("16:00", 70f, WeatherCondition.RAINY, 85),
                    HourlyForecast("17:00", 68f, WeatherCondition.CLOUDY, 50)
                )
            ),
            RegionData(
                id = "new_england",
                name = "New England Slopes",
                centerLatitude = 44.2700,
                centerLongitude = -71.3031,
                currentTemp = 28f,
                condition = WeatherCondition.SNOWY,
                humidity = 89,
                windSpeed = 16.5f,
                pressure = 1002,
                radarEchoes = listOf(
                    RadarEcho("ne_1", 0.42f, 0.48f, 0.30f, 30, 0.01f, 0.01f), // Snowband
                    RadarEcho("ne_2", 0.58f, 0.55f, 0.20f, 22, 0.008f, 0.012f)
                ),
                alerts = listOf(
                    WeatherAlert(
                        id = "alert_ne_winter",
                        severity = AlertSeverity.SEVERE,
                        title = "Winter Storm Warning",
                        message = "Heavy lake-effect snow bands merging with high northern winds. Visibility under 1/4 mile with sudden whiteouts likely.",
                        affectedRegion = "White Mountains & Uplands",
                        issuedTime = "2:10 PM"
                    )
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("12:00", 29f, WeatherCondition.SNOWY, 95),
                    HourlyForecast("13:00", 28f, WeatherCondition.SNOWY, 90),
                    HourlyForecast("14:00", 27f, WeatherCondition.SNOWY, 90),
                    HourlyForecast("15:00", 26f, WeatherCondition.CLOUDY, 60),
                    HourlyForecast("16:00", 24f, WeatherCondition.SNOWY, 80),
                    HourlyForecast("17:00", 22f, WeatherCondition.SNOWY, 95)
                )
            ),
            RegionData(
                id = "sw_desert",
                name = "Southwest Arid Plains",
                centerLatitude = 33.4484,
                centerLongitude = -112.0740,
                currentTemp = 98f,
                condition = WeatherCondition.SUNNY,
                humidity = 12,
                windSpeed = 6.0f,
                pressure = 1016,
                radarEchoes = emptyList(),
                alerts = emptyList(),
                hourlyForecasts = listOf(
                    HourlyForecast("12:00", 94f, WeatherCondition.SUNNY, 0),
                    HourlyForecast("13:00", 98f, WeatherCondition.SUNNY, 0),
                    HourlyForecast("14:00", 101f, WeatherCondition.SUNNY, 0),
                    HourlyForecast("15:00", 102f, WeatherCondition.SUNNY, 0),
                    HourlyForecast("16:00", 100f, WeatherCondition.SUNNY, 0),
                    HourlyForecast("17:00", 97f, WeatherCondition.SUNNY, 5)
                )
            )
        )

        _regions.value = defaultRegions
        selectRegion(defaultRegions.first())
    }

    fun selectRegion(region: RegionData) {
        _selectedRegion.value = region
        // Reset custom interactive simulation offsets when switching regions
        _customTempSlider.value = 0f
        _customPrecipSlider.value = 0f
        fetchBriefingForRegion(region)
    }

    fun toggleRadarPlaying() {
        _radarPlaying.value = !_radarPlaying.value
    }

    fun setActiveOverlay(overlay: String) {
        _activeOverlay.value = overlay
    }

    fun adjustSimulationParams(tempOffset: Float, precipScale: Float) {
        _customTempSlider.value = tempOffset
        _customPrecipSlider.value = precipScale

        // Optionally trigger real-time AI briefing recalculations or alert updates based on simulation adjustments
        val current = _selectedRegion.value ?: return
        // Recalculate if threshold transitions occur
        val simulatedTemp = current.currentTemp + tempOffset
        val simulatedPrecip = (1f + precipScale)

        // Find alert scenarios
        val alertList = current.alerts.toMutableList()
        if (simulatedTemp > 100f && current.id == "sw_desert") {
            if (alertList.none { it.id == "sim_excessive_heat" }) {
                alertList.add(
                    WeatherAlert(
                        "sim_excessive_heat",
                        AlertSeverity.SEVERE,
                        "Simulated Extreme Heat Advisory",
                        "Continuous high-pressure caps are pushing regional temperatures limits past 105°F. Maintain active rehydration.",
                        current.name,
                        "实时"
                    )
                )
            }
        } else {
            alertList.removeAll { it.id == "sim_excessive_heat" }
        }

        if (simulatedTemp < 32f && current.id != "new_england" && current.id != "sw_desert") {
            if (alertList.none { it.id == "sim_freeze_warn" }) {
                alertList.add(
                    WeatherAlert(
                        "sim_freeze_warn",
                        AlertSeverity.WARNING,
                        "Simulated Freeze Framework Warning",
                        "Atmospheric drops are freezing surface structures. Black ice formations possible across high-altitude roads.",
                        current.name,
                        "实时"
                    )
                )
            }
        } else {
            alertList.removeAll { it.id == "sim_freeze_warn" }
        }

        // Dynamically reflect changes
        _selectedRegion.value = current.copy(
            alerts = alertList
        )
    }

    fun refreshBriefing() {
        _selectedRegion.value?.let { fetchBriefingForRegion(it) }
    }

    private fun fetchBriefingForRegion(region: RegionData) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            val alertStr = if (region.alerts.isEmpty()) "No active weather threats" 
                          else region.alerts.joinToString("; ") { "${it.title}: ${it.message}" }

            val simulatedTemp = region.currentTemp + _customTempSlider.value
            val simulatedPrecip = if (region.radarEchoes.isEmpty()) "None" else "${region.radarEchoes.size} echoes active"

            val simulatedCondition = when {
                simulatedTemp < 32f && region.condition == WeatherCondition.RAINY -> WeatherCondition.SNOWY
                simulatedTemp > 80f && _customPrecipSlider.value > 0.5f -> WeatherCondition.STORMY
                else -> region.condition
            }

            val briefing = WeatherAiService.getAlertBriefing(
                regionName = region.name,
                temp = simulatedTemp,
                conditionName = simulatedCondition.name,
                humidity = region.humidity,
                windSpeed = region.windSpeed,
                echoCount = region.radarEchoes.size,
                activeAlerts = alertStr
            )
            _aiBriefing.value = briefing
            _isLoadingAi.value = false
        }
    }

    private fun startRadarEngine() {
        viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(16) // ~60fps smooth progression ticking
                if (_radarPlaying.value) {
                    val currentOffset = _radarTimeOffset.value
                    // Slowly increment offset and wrap around beautifully between 0.0 and 1.0
                    val nextOffset = (currentOffset + 0.005f) % 1.0f
                    _radarTimeOffset.value = nextOffset
                }
            }
        }
    }
}
