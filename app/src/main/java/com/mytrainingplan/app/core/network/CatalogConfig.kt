package com.mytrainingplan.app.core.network

/**
 * Config del catálogo (Paso 7 spec 006).
 * Fork por jsDelivr con **tag fijo**, nunca rama. Solo URLs de GIF en DB
 * (Coil cachea). Al taggear el fork `gadetru` se cambia SOLO este fichero.
 *
 * Nota: el fake de Fase A apunta al upstream
 * `JahelCuadrado/ExerciseGymGifsDB@v1.1.0` porque el fork aún no tiene tag
 * (ver `FakeExerciseRepository.CATALOG_BASE`); en cuanto exista el tag del
 * fork, este `CATALOG_TAG` lo pineará.
 */
object CatalogConfig {
    /** Tag fijo pineado (no usar ramas ni `latest`). */
    const val CATALOG_TAG = "v1.1.0"

    /** Base del fork sin tag (el tag se añade en [baseUrl]). */
    const val CATALOG_REPO = "https://cdn.jsdelivr.net/gh/gadetru/ExerciseGymGifsDB"

    /** Idioma por defecto del JSON (`api/{lang}/exercises.json`). */
    const val DEFAULT_LANG = "es"

    /** Base Retrofit (debe terminar en `/`). */
    fun baseUrl(): String = "$CATALOG_REPO@$CATALOG_TAG/"
}
