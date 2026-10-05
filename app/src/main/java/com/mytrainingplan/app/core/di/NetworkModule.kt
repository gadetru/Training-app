package com.mytrainingplan.app.core.di

import com.mytrainingplan.app.core.network.CatalogConfig
import com.mytrainingplan.app.data.remote.ExerciseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Módulo red (Paso 10 spec 006). Retrofit solo rellena Room vía `CatalogSync`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(CatalogConfig.baseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideExerciseApi(retrofit: Retrofit): ExerciseApi =
        retrofit.create(ExerciseApi::class.java)
}
