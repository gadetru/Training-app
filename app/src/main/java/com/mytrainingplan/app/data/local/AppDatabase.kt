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
import com.mytrainingplan.app.data.local.dao.ScheduledDayDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.local.entity.ExerciseEntity
import com.mytrainingplan.app.data.local.entity.PlannedSetEntity
import com.mytrainingplan.app.data.local.entity.ProfileEntity
import com.mytrainingplan.app.data.local.entity.RoutineEntity
import com.mytrainingplan.app.data.local.entity.RoutineExerciseEntity
import com.mytrainingplan.app.data.local.entity.ScheduledDayEntity
import com.mytrainingplan.app.data.local.entity.SetEntryEntity
import com.mytrainingplan.app.data.local.entity.WorkoutSessionEntity

/**
 * Base Room v5 (Paso 3 spec 006, Fase B; v5 spec 023: +`durationManual` en
 * `routines` para el modo AUTO/MANUAL de la duración).
 * 8 tablas espejo del dominio. `exportSchema = true`, esquemas en
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
        SetEntryEntity::class,
        ScheduledDayEntity::class
    ],
    version = 5,
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
    abstract fun scheduledDayDao(): ScheduledDayDao
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

/**
 * Migración 2→3 (spec 015): crea `scheduled_days` (apunte día + rutina
 * con borrado lógico). Sin tocar sesiones ni rutinas existentes.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scheduled_days` (" +
                "`id` TEXT NOT NULL, " +
                "`routineId` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`deleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * Migración 3→4 (spec 022): añade `timerBaseSec` (segundos ya contados,
 * 0 por defecto) y `runningSince` (marca de arranque, null = parado) a
 * `workout_sessions`. Filas viejas quedan con 0 / null: la sesión fresca
 * conserva el arranque del 013 (00:00 + Comenzar). Sin tocar sesiones ni
 * series existentes; siempre `@Upsert`, nunca REPLACE.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN timerBaseSec INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN runningSince INTEGER")
    }
}

/**
 * Migración 4→5 (spec 023): añade `durationManual` (INTEGER NOT NULL DEFAULT 1)
 * a `routines`. Las filas existentes pasan a MANUAL y conservan su `durationMin`
 * (hoy 45 min por defecto de la entidad); las nuevas nacen en AUTO (`false` = 0)
 * salvo que el repo las cree con `durationManual = true`.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routines ADD COLUMN durationManual INTEGER NOT NULL DEFAULT 1")
    }
}
