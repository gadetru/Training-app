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
 * Migración 1→2 (spec 010; spec 011, paso 12): añade `instructions`
 * (`TEXT NOT NULL DEFAULT '[]'`) conservando las filas viejas.
 * Sin `room-testing` (no declarado): se crea el esquema v1 a mano sobre
 * `sqlite-framework` (transitivo de `room-runtime`) y se ejecuta el objeto
 * real [MIGRATION_1_2].
 */
@RunWith(AndroidJUnit4::class)
class Migration12Test {

    private fun v1db(): SupportSQLiteDatabase {
        val helper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE exercises (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "slug TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "muscle TEXT NOT NULL, " +
                            "bodyPart TEXT NOT NULL, " +
                            "equipment TEXT NOT NULL, " +
                            "category TEXT NOT NULL, " +
                            "secondaryMuscles TEXT NOT NULL, " +
                            "gifUrl TEXT NOT NULL, " +
                            "source TEXT NOT NULL, " +
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
    fun migrate1_2_anade_instructions_con_defecto_vacio() {
        val db = v1db()
        db.execSQL(
            "INSERT INTO exercises (id, slug, name, muscle, bodyPart, equipment, " +
                "category, secondaryMuscles, gifUrl, source, updatedAt, deleted) " +
                "VALUES ('biceps/barbell-curl', 'barbell-curl', 'Curl con barra', " +
                "'biceps', 'arms', 'barbell', 'strength', '[\"forearms\"]', " +
                "'https://example.invalid/b.gif', 'CATALOG', 1, 0)"
        )

        MIGRATION_1_2.migrate(db)

        db.query("SELECT name, secondaryMuscles, instructions FROM exercises").use { c ->
            c.moveToFirst()
            assertEquals("Curl con barra", c.getString(0))
            assertEquals("[\"forearms\"]", c.getString(1))
            assertEquals("[]", c.getString(2))
        }
        db.close()
    }
}
