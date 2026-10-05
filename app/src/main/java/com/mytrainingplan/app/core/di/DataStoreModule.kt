package com.mytrainingplan.app.core.di

import android.content.Context
import com.mytrainingplan.app.core.datastore.CatalogTagStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo DataStore (Paso 10 spec 006). Guarda el último tag aplicado.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideCatalogTagStore(@ApplicationContext ctx: Context): CatalogTagStore =
        CatalogTagStore(ctx)
}
