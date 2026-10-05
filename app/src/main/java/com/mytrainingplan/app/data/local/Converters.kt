package com.mytrainingplan.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

/**
 * Conversores Room (Paso 3 spec 006).
 * `List<String> <-> JSON` para `secondaryMuscles` (ver
 * `docs/MODELO_DE_DATOS.md`). Usa `org.json` del SDK, sin dependencia nueva.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String =
        JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val arr = JSONArray(value)
        return List(arr.length()) { i -> arr.optString(i) }
    }
}
