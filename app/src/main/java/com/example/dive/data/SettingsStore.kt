package com.example.dive.data

import android.content.Context
import com.example.dive.model.WaterType

/** 앱 설정을 SharedPreferences에 저장 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("dive_settings", Context.MODE_PRIVATE)

    var waterType: WaterType
        get() = runCatching {
            WaterType.valueOf(prefs.getString(KEY_WATER_TYPE, WaterType.SEA.name)!!)
        }.getOrDefault(WaterType.SEA)
        set(value) {
            prefs.edit().putString(KEY_WATER_TYPE, value.name).apply()
        }

    private companion object {
        const val KEY_WATER_TYPE = "water_type"
    }
}
