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
`FakeWorkoutRepository` con prefill última-vs-plan; verificado `assembleDebug` OK).
`MainActivity.kt` ya no muestra Profile directo: `TrainingNav()` con `NavHost` condicional `¿hay perfil? home:profile`
(rutas `home/calendar/progress/profile` + `routineEdit?routineId` + `workout?routineId` + `exerciseDetail?exerciseId`;
`onStart` de Home abre la sesión; el tab Perfil reutiliza `ProfileScreen` sin dock; la ficha abre desde
picker, editar rutina y sesión con `Uri.encode` por el `/` de `músculo/slug`).
Hecha Fase B Room local y mergeada en `main` (rama `006-fase-b-room-local`, PR #7): entidades + DAOs con `@Upsert`,
`exportSchema=true`, repositorios Room con la misma firma que los fakes, Hilt, `CatalogTagStore` (DataStore),
Retrofit + Gson que solo rellena vía `CatalogSync`, y `CatalogMappingTest` (DTO → dominio → entidad).
Mergeados en `main` tras Fase B: `007-catalogo-fork` (PR #8, tag `v1.1.0` del fork `gadetru` verificado,
1323 entradas, `CatalogSync` distingue 404 con `Log` de sin-red, `getById` a `suspend`, DTO camelCase +
`CatalogResponse`), `008-rutinas-feed` (PR #9, feed único + descarte seguro + `position` sin huecos),
`009-sesion-persistida` (PR #10, prefill persistido al iniciar + cierre sin fantasmas + log interno).
En curso `010-detalle-ejercicio` (rama `010-detalle-ejercicio`, sin mergear): ficha `ExerciseDetailScreen` +
`ExerciseDetailViewModel`, `instructions` en modelo/entidad/mapper/DTO, DB v2 + `MIGRATION_1_2` y re-descarga
mismo tag; checklist 6/10 en `[x]`, pendiente sin-red, rotación y edge-to-edge.
Deuda: los `Fake*Repository` de Fase A conviven con los Room pero ya no se inyectan (Hilt usa los Room);
marcados como deuda a borrar en cuanto dejen de estar en uso, no borrar aún sin confirmar.
Siguiente: cerrar 010 y mergear; luego ejercicios propios `CUSTOM` + historial real / Fase D Spring/MySQL.
Punto de control: `docs/01-punto-actual-2026-10-08.md` (revisión 2026-10-08).
`docs/`, `specs/`, `references/` siguen siendo guía objetivo/diseño, no código hecho.
Desviación conocida: `material-icons-core` solo trae 49 iconos (fallbacks `List/DateRange/Star/Person` en `HomeScreen.kt:67-82`);
fidelidad exacta a los iconos del spec exigiría `material-icons-extended` (proponer y esperar).
Antes de editar, comprueba con `Glob`/`Grep` que el fichero existe; los specs pueden citar rutas futuras
(`feature/history/` aún no existe: solo `PlaceholderTabs`).

Fuente de verdad: `README.md` (principios + restricción Gradle), `docs/ARQUITECTURA.md` (capas y carpetas),
`docs/MODELO_DE_DATOS.md` (entidades y series). Si un spec contradice a estos o al código, avisa y sigue al código.

## Restricción Gradle (obligatoria)

El esqueleto Gradle lo crea y mantiene Android Studio, **no el agente**. No crear, regenerar ni editar:
`gradlew`, `gradlew.bat`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `build.gradle.kts` (raíz y `app/`).
Excepción lote A consumida: `navigation-compose, lifecycle-viewmodel-compose, material-icons-core, coil-compose`
ya declarados (`app/build.gradle.kts:42-45`) y en uso (navigation, viewmodel, icons; coil en uso vía `AsyncImage`
en picker/detalle/editar para thumbs + GIF grande —sin `coil-gif` = primer frame—; avatar picker pendiente).
Excepción lote B consumida (spec 006, mergeado): `room, hilt, datastore-preferences, retrofit + converter-gson`
ya declarados y en uso (Room/Hilt/DataStore/Retrofit). No añadir más dependencias: `material-icons-extended`
solo proponer línea exacta y esperar. El agente trabaja Kotlin en `app/src/main/`, recursos y documentación.

## Comandos

- Compilar: `./gradlew assembleDebug` (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- Tests: `./gradlew testDebugUnitTest` (`CatalogMappingTest` 5/5 en verde + `ExampleUnitTest` de plantilla).
- Verificación: **emulador Pixel 6 API 34** (`emulator-5554`) **+ dispositivo físico, de forma automática**;
  se testea en ambos: `adb` + MCP `mobile-mcp` (`list_available_devices` → `list_elements` por `ref` → `click`/`screenshot`/`logs`)
  y `connectedDebugAndroidTest` como red de seguridad. Si un spec dice explícitamente solo uno, manda el spec y se anota.
- Tests auto: `testDebugUnitTest` (JVM) + `connectedDebugAndroidTest` (Compose smoke `HomeNav/RoutineEdit/Workout` con `testTag`,
  smoke detalle pendiente);
  `testTag` en raíces/CTAs (`profileRoot/profileSave`, `homeRoot/homeDock/routineCard:<id>`,
  `routineEditRoot/routineEditAddExercise/routineEditSave`, `workoutRoot/workoutFinish/restOverlay).
- Proyecto: `applicationId`/`namespace` `com.mytrainingplan.app`, `minSdk 26`, `compileSdk/targetSdk 37`, Java 11.

## Arquitectura objetivo (al implementar)

- Un solo módulo `app`. Flujo final: `feature/* Screen → ViewModel(StateFlow) → Repository → Room`; Retrofit solo rellena la DB.
- Plan acordado: **Fase A maquetación primero** (5 vistas `references/` UI-first con `ViewModel` fake + repo fake misma firma + `NavHost` condicional `¿hay perfil? home:profile`, sin Room), luego **Fase B Room local (hecha, v2)**, **catálogo hecho (007+010)**, pendiente **CUSTOM + historial real** y **Fase D Spring/MySQL**.
- Progreso Fase A: 5/5 hechas (las 5 vistas + `NavHost` con `home/calendar/progress/profile/routineEdit/workout/exerciseDetail` ya cableados); Fase B + 007/008/009 mergeados en `main`; en curso 010.
- Carpetas: `core/{di,network,datastore,ui}`, `data/{local/{entity,dao},remote/{dto},mapper,repository}`, `domain/model`, `feature/{profile,home,exercises,routines,workout}`. Cada `feature/` = `Screen` + `ViewModel` (`exercises` tiene Picker + Detail). `feature/history/` aún no existe: solo `PlaceholderTabs`.
- Mapeo `DTO/Entity ⇄ dominio` en `data/mapper`; la UI solo ve `domain/model`, nunca Entity ni DTO.
- Sin casos de uso ni módulos extra hasta que duelan. Versiones solo vía catálogo `gradle/libs.versions.toml`.
- Catálogo: fork ExerciseGymGifsDB por jsDelivr con **tag fijo** (`CATALOG_TAG` + DataStore), nunca rama; solo URLs de GIF en DB (Coil cachea); update por `upsert` y solo filas `source=CATALOG`.

## Datos Room (al implementar)

- IDs de usuario = **UUID** en cliente; ejercicios de catálogo conservan su `id` (`músculo/slug`).
- Toda entidad de usuario lleva `updatedAt` + `deleted` (borrado lógico, sync-ready Fase 2) desde el día uno.
- Catálogo y propios comparten tabla con `source` (`CATALOG`/`CUSTOM`).
- **Siempre `@Upsert`, nunca `@Insert(onConflict = REPLACE)`** (REPLACE borra la fila y arrastra hijas).
- `exportSchema = true`; cada cambio de esquema sube versión + `Migration` real; `fallbackToDestructiveMigration()` solo en desarrollo. Estado: DB **v2** + `MIGRATION_1_2` (`instructions TEXT DEFAULT '[]'`; la re-descarga con el mismo tag rellena las viejas vía `upsert`).
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
- Checklist para futuras vistas: probar en emulador Pixel 6 API 34 **y en físico, de forma automática**, con navegación por gestos y (si se puede) con 3 botones; arriba nada bajo hora/cobertura/batería, abajo nada bajo la barra del sistema.
