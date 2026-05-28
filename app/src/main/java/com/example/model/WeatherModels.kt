package com.example.model

enum class WeatherCondition {
    SUNNY, CLOUDY, RAINY, SNOWY, STORMY
}

enum class AlertSeverity {
    INFO, WARNING, SEVERE
}

data class RadarEcho(
    val id: String,
    val cx: Float, // Normalized coordinates (0.0f to 1.0f) relative to radar center
    val cy: Float,
    val radius: Float, // Size of storm cell
    val dbz: Int, // Radar reflection intensity (15 to 65 dBZ)
    val velocityX: Float, // Drift velocity
    val velocityY: Float
)

data class WeatherAlert(
    val id: String,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val affectedRegion: String,
    val issuedTime: String
)

data class HourlyForecast(
    val hour: String,
    val temp: Float,
    val condition: WeatherCondition,
    val precipProb: Int // Percentage
)

data class RegionData(
    val id: String,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val currentTemp: Float,
    val condition: WeatherCondition,
    val humidity: Int, // Percentage
    val windSpeed: Float, // mph or km/h
    val pressure: Int, // hPa
    val radarEchoes: List<RadarEcho>,
    val alerts: List<WeatherAlert>,
    val hourlyForecasts: List<HourlyForecast>
)

data class AiWeatherBriefing(
    val localizedSummary: String,
    val severeThreatAssessment: String,
    val regionalImpactAdvice: String,
    val timestamp: String
)
