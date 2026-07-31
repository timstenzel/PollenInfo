package ch.stenzel.tim.polleninfo.feature.example.domain.model

enum class PollenLevel { NONE, LOW, MODERATE, HIGH, VERY_HIGH }

fun Float?.toPollenLevel(): PollenLevel = when {
    this == null || this <= 0f -> PollenLevel.NONE
    this < 10f -> PollenLevel.LOW
    this < 50f -> PollenLevel.MODERATE
    this < 200f -> PollenLevel.HIGH
    else -> PollenLevel.VERY_HIGH
}
