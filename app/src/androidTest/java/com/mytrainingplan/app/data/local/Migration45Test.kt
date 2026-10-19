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
 * Migración 4→5 (spec 023): añade `durationManual` (DEFAULT 1) a `routines`.
 * Las filas existentes pasan a MANUAL (`durationManual = 1`) y conservan su
 * `durationMin`. Patrón [Migration34Test]: esquema v4 a mano + objeto real
 * [MIGRATION_4_5].
 */
@RunWith(AndroidJUnit4::class)
class Migration45Test {

    private fun v4db(): SupportSQLiteDatabase {
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE routines (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "name TEXT NOT NULL DEFAULT '', " +
                            "durationMin INTEGER NOT NULL DEFAULT 45, " +
                            "position INTEGER NOT NULL DEFAULT 0, " +
                            "createdAt INTEGER NOT NULL DEFAULT 0, " +
                            "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                            "deleted INTEGER NOT NULL DEFAULT 0)"
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
    fun migrate4_5_anade_duration_manual_y_conserva_filas_viejas() {
        val db = v4db()
        db.execSQL(
            "INSERT INTO routines (id, name, durationMin, position, createdAt, updatedAt, deleted) " +
                "VALUES ('r1', 'Torso', 45, 0, 1000, 1000, 0)"
        )

        MIGRATION_4_5.migrate(db)

        // La columna nueva existe; la fila vieja pasa a MANUAL y conserva el minuto.
        db.query("SELECT durationMin, durationManual FROM routines WHERE id = 'r1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(45, c.getInt(0))
            assertEquals(1, c.getInt(1))
        }
        db.close()
    }
}
