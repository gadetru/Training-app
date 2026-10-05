package com.mytrainingplan.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.catalogStore by preferencesDataStore(name = "catalog")

/**
 * Guarda el último tag de catálogo aplicado (Paso 7 spec 006).
 * La descarga (paso 8) solo corre si el tag fijo cambia; sin red se usa
 * lo local. Se inyectará con Hilt en el paso 10.
 */
class CatalogTagStore(private val context: Context) {

    fun observeTag(): Flow<String?> =
        context.catalogStore.data.map { it[KEY_TAG] }

    suspend fun saveTag(tag: String) {
        context.catalogStore.edit { it[KEY_TAG] = tag }
    }

    companion object {
        private val KEY_TAG = stringPreferencesKey("catalog_tag")
    }
}
