package com.mytrainingplan.app

import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.data.remote.dto.ExerciseDto
import com.mytrainingplan.app.data.remote.dto.toDomain
import com.mytrainingplan.app.domain.model.ExerciseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Prueba de conversión del catálogo (Paso 9 spec 006).
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
}
