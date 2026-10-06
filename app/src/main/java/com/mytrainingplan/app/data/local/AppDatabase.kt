package com.mytrainingplan.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.PlannedSetDao
import com.mytrainingplan.app.data.local.dao.ProfileDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.RoutineExerciseDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.local.entity.ExerciseEntity
import com.mytrainingplan.app.data.local.entity.PlannedSetEntity
import com.mytrainingplan.app.data.local.entity.ProfileEntity
import com.mytrainingplan.app.data.local.entity.RoutineEntity
import com.mytrainingplan.app.data.local.entity.RoutineExerciseEntity
import com.mytrainingplan.app.data.local.entity.SetEntryEntity
import com.mytrainingplan.app.data.local.entity.WorkoutSessionEntity

/**
 * Base Room v2 (Paso 3 spec 006, Fase B; v2 spec 010: +`instructions`).
 * 7 tablas espejo del dominio. `exportSchema = true`, esquemas en
 * `app/schemas/` (se generan al sincronizar en Android Studio con red;
 * aquí se deja la carpeta lista). `fallbackToDestructiveMigration()` solo
 * en desarrollo (no incluido). Siempre `@Upsert`, nunca REPLACE.
 */
@Database(
    entities = [
        ProfileEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        PlannedSetEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun plannedSetDao(): PlannedSetDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setEntryDao(): SetEntryDao
}

/**
 * Migración 1→2 (spec 010): añade `instructions` (JSON `[]` por defecto)
 * a `exercises`. Filas viejas quedan con lista vacía y la re-descarga del
 * catálogo (mismo tag) las rellena vía `upsert`.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT NOT NULL DEFAULT '[]'")
    }
}
