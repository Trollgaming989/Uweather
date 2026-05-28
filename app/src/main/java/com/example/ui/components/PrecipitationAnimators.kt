package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.model.WeatherCondition
import kotlin.random.Random

// Represents a standalone atmospheric weather particle (raindrop or snowflake)
private data class WeatherParticle(
    val xPercent: Float,
    val ySpeedMultiplier: Float,
    val size: Float,
    val opacity: Float,
    val horizontalDrift: Float
)

@Composable
fun AtmosphericParticleLayer(
    modifier: Modifier = Modifier,
    condition: WeatherCondition,
    intensityScale: Float = 1.0f // Slider dynamic adjustment modifier
) {
    if (condition == WeatherCondition.SUNNY) return

    val infiniteTransition = rememberInfiniteTransition(label = "precipitation_ticks")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Remember a fixed set of random seeds so particles stay stable but randomized
    val particles = remember(condition) {
        val count = when (condition) {
            WeatherCondition.CLOUDY -> 12
            WeatherCondition.RAINY -> 75
            WeatherCondition.SNOWY -> 60
            WeatherCondition.STORMY -> 110
            else -> 0
        }
        List(count) {
            WeatherParticle(
                xPercent = Random.nextFloat(),
                ySpeedMultiplier = Random.nextFloat() * 0.7f + 0.3f,
                size = Random.nextFloat() * 3.5f + 1.2f,
                opacity = Random.nextFloat() * 0.6f + 0.4f,
                horizontalDrift = Random.nextFloat() * 20f - 10f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val scaledParticles = particles.take((particles.size * intensityScale).coerceAtLeast(0f).toInt())

        scaledParticles.forEach { p ->
            // Calculate base Y position wrapped around screen bound and staggered
            val startingY = p.ySpeedMultiplier * height
            val currentY = (startingY + progress * height) % height

            // Calculate drift X displacement based on current height y-ratio
            val currentX = (p.xPercent * width + (currentY / height) * p.horizontalDrift) % width

            when (condition) {
                WeatherCondition.RAINY, WeatherCondition.STORMY -> {
                    // Draw raindrop streaks inclined slightly down-right
                    val length = (p.size * 5f).coerceAtLeast(8f)
                    val strokeWidth = p.size.coerceAtMost(2.5f)
                    val rainColor = if (condition == WeatherCondition.STORMY) {
                        Color(0xFFE0E6ED).copy(alpha = p.opacity) // Bleaker gray-cyan streaks
                    } else {
                        Color(0xFF8da1b9).copy(alpha = p.opacity * 0.9f) // Warm rainy blue
                    }

                    drawLine(
                        color = rainColor,
                        start = Offset(currentX, currentY),
                        end = Offset(currentX + (p.horizontalDrift * 0.2f), currentY + length),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                WeatherCondition.SNOWY -> {
                    // Draw soft fluffy snow bubbles
                    val radius = p.size * 1.5f
                    val snowColor = Color.White.copy(alpha = p.opacity * 0.95f)
                    
                    drawCircle(
                        color = snowColor,
                        center = Offset(currentX, currentY),
                        radius = radius
                    )
                }

                WeatherCondition.CLOUDY -> {
                    // Draw soft drifting clouds/fog puffs
                    val cloudRadius = p.size * 18f
                    val fogColor = Color.White.copy(alpha = p.opacity * 0.12f)
                    drawCircle(
                        color = fogColor,
                        center = Offset(currentX, currentY),
                        radius = cloudRadius
                    )
                }
                else -> {}
            }
        }
    }
}
