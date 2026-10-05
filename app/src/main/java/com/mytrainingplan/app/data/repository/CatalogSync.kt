package com.mytrainingplan.app.data.repository

import android.util.Log
import com.mytrainingplan.app.core.datastore.CatalogTagStore
import com.mytrainingplan.app.core.network.CatalogConfig
import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.data.remote.ExerciseApi
import com.mytrainingplan.app.data.remote.dto.toDomain
import java.io.IOException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

/**
 * Sincronización del catálogo (Paso 8 spec 006).
 * - Solo corre si el tag fijo cambió (DataStore).
 * - Solo toca filas `source = CATALOG`; los propios (`CUSTOM`) nunca se
 *   pisan (upsert por PK: ids `músculo/slug` vs UUID, sin colisión; las
 *   bajas de catálogo se marcan `deleted`, las CUSTOM ni se listan).
 * - Sin red se usa lo local: `IOException` sale en silencio y no se guarda
 *   el tag (reintenta en el próximo arranque).
 * - Tag inexistente (HTTP 404) u otra respuesta inesperada: se avisa por
 *   log y tampoco se guarda el tag (nunca silencio total).
 */
class CatalogSync(
    private val api: ExerciseApi,
    private val dao: ExerciseDao,
    private val tags: CatalogTagStore
) {
    suspend fun syncIfNeeded(lang: String = CatalogConfig.DEFAULT_LANG) {
        val applied = tags.observeTag().first()
        if (applied == CatalogConfig.CATALOG_TAG) return
        val remote = try {
            api.getExercises(lang).exercises
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Log.w(TAG, "Catálogo: tag ${CatalogConfig.CATALOG_TAG} inexistente (404) en api/$lang/exercises.json; revisa el tag del fork, no se guarda tag")
            } else {
                Log.w(TAG, "Catálogo: HTTP ${e.code()} al descargar $lang, no se guarda tag", e)
            }
            return // HTTP: sigue con lo local, sin crash, sin guardar tag
        } catch (_: IOException) {
            return // sin red: sigue con lo local, sin crash, reintenta al próximo arranque
        } catch (e: Exception) {
            Log.w(TAG, "Catálogo: respuesta inesperada al descargar $lang, no se guarda tag", e)
            return // p. ej. JSON con otra forma: visible en logs, sin guardar tag
        }
        val now = System.currentTimeMillis()
        val catalog = remote.map { it.toDomain(now).toEntity(now) }
        dao.upsertAll(catalog)
        // Bajas: CATALOG local que ya no viene -> borrado lógico.
        // CUSTOM intactos (ni se consultan para borrado).
        val incomingIds = catalog.map { it.id }.toSet()
        val stale = dao.observeBySource("CATALOG").first()
            .filter { it.id !in incomingIds && !it.deleted }
        stale.forEach { dao.upsert(it.copy(deleted = true, updatedAt = now)) }
        tags.saveTag(CatalogConfig.CATALOG_TAG)
    }

    companion object {
        private const val TAG = "CatalogSync"
    }
}
