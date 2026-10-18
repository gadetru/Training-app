package com.mytrainingplan.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración 3→4 (spec 022): añade `timerBaseSec` (0 por defecto) y
 * `runningSince` (null) a `workout_sessions` conservando las filas viejas.
 * Patrón [Migration23Test]: esquema v3 a mano + objeto real [MIGRATION_3_4].
 */
@RunWith(AndroidJUnit4::class)
class Migration34Test {

    private fun v3db(): SupportSQLiteDatabase {
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE workout_sessions (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "routineId TEXT NOT NULL, " +
                            "startedAt INTEGER NOT NULL, " +
                            "endedAt INTEGER, " +
                            "pausedAccumSec INTEGER NOT NULL, " +
                            "updatedAt INTEGER NOT NULL, " +
                            "deleted INTEGER NOT NULL)"
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            }).build()
        )
        return helper.writableDatabase
    }

    @Test
    fun migrate3_4_anade_columnas_sin_tocar_filas_viejas() {
        val db = v3db()
        db.execSQL(
            "INSERT INTO workout_sessions (id, routineId, startedAt, endedAt, pausedAccumSec, updatedAt, deleted) " +
                "VALUES ('s1', 'r-torso', 1000, null, 0, 1, 0)"
        )

        MIGRATION_3_4.migrate(db)

        // Las columnas nuevas existen y las viejas conservan sus valores.
        db.query("SELECT timerBaseSec, runningSince, startedAt FROM workout_sessions WHERE id = 's1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
            assertTrue(c.isNull(1))
            assertEquals(1000L, c.getLong(2))
        }
        db.close()
    }
}
