# AGENTS.md

## Estado actual (no asumir)

Hecho Fase A entera (UI-first en memoria, sin Room), 5/5 vistas:
`001-plantilla-usuario` (`feature/profile/ProfileScreen` + `domain/model/Profile`),
`002-vista-principal` (`feature/home/{HomeScreen,HomeViewModel,PlaceholderTabs}` + `domain/model/Home.kt` +
`data/repository/FakeHomeRepository` con interfaz `HomeRepository` de firma futura),
`003-lista-ejercicios` (picker `ExercisePickerSheet` + `FakeExerciseRepository`),
`004-editar-rutina` (`feature/routines/{RoutineEditScreen,RoutineEditViewModel}` + `domain/model/Routine.kt` +
`FakeRoutineRepository` con borradores separados de guardadas),
`005-rutina` (sesión en vivo `feature/workout/{WorkoutScreen,WorkoutViewModel}` + `domain/model/Workout.kt` +
`FakeWorkoutRepository` con prefill última-vs-plan; verificado `assembleDebug` OK, resto pendiente de móvil físico).
`MainActivity.kt` ya no muestra Profile directo: `TrainingNav()` con `NavHost` condicional `¿hay perfil? home:profile`
(rutas `home/calendar/progress/profile` + `routineEdit?routineId` + `workout?routineId`; `onStart` de Home abre la sesión;
el tab Perfil reutiliza `ProfileScreen` sin dock).
Hecha Fase B Room local y mergeada en `main` (rama `006-fase-b-room-local`, PR #7): entidades + DAOs con `@Upsert`,
`exportSchema=true`, repositorios Room con la misma firma que los fakes, Hilt, `CatalogTagStore` (DataStore),
Retrofit + Gson que solo rellena vía `CatalogSync`, y `CatalogMappingTest` (DTO → dominio → entidad).
En curso `007-catalogo-fork` (rama `007-catalogo-fork`, sin mergear): tag `v1.1.0` del fork `gadetru` verificado
(1323 entradas), `CatalogSync` distingue 404 (con `Log`) de sin-red, `getById` pasa a `suspend`,
DTO con `alternate` camelCase + envoltorio `CatalogResponse`; pendiente commit del paso 5 y prueba en móvil físico.
Siguiente: probar 007 en móvil físico y mergear; luego Fase C (ejercicios propios `CUSTOM`) / Fase D Spring/MySQL.
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
Excepción lote B consumida (spec 006, mergeado): `room, hilt, datastore-preferences, retrofit + converter-gson`
ya declarados y en uso (Room/Hilt/DataStore/Retrofit). No añadir más dependencias: `material-icons-extended`
solo proponer línea exacta y esperar. El agente trabaja Kotlin en `app/src/main/`, recursos y documentación.

## Comandos

- Compilar: `./gradlew assembleDebug` (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- Tests: `./gradlew testDebugUnitTest` (`CatalogMappingTest` 4/4 en verde + `ExampleUnitTest` de plantilla).
- Verificación: **emulador Pixel 6 API 34** (`emulator-5554`, verificado vía `adb` 07-10-2026);
- Tests auto: `testDebugUnitTest` (JVM) + `connectedDebugAndroidTest` (Compose smoke `HomeNav/RoutineEdit/Workout` con `testTag`);
  `testTag` en raíces/CTAs (`profileRoot/profileSave`, `homeRoot/homeDock/routineCard:<id>`,
  `routineEditRoot/routineEditAddExercise/routineEditSave`, `workoutRoot/workoutFinish/restOverlay).
- Proyecto: `applicationId`/`namespace` `com.mytrainingplan.app`, `minSdk 26`, `compileSdk/targetSdk 37`, Java 11.

## Arquitectura objetivo (al implementar)

- Un solo módulo `app`. Flujo final: `feature/* Screen → ViewModel(StateFlow) → Repository → Room`; Retrofit solo rellena la DB.
- Plan acordado: **Fase A maquetación primero** (5 vistas `references/` UI-first con `ViewModel` fake + repo fake misma firma + `NavHost` condicional `¿hay perfil? home:profile`, sin Room), luego **Fase B Room local**, **Fase C catálogo**, **Fase D Spring/MySQL**.
- Progreso Fase A: 5/5 hechas (las 5 vistas + `NavHost` con `home/calendar/progress/profile/routineEdit/workout` ya cableados); pendiente Fase B Room local.
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

## Edge-to-edge (obligatorio en cada vista)

La app usa `enableEdgeToEdge()` (`MainActivity.kt`): el contenido dibuja por debajo de las barras del sistema, que son una capa del sistema por encima (verificado contra docs oficiales vía Context7). Reglas:

- Toda cabecera superior lleva `statusBarsPadding()` (con el fondo extendido, para que la zona de estado quede del color de la vista).
- Todo dock/CTA inferior fijo lleva `navigationBarsPadding()` (conservando su margen visual de 16dp).
- Prohibido compensar con dp fijos extra que imiten la altura de las barras.
- Excepciones que ya gestionan insets solas (no tocar): `ModalBottomSheet` y diálogos de Material3; contenido centrado sin elementos pegados a los bordes.
- Checklist para futuras vistas: probar en emulador Pixel 6 API 34 con navegación por gestos y (si se puede) con 3 botones; arriba nada bajo hora/cobertura/batería, abajo nada bajo la barra del sistema.
