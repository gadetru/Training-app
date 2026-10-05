package com.mytrainingplan.app.data.remote

import com.mytrainingplan.app.data.remote.dto.CatalogResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Catálogo remoto (Paso 6 spec 006; envoltorio `CatalogResponse` del
 * spec 007: el JSON real es `{"count", "exercises"}`, no una lista).
 * Retrofit solo rellena Room (nunca pinta directo). Base jsDelivr con tag
 * fijo en `CatalogConfig` (paso 7); aquí solo la ruta relativa.
 */
interface ExerciseApi {
    @GET("api/{lang}/exercises.json")
    suspend fun getExercises(@Path("lang") lang: String): CatalogResponse
}
