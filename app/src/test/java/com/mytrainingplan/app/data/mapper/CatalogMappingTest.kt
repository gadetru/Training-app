package com.mytrainingplan.app.data.mapper

import com.google.gson.Gson
import com.mytrainingplan.app.data.remote.dto.CatalogResponse
import com.mytrainingplan.app.data.remote.dto.ExerciseDto
import com.mytrainingplan.app.data.remote.dto.toDomain
import com.mytrainingplan.app.domain.model.ExerciseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Prueba de conversión del catálogo (Paso 9 spec 006; movida a su espejo
 * `data/mapper` en el paso 8 del spec 011, sin cambiar lógica).
 * DTO snake_case -> dominio -> entidad, sin tocar Android/Room.
 */
class CatalogMappingTest {

    @Test
    fun dto_snake_case_mapea_a_dominio_catalog() {
        val dto = ExerciseDto(
            slug = "barbell-curl",
            name = "Curl con barra",
            muscle = "biceps",
            bodyPart = "arms",
            equipment = "barbell",
            category = "strength",
            secondaryMuscles = listOf("forearms"),
            gifUrl = "https://cdn.jsdelivr.net/gh/f/cli/biceps/barbell-curl.gif"
        )

        val domain = dto.toDomain(now = 123L)

        assertEquals("biceps/barbell-curl", domain.id)
        assertEquals("arms", domain.bodyPart)
        assertEquals("https://cdn.jsdelivr.net/gh/f/cli/biceps/barbell-curl.gif", domain.gifUrl)
        assertEquals(listOf("forearms"), domain.secondaryMuscles)
        assertEquals(ExerciseSource.CATALOG, domain.source)
        assertEquals(123L, domain.updatedAt)
        assertFalse(domain.deleted)
    }

    /**
     * Entrada real del JSON del fork (spec 007): claves camelCase
     * (`bodyPart`, `secondaryMuscles`, `gifUrl`), `gifUrl` absoluta al
     * upstream y extras (`id`, `file`, `thumbUrl`) que Gson debe ignorar.
     */
    @Test
    fun json_real_del_fork_con_camelCase_mapea_a_dominio() {
        val json = """
            {
              "id": "abs/side-bridge-hip-abduction",
              "slug": "side-bridge-hip-abduction",
              "name": "Abducción de cadera en puente lateral",
              "muscle": "abs",
              "bodyPart": "core",
              "equipment": "bodyweight",
              "category": "strength",
              "secondaryMuscles": ["hamstrings", "abs"],
              "instructions": ["Adopta la postura inicial con buena alineación corporal."],
              "file": "abs/side-bridge-hip-abduction.gif",
              "gifUrl": "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main/abs/side-bridge-hip-abduction.gif",
              "thumbUrl": "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main/abs/side-bridge-hip-abduction.thumb.webp"
            }
        """.trimIndent()

        val domain = Gson().fromJson(json, ExerciseDto::class.java).toDomain(now = 5L)

        assertEquals("abs/side-bridge-hip-abduction", domain.id)
        assertEquals("core", domain.bodyPart)
        assertEquals(listOf("hamstrings", "abs"), domain.secondaryMuscles)
        assertEquals(
            "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main/abs/side-bridge-hip-abduction.gif",
            domain.gifUrl
        )
        // Spec 010: la entrada real trae `instructions` y llegan al dominio.
        assertEquals(
            listOf("Adopta la postura inicial con buena alineación corporal."),
            domain.instructions
        )
        assertEquals(ExerciseSource.CATALOG, domain.source)
    }

    /**
     * El `exercises.json` real es un objeto `{"count", "exercises"}`
     * (spec 007), no una lista directa.
     */
    @Test
    fun envoltorio_con_count_deserializa_lista_de_entradas() {
        val json = """
            {
              "count": 1,
              "exercises": [
                {
                  "id": "abs/bottoms-up",
                  "slug": "bottoms-up",
                  "name": "Fondo up",
                  "muscle": "abs",
                  "bodyPart": "core",
                  "equipment": "bodyweight",
                  "category": "strength",
                  "secondaryMuscles": [],
                  "instructions": [],
                  "file": "abs/bottoms-up.gif",
                  "gifUrl": "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main/abs/bottoms-up.gif",
                  "thumbUrl": "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main/abs/bottoms-up.thumb.webp"
                }
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, CatalogResponse::class.java)

        assertEquals(1, response.count)
        assertEquals(1, response.exercises.size)
        assertEquals("abs/bottoms-up", response.exercises[0].toDomain(now = 5L).id)
    }

    @Test
    fun dominio_a_entidad_conserva_source_catalog() {
        val domain = ExerciseDto(
            slug = "pull-up",
            name = "Dominadas",
            muscle = "lats",
            bodyPart = "back",
            equipment = "bodyweight",
            category = "strength"
        ).toDomain(now = 7L)

        val entity = domain.toEntity(now = 9L)

        assertEquals("lats/pull-up", entity.id)
        assertEquals("CATALOG", entity.source)
        assertEquals("back", entity.bodyPart)
        assertEquals(9L, entity.updatedAt)
        assertFalse(entity.deleted)
    }

    /**
     * Spec 010: las instrucciones sobreviven la ida y vuelta
     * DTO -> dominio -> entidad -> dominio, en orden y completas.
     */
    @Test
    fun instrucciones_sobreviven_ida_y_vuelta_dominio_entidad() {
        val steps = listOf(
            "De pie, agarra la barra con las manos a la anchura de los hombros.",
            "Flexiona los codos y sube la barra hasta los hombros sin balancear el cuerpo.",
            "Baja despacio hasta estirar los brazos del todo."
        )
        val domain = ExerciseDto(
            slug = "barbell-curl",
            name = "Curl con barra",
            muscle = "biceps",
            bodyPart = "arms",
            equipment = "barbell",
            category = "strength",
            secondaryMuscles = listOf("forearms"),
            instructions = steps
        ).toDomain(now = 11L)

        val roundTrip = domain.toEntity(now = 13L).toDomain()

        assertEquals(steps, roundTrip.instructions)
    }
}
