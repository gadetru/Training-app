package com.mytrainingplan.app.core.di

import android.content.Context
import androidx.room.Room
import com.mytrainingplan.app.data.local.AppDatabase
import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.PlannedSetDao
import com.mytrainingplan.app.data.local.dao.ProfileDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.RoutineExerciseDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo DB (Paso 10 spec 006). Sin `fallbackToDestructiveMigration`
 * (solo desarrollo): v1 arranca en vacío y las migraciones futuras serán
 * reales con versión + `Migration`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "training.db").build()

    @Provides fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideRoutineDao(db: AppDatabase): RoutineDao = db.routineDao()
    @Provides fun provideRoutineExerciseDao(db: AppDatabase): RoutineExerciseDao = db.routineExerciseDao()
    @Provides fun providePlannedSetDao(db: AppDatabase): PlannedSetDao = db.plannedSetDao()
    @Provides fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()
    @Provides fun provideSetEntryDao(db: AppDatabase): SetEntryDao = db.setEntryDao()
}
