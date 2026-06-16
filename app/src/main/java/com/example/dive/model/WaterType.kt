package com.example.dive.model

/** 수질에 따른 수심 환산 계수(1 hPa 당 m) */
enum class WaterType(val metersPerHpa: Float, val label: String) {
    FRESH(0.010197f, "담수"),
    SEA(0.00995f, "해수")
}
