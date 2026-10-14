package com.mytrainingplan.app.core.di

import com.mytrainingplan.app.core.datastore.CatalogTagStore
import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.PlannedSetDao
import com.mytrainingplan.app.data.local.dao.ProfileDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.RoutineExerciseDao
import com.mytrainingplan.app.data.local.dao.ScheduledDayDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.remote.ExerciseApi
import com.mytrainingplan.app.data.repository.CatalogSync
import com.mytrainingplan.app.data.repository.ExerciseRepository
import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.data.repository.ProfileRepository
import com.mytrainingplan.app.data.repository.RoomExerciseRepository
import com.mytrainingplan.app.data.repository.RoomProfileRepository
import com.mytrainingplan.app.data.repository.RoomRoutineRepository
import com.mytrainingplan.app.data.repository.RoomScheduleRepository
import com.mytrainingplan.app.data.repository.RoomWorkoutRepository
import com.mytrainingplan.app.data.repository.RoutineRepository
import com.mytrainingplan.app.data.repository.ScheduleRepository
import com.mytrainingplan.app.data.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo repositorios (Paso 10 spec 006). Misma forma que los fakes;
 * solo cambia la inyección (sin Hilt manual en ViewModels, paso 11).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProfileRepository(dao: ProfileDao): ProfileRepository =
        RoomProfileRepository(dao)

    @Provides
    @Singleton
    fun provideExerciseRepository(dao: ExerciseDao): ExerciseRepository =
        RoomExerciseRepository(dao)

    @Provides
    @Singleton
    fun provideRoutineRepository(
        routines: RoutineDao,
        items: RoutineExerciseDao,
        sets: PlannedSetDao,
        exercises: ExerciseDao
    ): RoutineRepository = RoomRoutineRepository(routines, items, sets, exercises)

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        sessions: WorkoutSessionDao,
        entries: SetEntryDao,
        routines: RoutineDao,
        routineRepo: RoutineRepository,
        exercises: ExerciseDao,
        // Spec 016 paso 2: la doble escritura atómica necesita la DB y ambas tablas.
        db: com.mytrainingplan.app.data.local.AppDatabase,
        routineExercises: RoutineExerciseDao,
        plannedSets: PlannedSetDao,
        // Spec 016 paso 6: el auto-reemplazo del apunte al finalizar.
        scheduleRepo: ScheduleRepository
    ): WorkoutRepository =
        RoomWorkoutRepository(sessions, entries, routines, routineRepo, exercises, db, routineExercises, plannedSets, scheduleRepo)

    @Provides
    @Singleton
    fun provideCatalogSync(
        api: ExerciseApi,
        dao: ExerciseDao,
        tags: CatalogTagStore
    ): CatalogSync = CatalogSync(api, dao, tags)

    @Provides
    @Singleton
    fun provideHomeRepository(
        routineRepo: RoutineRepository,
        profileRepo: ProfileRepository,
        sessions: WorkoutSessionDao
    ): HomeRepository = com.mytrainingplan.app.data.repository.RoomHomeRepository(routineRepo, profileRepo, sessions)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        scheduled: ScheduledDayDao,
        sessions: WorkoutSessionDao,
        routines: RoutineDao
    ): ScheduleRepository = RoomScheduleRepository(scheduled, sessions, routines)
}
