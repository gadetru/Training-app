package com.example.mytrainingplan.domain.model

/**
 * Perfil de atleta (Paso 1 spec 001).
 * Modelo limpio de dominio: sin dependencias Android / Room / Retrofit.
 * UI solo ve este modelo (mapping en data/mapper en pasos posteriores).
 */
enum class TrainingLevel(val displayName: String, val detail: String) {
    INICIAL("Inicial", "< 1 año"),
    INTERMEDIO("Intermedio", "1 - 3 años"),
    AVANZADO("Avanzado", "+3 años");

    companion object {
        val Default: TrainingLevel = INTERMEDIO
    }
}

enum class TrainingGoal(val displayName: String) {
    HIPERTROFIA("Hipertrofia"),
    FUERZA_POTENCIA("Fuerza & Potencia"),
    RESISTENCIA("Resistencia");

    companion object {
        val Default: TrainingGoal = FUERZA_POTENCIA
    }
}

data class Profile(
    // Singleton local "me" (ver Preguntas abiertas del spec).
    // Nota: AGENTS.md pide UUID para lo creado por usuario; aquí se usa
    // id fijo por ser perfil único local, no colección.
    val id: String = "me",
    val displayName: String = "Carlos Mendoza",
    val age: Int? = 28,
    val heightCm: Int? = 178,
    val weightKg: Double? = 78.5,
    val level: TrainingLevel = TrainingLevel.Default,
    val goal: TrainingGoal = TrainingGoal.Default,
    val avatarUri: String? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
) {
    val firstName: String
        get() = displayName.trim().split(" ").firstOrNull()
            ?.takeIf { it.isNotEmpty() } ?: "Atleta"

    val greeting: String
        get() = "Hola, $firstName"
}
