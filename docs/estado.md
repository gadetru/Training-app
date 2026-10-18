# Estado del proyecto (detalle)

Historial de lo ya mergeado en `main`, deuda y puntos de control. `AGENTS.md` solo lleva el resumen; este fichero es el detalle.
**Actualízalo al mergear cada spec** (una entrada nueva abajo del bloque correspondiente). Ante duda, manda el código.

## Fase A: UI-first en memoria, sin Room (5/5 vistas)

- `001-plantilla-usuario`: `feature/profile/ProfileScreen` + `domain/model/Profile`.
- `002-vista-principal`: `feature/home/{HomeScreen,HomeViewModel,PlaceholderTabs}` + `domain/model/Home.kt` + `data/repository/FakeHomeRepository` con interfaz `HomeRepository` de firma futura.
- `003-lista-ejercicios`: picker `ExercisePickerSheet` + `FakeExerciseRepository`.
- `004-editar-rutina`: `feature/routines/{RoutineEditScreen,RoutineEditViewModel}` + `domain/model/Routine.kt` + `FakeRoutineRepository` con borradores separados de guardadas.
- `005-rutina`: sesión en vivo `feature/workout/{WorkoutScreen,WorkoutViewModel}` + `domain/model/Workout.kt` + `FakeWorkoutRepository` con prefill última-vs-plan. Verificado `assembleDebug` OK.

Navegación: `MainActivity.kt` ya no muestra Profile directo. Usa `TrainingNav()` con `NavHost` condicional `¿hay perfil? home:profile`. Rutas: `home/calendar/progress/profile` + `routineEdit?routineId` + `workout?routineId` + `exerciseDetail?exerciseId`. El `onStart` de Home abre la sesión. El tab Perfil reutiliza `ProfileScreen` sin dock. La ficha se abre desde picker, editar rutina y sesión, con `Uri.encode` por el `/` de `músculo/slug`.

## Fase B: Room local (mergeada, rama `006-fase-b-room-local`, PR #7)

Entidades + DAOs con `@Upsert`, `exportSchema=true`, repositorios Room con la misma firma que los fakes, Hilt, `CatalogTagStore` (DataStore), Retrofit + Gson que solo rellena vía `CatalogSync`, y `CatalogMappingTest` (DTO → dominio → entidad).

## Specs mergeados en `main`

