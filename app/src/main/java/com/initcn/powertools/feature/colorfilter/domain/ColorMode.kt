package com.initcn.powertools.feature.colorfilter.domain

enum class ColorMode(val value: Int) {
    OFF(-1),
    GRAYSCALE(0),
    PROTANOMALY(11),   // Red-Green (Protanopia)
    DEUTERANOMALY(12), // Green-Red (Deuteranopia)
    TRITANOMALY(13);   // Blue-Yellow (Tritanopia)

    companion object {
        fun fromValue(value: Int): ColorMode {
            return entries.find { it.value == value } ?: OFF
        }
    }
}