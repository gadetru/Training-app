# AGENTS.md

## Estado actual (no asumir)

Hecho Fase A (UI-first en memoria, sin Room): `001-plantilla-usuario` (`feature/profile/ProfileScreen` + `domain/model/Profile`)
y `002-vista-principal` (implementado y verificado: `feature/home/{HomeScreen,HomeViewModel,PlaceholderTabs}` +
`domain/model/Home.kt` + `data/repository/FakeHomeRepository` con interfaz `HomeRepository` de firma futura).
`MainActivity.kt` ya no muestra Profile directo: `TrainingNav()` con `NavHost` condicional `¿hay perfil? home:profile`
(rutas `home/calendar/progress/profile`, `TabShell` + `BottomDock`; el tab Perfil reutiliza `ProfileScreen` sin dock).
Pendientes Fase A: `lista-ejercicios`, `rutina`, `editar-rutina` (2 de 5 vistas hechas).
`docs/`, `specs/`, `references/` siguen siendo guía objetivo/diseño, no código hecho.
Desviación conocida: `material-icons-core` solo trae 49 iconos (fallbacks `List/DateRange/Star/Person` en `HomeScreen.kt:67-82`);
fidelidad exacta a los iconos del spec exigiría `material-icons-extended` (proponer y esperar).
Antes de editar, comprueba con `Glob`/`Grep` que el fichero existe; los specs pueden citar rutas futuras.

Fuente de verdad: `README.md` (principios + restricción Gradle), `docs/ARQUITECTURA.md` (capas y carpetas),
`docs/MODELO_DE_DATOS.md` (entidades y series). Si un spec contradice a estos o al código, avisa y sigue al código.

## Restricción Gradle (obligatoria)

El esqueleto Gradle lo crea y mantiene Android Studio, **no el agente**. No crear, regenerar ni editar:
`gradlew`, `gradlew.bat`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `build.gradle.kts` (raíz y `app/`).
Excepción lote A consumida: `navigation-compose, lifecycle-viewmodel-compose, material-icons-core, coil-compose`
ya declarados (`app/build.gradle.kts:42-45`) y en uso (navigation, viewmodel, icons; coil aún sin uso real, picker pendiente).
No añadir más dependencias: Fases B/C (Room/Hilt/DataStore/Retrofit) y `material-icons-extended`
solo proponer línea exacta y esperar. El agente trabaja Kotlin en `app/src/main/`, recursos y documentación.

## Comandos

- Compilar: `./gradlew assembleDebug` (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- Tests plantilla: `./gradlew testDebugUnitTest` (solo `ExampleUnitTest`; no hay suites reales aún).
- Verificación real: probar en **móvil físico** con depuración USB/inalámbrica; **sin emulador**.
- Proyecto: `applicationId`/`namespace` `com.mytrainingplan.app`, `minSdk 26`, `compileSdk/targetSdk 37`, Java 11.

## Arquitectura objetivo (al implementar)

- Un solo módulo `app`. Flujo final: `feature/* Screen → ViewModel(StateFlow) → Repository → Room`; Retrofit solo rellena la DB.
- Plan acordado: **Fase A maquetación primero** (5 vistas `references/` UI-first con `ViewModel` fake + repo fake misma firma + `NavHost` condicional `¿hay perfil? home:profile`, sin Room), luego **Fase B Room local**, **Fase C catálogo**, **Fase D Spring/MySQL**.
- Progreso Fase A: 2/5 hechas (`plantilla-usuario`, `vista-principal` con `NavHost` + `HomeViewModel`/`FakeHomeRepository` ya cableados); pendientes `lista-ejercicios`, `rutina`, `editar-rutina`.
- Carpetas: `core/{di,network,ui}`, `data/{local/{entity,dao},remote/{dto},mapper,repository}`, `domain/model`, `feature/{profile,exercises,routines,workout,history}`. Cada `feature/` = `Screen` + `ViewModel`.
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