- `007-catalogo-fork` (PR #8): tag `v1.1.0` del fork `gadetru` verificado, 1323 entradas. `CatalogSync` distingue 404 con `Log` de sin-red. `getById` pasa a `suspend`. DTO camelCase + `CatalogResponse`.
- `008-rutinas-feed` (PR #9): feed único + descarte seguro + `position` sin huecos.
- `009-sesion-persistida` (PR #10): prefill persistido al iniciar + cierre sin fantasmas + log interno.
- `010-detalle-ejercicio` (PR #13): ficha `ExerciseDetailScreen` + `ExerciseDetailViewModel`, `instructions` en modelo/entidad/mapper/DTO, DB v2 + `MIGRATION_1_2` y re-descarga con el mismo tag (rellena las filas viejas vía `upsert`). Checklist 10/10 en `[x]`.
- `011-blindaje-visual-tests-autofix` (PR #14/#15/#16):
  - UI a botes con nombre en `ui/theme/` (`Color/Dimensions/Theme/Type`).
  - Tests en espejo por paquete y regla en `src/test/` (botes JVM + mapper + reglas con falsos + fotos Roborazzi Home y tarjeta) y `src/androidTest/` (DAO + migración + smokes movidos a su espejo).
  - Puerta de merge `.github/workflows/blindaje.yml` (`assembleDebug` + `testDebugUnitTest`).
  - Fix CI autorizado 2026-10-09: `setup-android@v3` → `@v4` (v3 pedía el paquete obsoleto `tools` → `Failed to find package 'tools'`; v4 solo instala `platform-tools` por defecto) + bit ejecutable en `gradlew` (`100644` → `100755`, contenido sin cambios).
  - Dependencias lote C: `roborazzi`, `roborazzi-compose`, `robolectric` + plugin Roborazzi, con `testOptions.unitTests.isIncludeAndroidResources=true` y dorados en `app/src/test/screenshots` (solo Home + tarjeta).
- `012-buscador-gifs` (PR #17): buscador escribible + filtro por etiqueta ES + `coil-gif` (lote D, autorizado 2026-10-09, misma versión que `coil` 2.6.0) + hero animado en ficha con thumbs quietos + test JVM del filtro. `ImageLoaderFactory` global en `TrainingApp.kt` (`GifDecoder.Factory()`).
- `013-sesion-vivo-fixes` (PR #18): modal numérico KG/REPS/PAUSA + cronómetro detenido con Comenzar/Pausar/Reanudar + tap único para expandir.
- `014-perfil-enfoque-crear-rutina` (PR #19): chips de enfoque legibles a 360dp + preview que navega a `routineEdit` nuevo.
- `01-reparacion-bugs` (PR #20, sin spec): retoques en `HomeScreen` + `RoutineEditScreen`; ver apéndice en `README.md`.
- `015-calendario-progreso` (PR #22): `feature/history/` nuevo con `CalendarScreen`/`ProgressScreen` + ViewModels, `domain/model/History.kt` con `ScheduledDay`/`MonthDay`/`HistoryCalcs`, tabla `scheduled_days` + `ScheduledDayDao`, DB v3 + `MIGRATION_2_3`, `ScheduleRepository` Room + falso + `ScheduleMapper`, progreso y etiquetas reales en `RoomHomeRepository`, `calendar`/`progress` reales en `MainActivity` + `Ver mes`, `PlaceholderTabs` borrado. Tests: `HistoryCalcsTest`/`ScheduleFakeTest` + `ScheduledDayDaoTest`/`Migration23Test` + `CalendarSmokeTest`. Verificado 36/36 instrumentados en emulador y en físico (físico vía `am instrument` porque MIUI rechaza el `-g` de AGP). Fix mínimo ajeno al spec: `performImeAction()` en `RoutineEditSmokeTest`.
- `016-sesion-edicion` (PR #23, spec en `implementado`): ⋮ con "Editar rutina"/"Salir del entrenamiento", "+ Añadir serie" que clona la última con doble escritura transaccional (serie viva + planificada, `@Upsert`, borrado lógico), swipe solo en pendientes con "¿Borrar?", auto-reemplazo del apunte al finalizar otra rutina + aviso en Descartar. Verificado 11/11: `assembleDebug` OK, `testDebugUnitTest` 62/62 (con `WorkoutLiveEditTest` 7/7), emulador 42/42 (con `Workout016LiveEditTest` 6/6). Físico bloqueado por MIUI `INSTALL_FAILED_USER_RESTRICTED`.
- `017-picker-alto-completo` (PR #24, spec en `implementado`): picker a alto completo (`fillMaxHeight(0.88f)` → `1f` en `ExercisePickerSheet`), resto intacto. Verificado: `assembleDebug` OK, `testDebugUnitTest` 62/62, `PickerSearchTest` 4/4 en emulador. Físico bloqueado por el mismo MIUI.

## Implementados, pendientes de merge

- `022-sesion-en-marcha` (rama `022-sesion-en-marcha`, sin commitear): cronómetro persistente (DB v4 + `MIGRATION_3_4` con `timerBaseSec`/`runningSince`, `Migration34Test`), guardia anti-doble en `startSession` (Room + fake), `WorkoutViewModel.openSession(routineId, sessionId)` con estados `timerState`/`timerElapsedSec` derivados de DB, tick 1s pantalla + tick 15s `timerSync` kill-safe, CTA "Finalizar" (sesión). Home: `HomeUiState.liveRoutineId`/`liveSessionId`/`liveElapsedSec`/`scheduledTodayId`/`hasLiveSession`, `RoomHomeRepository` combina `observeLive()` + `scheduled_days` de hoy, `HomeScreen` card EN MARCHA (Reanudar `resumeWorkout:<id>` / Terminar `finishWorkout:<id>` / Descartar `discardWorkout:<id>` + confirmación + pie contador con tick 1s), `primary` = programada hoy, resto Iniciar no clicable. `MainActivity` ruta `workout?routineId&sessionId` + wiring `onLiveResume/onLiveFinish/onLiveDiscard`. Tests JVM: `TimerRulesTest`, `HomeLiveTest`, `StartSessionGuardTest`; `testDebugUnitTest` verde (96/96). `assembleDebug` OK; schema v4 regenerado (`4.json` con las 2 columnas).
  Verificación bloqueada: `connectedDebugAndroidTest` no lanzable (no hay `adb` ni dispositivo en estado `device` en este entorno). Pendiente de prueba en emulador Pixel 6 API 34 y físico.

- `023-duracion-rutina` (rama `023-duracion-rutina`, sin commitear): duración vacía/estimada/real. `domain/model/RoutineDurationRules.kt` (reglas puras `estimate`/`fromRealSeconds`); `Routine` + `RoutineEditUiState` con `durationManual: Boolean = false` y default `durationMin = 0`; `RoutineEntity` + `RoutineMapper` columna `durationManual`; `AppDatabase` v5 + `MIGRATION_4_5` (DEFAULT 1, existentes → MANUAL conservando minuto) registrado en `DatabaseModule`; repositorios (fake + Room) `createRoutine`/`updateMeta` con `durationManual`, `saveRoutine` recalcula estimación si AUTO, `finishSession` aplica tiempo real si AUTO (misma transacción en Room; fake espeja sin pasar por `saveRoutine` para no recalcular); `RoutineEditViewModel.onDurationChange` (→ MANUAL) / `onDurationModeChange` (AUTO recalcula); `RoutineEditScreen` interruptor AUTO/MANUAL + campo según modo (`—` sin series, `≈ X min` AUTO, editable MANUAL) y `SaveFooter` con valor efectivo (`≈` solo AUTO); `HomeScreen` oculta línea de duración si 0. Tests JVM: `RoutineDurationRulesTest` (8), `RoutineDurationFlowTest` (3), `FinishSessionDurationTest` (3) — verdes; `CatalogMappingTest` sin romper. `assembleDebug` OK; `testDebugUnitTest` verde.
  Tests instrumentados: `Migration45Test` + `RoutineEdit023DurationTest` creados en `src/androidTest/`; no ejecutables (no hay `adb` ni dispositivo en estado `device`) → `bloqueado: sin dispositivo`. Pendiente de prueba en emulador Pixel 6 API 34 y físico (smoke: vacío → `≈` al añadir series → MANUAL → AUTO → guardar; finalizar sesión AUTO → tiempo real, MANUAL intacta; Home muestra/oculta línea).


## Siguiente

`018-guardar-rutina-scroll` (Borrador en `specs/`). Más tarde: CUSTOM (MVP-3) y Fase D Spring/MySQL.

## Deuda y desviaciones conocidas

- Los `Fake*Repository` de Fase A conviven con los Room pero ya no se inyectan (Hilt usa los Room). Borrar en cuanto dejen de estar en uso, no antes de confirmar. `FakeScheduleRepository` es nuevo del 015 y sí se usa en tests JVM.
- `material-icons-core` solo trae 49 iconos: hay fallbacks (`List/DateRange/Star/Person`) en `HomeScreen`. La fidelidad exacta a los iconos del spec exigiría `material-icons-extended`: proponer la línea exacta y esperar.
- Avatar picker de Coil pendiente.
- Smoke del detalle de ejercicio pendiente.

## Puntos de control

- `docs/01-punto-actual-2026-10-08.md`: revisión de 2026-10-08, **desactualizada** (dice que 010 está sin mergear).
- `docs/fotos-referencia.md`: dorados Roborazzi + regeneración.
- `docs/revision-tarde.md`: borrador del spec 011 (hora pendiente).
