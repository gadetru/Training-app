# AGENTS.md

## Estado actual (no asumir)

`app/src` es todavía la plantilla "Hello Android" de Android Studio (`MainActivity.kt` + `ui/theme/`).
`docs/`, `specs/`, `references/` describen la arquitectura **objetivo**, no código existente.
Antes de editar, comprueba con `Glob`/`Grep` que el fichero existe; los specs pueden citar rutas futuras.

Fuente de verdad: `README.md` (principios + restricción Gradle), `docs/ARQUITECTURA.md` (capas y carpetas),
`docs/MODELO_DE_DATOS.md` (entidades y series). Si un spec contradice a estos o al código, avisa y sigue al código.

## Restricción Gradle (obligatoria)

El esqueleto Gradle lo crea y mantiene Android Studio, **no el agente**. No crear, regenerar ni editar:
`gradlew`, `gradlew.bat`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `build.gradle.kts` (raíz y `app/`).
Si falta una dependencia/plugin (hoy solo hay Compose/Material3/activity/core/lifecycle; aún **no** están
Room, Hilt, Retrofit, Coil, DataStore), indica la línea exacta a añadir y espera al desarrollador.
El agente trabaja solo sobre Kotlin en `app/src/main/`, recursos y documentación.

## Comandos

- Compilar: `./gradlew assembleDebug` (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- Tests plantilla: `./gradlew testDebugUnitTest` (solo `ExampleUnitTest`; no hay suites reales aún).
- Verificación real: probar en **móvil físico** con depuración USB/inalámbrica; **sin emulador**.
- Proyecto: `applicationId`/`namespace` `com.example.mytrainingplan`, `minSdk 26`, `compileSdk/targetSdk 37`, Java 11.

## Arquitectura objetivo (al implementar)

- Un solo módulo `app`. Flujo: `feature/* Screen → ViewModel(StateFlow) → Repository → Room`; Retrofit solo rellena la DB.
- Carpetas: `core/{di,network,ui}`, `data/{local/{entity,dao},remote/{dto},mapper,repository}`, `domain/model`, `feature/{exercises,routines,workout,history}`. Cada `feature/` = `Screen` + `ViewModel`.
- Mapeo `DTO/Entity ⇄ dominio` en `data/mapper`; la UI solo ve `domain/model`, nunca Entity ni DTO.
- Sin casos de uso ni módulos extra hasta que duelan. Versiones solo vía catálogo `gradle/libs.versions.toml`.
- Catálogo: fork ExerciseGymGifsDB por jsDelivr con **tag fijo** (`CATALOG_TAG` + DataStore), nunca rama; solo URLs de GIF en DB (Coil cachea); update por `upsert` y solo filas `source=CATALOG`.

## Datos Room (al implementar)

- IDs de usuario = **UUID** en cliente; ejercicios de catálogo conservan su `id` (`músculo/slug`).
- Toda entidad de usuario lleva `updatedAt` + `deleted` (borrado lógico, sync-ready Fase 2) desde el día uno.
- Catálogo y propios comparten tabla con `source` (`CATALOG`/`CUSTOM`).
- **Siempre `@Upsert`, nunca `@Insert(onConflict = REPLACE)`** (REPLACE borra la fila y arrastra hijas).
- `exportSchema = true`; cada cambio de esquema sube versión + `Migration` real; `fallbackToDestructiveMigration()` solo en desarrollo.
- `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `loadNote` libre; `restSeconds` vive en **cada serie**, no en el ejercicio.
- Comportamiento series (`docs/MODELO_DE_DATOS.md`): al crear N series se copian valores de la 1ª; editar propaga solo a siguientes **no editadas a mano** (estado solo de pantalla); día nuevo = valores por defecto de ajustes, sin copiar de otros días; al iniciar sesión, prefill con **última sesión** de ese ejercicio en ese día, si no con lo planificado.

## Distribución

Mismo `applicationId` siempre; misma keystore (fuera del repo, con copia externa); subir `versionCode` por versión; cambio de esquema Room = subir versión DB + migración o se pierden datos.
