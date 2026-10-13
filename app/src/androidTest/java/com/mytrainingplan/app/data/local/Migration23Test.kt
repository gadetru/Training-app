package com.mytrainingplan.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración 2→3 (Paso 9 spec 015): crea `scheduled_days` conservando las
 * filas viejas. Mismo patrón que [Migration12Test] (sin `room-testing`:
 * esquema v2 a mano + objeto real [MIGRATION_2_3]).
 */
@RunWith(AndroidJUnit4::class)
class Migration23Test {

    private fun v2db(): SupportSQLiteDatabase {
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE routines (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "durationMin INTEGER NOT NULL, " +
                            "position INTEGER NOT NULL, " +
                            "createdAt INTEGER NOT NULL, " +
                            "updatedAt INTEGER NOT NULL, " +
                            "deleted INTEGER NOT NULL)"
                    )
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
    fun migrate2_3_crea_scheduled_days_sin_tocar_lo_viejo() {
        val db = v2db()
        db.execSQL(
            "INSERT INTO routines (id, name, durationMin, position, createdAt, updatedAt, deleted) " +
                "VALUES ('r-torso', 'Torso', 45, 0, 1, 1, 0)"
        )
        db.execSQL(
            "INSERT INTO workout_sessions (id, routineId, startedAt, endedAt, pausedAccumSec, updatedAt, deleted) " +
                "VALUES ('s1', 'r-torso', 1000, 2000, 0, 1, 0)"
        )

        MIGRATION_2_3.migrate(db)

        db.query("SELECT name FROM routines").use { c ->
            c.moveToFirst()
            assertEquals("Torso", c.getString(0))
        }
        db.query("SELECT COUNT(*) FROM workout_sessions").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.execSQL(
            "INSERT INTO scheduled_days (id, routineId, date, updatedAt, deleted) " +
                "VALUES ('d1', 'r-torso', '2026-10-15', 1, 0)"
        )
        db.query("SELECT routineId, date FROM scheduled_days").use { c ->
            c.moveToFirst()
            assertEquals("r-torso", c.getString(0))
            assertEquals("2026-10-15", c.getString(1))
        }
        db.close()
    }
}
