package com.example.dive.presentation

import com.example.dive.model.DiveMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 초 → "m:ss" */
internal fun formatClock(totalSec: Long): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

internal fun modeLabel(mode: DiveMode): String =
    if (mode == DiveMode.FREEDIVING) "프리다이빙" else "스쿠버"

internal fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(epochMs))
