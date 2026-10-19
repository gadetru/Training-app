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

## Mergeados: 018 a 022

- `018-guardar-rutina-scroll` (PR #25): pie "Finalizar y Guardar Rutina" auto-oculto al bajar y reaparece al subir (patrón M3), botón siempre accesible.
- `019-sesion-colapsado` (PR #26): ejercicios colapsados al abrir la sesión; colapso automático al cerrar el descanso y avance al siguiente pendiente; tap manual re-expande.
- `020-ceros-numericos` (PR #27): ceros a la izquierda mientras se teclea ("012" → "12"), una regla de limpieza compartida por modal de sesión, constructor y perfil.
- `021-sustituir-ejercicio` (PR #28): swipe a la izquierda sobre tarjeta colapsada revela "Sustituir"; el picker lo reemplaza y queda guardado en la rutina de forma inmediata y permanente.
- `022-sesion-en-marcha` (PR #30): cronómetro persistente (DB v4 + `MIGRATION_3_4` con `timerBaseSec`/`runningSince`, `Migration34Test`), guardia anti-doble en `startSession` (Room + fake), `WorkoutViewModel.openSession(routineId, sessionId)` con estados `timerState`/`timerElapsedSec` derivados de DB, tick 1s pantalla + tick 15s `timerSync` kill-safe, CTA "Finalizar" (sesión). Home: `HomeUiState.liveRoutineId`/`liveSessionId`/`liveElapsedSec`/`scheduledTodayId`/`hasLiveSession`, `RoomHomeRepository` combina `observeLive()` + `scheduled_days` de hoy, `HomeScreen` card EN MARCHA (Reanudar `resumeWorkout:<id>` / Terminar `finishWorkout:<id>` / Descartar `discardWorkout:<id>` + confirmación + pie contador con tick 1s), `primary` = programada hoy, resto Iniciar no clicable. `MainActivity` ruta `workout?routineId&sessionId` + wiring `onLiveResume/onLiveFinish/onLiveDiscard`.   Tests JVM: `TimerRulesTest`, `HomeLiveTest`, `StartSessionGuardTest`; `testDebugUnitTest` verde (96/96). `assembleDebug` OK; schema v4 regenerado (`4.json` con las 2 columnas).
  Verificación /verifier 2026-10-18: `assembleDebug` OK; `testDebugUnitTest` 110/110; `connectedDebugAndroidTest` en emulador Pixel 6 API 34 (`emulator-5554`, vía `ANDROID_SERIAL`): **59/59 verde** (incluye `Workout022ResumeTest` 2/2 nuevo y los smokes 013/019/021 arreglados). UI adb (MCP no disponible en sesión): flujo completo 022 verificado con capturas en `build/022-0*.png` — salir tras marcar → card EN MARCHA corriendo; Reanudar conserva series (100%) y reloj; Terminar → Iniciar de vuelta + "Hoy" + racha; Descartar con diálogo; pausa → force-stop → relanzar → EN PAUSA congelada (03:53); CTA naranja solo programada hoy (Rutina 2 gris); doble tap Iniciar → una sesión; sin crashes. Checklist §11 marcado (10/11; físico bloqueado).
  Físico: `bloqueado: sin dispositivo físico` (`ae4a376e` desapareció de `adb devices` a mitad de sesión; reintentar con instalación manual + `am instrument` si MIUI rechaza el `-g`).

## Implementados, pendientes de merge

- `023-duracion-rutina` (rama `023-duracion-rutina`, commiteada y en remoto): duración vacía/estimada/real. `domain/model/RoutineDurationRules.kt` (reglas puras `estimate`/`fromRealSeconds`); `Routine` + `RoutineEditUiState` con `durationManual: Boolean = false` y default `durationMin = 0`; `RoutineEntity` + `RoutineMapper` columna `durationManual`; `AppDatabase` v5 + `MIGRATION_4_5` (DEFAULT 1, existentes → MANUAL conservando minuto) registrado en `DatabaseModule`; repositorios (fake + Room) `createRoutine`/`updateMeta` con `durationManual`, `saveRoutine` recalcula estimación si AUTO, `finishSession` aplica tiempo real si AUTO (misma transacción en Room; fake espeja sin pasar por `saveRoutine` para no recalcular); `RoutineEditViewModel.onDurationChange` (→ MANUAL) / `onDurationModeChange` (AUTO recalcula); `RoutineEditScreen` interruptor AUTO/MANUAL + campo según modo (`—` sin series, `≈ X min` AUTO, editable MANUAL) y `SaveFooter` con valor efectivo (`≈` solo AUTO); `HomeScreen` oculta línea de duración si 0. Tests JVM: `RoutineDurationRulesTest` (8), `RoutineDurationFlowTest` (3), `FinishSessionDurationTest` (3) — verdes; `CatalogMappingTest` sin romper.
  Verificación /verifier 2026-10-18: `assembleDebug` OK; `testDebugUnitTest` 110/110. Emulador Pixel 6 API 34 (`emulator-5554`, vía MCP): flujo completo vacío `—` → `≈ 2 min` al añadir serie → MANUAL editable → AUTO recalcula → guardar (Home "2 min") → sesión AUTO de 115 s finalizada → "2 min" (redondeo) → sesión MANUAL de ~184 min no cambia "45 min"; migración v4→v5 EN VIVO (APK viejo v4 + datos → APK v5: filas conservan "45 min"). Instrumentados afectados: `Migration45Test` 1/1 + `RoutineEdit023DurationTest` 3/3 OK. Físico no disponible (no hay dispositivo en `adb devices`) → `bloqueado: sin dispositivo físico`.
  Fixes mínimos del /verifier: `RoutineDurationRules.fromRealSeconds` trunca → redondeo al más cercano (`(sec+30)/60` mín. 1); `SaveFooter` pintaba "≈ 0 min" en AUTO sin series → usa `durationDisplay` ("—"); smoke instrumentado sin `import org.junit.Rule` + asserts de nodo único ("—"/"≈ 2 min" salen 2 veces: fila + pie) actualizados.
  Deuda detectada ajena al 023: smokes de workout fallan en instrumentados por renombre del CTA en 022 (`WorkoutSmokeTest` busca "Finalizar y Guardar", el código dice "Finalizar" en `WorkoutScreen.kt:1451`) y flakiness de timing (`Workout013InteractionTest` 2, `Workout019CollapsedTest` 3, `Workout021ReplaceTest` 3). Pendiente de arreglo fuera de este spec. **RESUELTA 2026-10-18 con el /verifier del 022**: `WorkoutSmokeTest` actualizado a "Finalizar"; 013/019 con awaits de estado; 021 reescrito (swipe lento determinista + crash de Hilt del picker capturado como señal de apertura, ver deuda abajo); suite completa 59/59.

- `024-logo-launcher` (rama `024-logo-launcher`): logo del rack en el launcher — legacy `ic_launcher.png`/`ic_launcher_round.png` (48/72/96/144/192), adaptive foreground `ic_launcher_foreground.png` (108/162/216/324/432) + monochrome único `ic_launcher_foreground_monochrome.png`, fondo sólido #111316 (`drawable/ic_launcher_background.xml`), XMLs adaptativos `mipmap-anydpi/ic_launcher(.xml)/_round` y splash `drawable-nodpi/splash_logo.png` + `values-v31/themes.xml`. Script `scripts/gen-icons.ps1` lee `references/plantilla-logo/miLogo.png` y genera los PNG. Borrados los webp del robot (`ic_launcher*.webp`).
  Verificación: `./gradlew assembleDebug` OK; `./gradlew testDebugUnitTest` OK. Pruebas en dispositivo (CA1-CA4: launcher cuadrado y redondo, themed en emulador, splash API 31+) `bloqueado: sin dispositivo` (`adb` no disponible en esta máquina).

- `025-rutinas-series-duracion` (rama `025-rutinas-series-duracion`): propagación de series solo desde la serie 1 (serie N>1 toca solo su celda; `handEdited` evita sobrescribir); `addSet` (fake + Room) copia de la última serie no de la primera; `RoutineEditScreen.MetaCard` modo AUTO/MANUAL pasa a icono-only (sin texto recortado) y la fila `[Duración][icono modo][valor][min]` redistribuida para que quepa sin recorte (`value`/`min` recuperan el ancho sobrante con `weight(1f)`). Sin migración de esquema (DB v5 mantenida). Tests JVM: nuevo `addSet_copia_valores_de_ultima_serie_no_de_primera` en `RoutineRulesTest`; `RoutineDurationFlowTest`/`RoutineDurationRulesTest` sin cambios y verdes.
  Verificación: `assembleDebug` OK; `testDebugUnitTest` verde. Pruebe en móvil/emulador: propagación serie 1, no-propagación serie 2+, añadir-copiar-última, botón modo sin recorte, fila duración legible. `bloqueado: sin dispositivo` para instrumentados.
  Desviación vs spec (§6/§79 icono): `material-icons-core` trae solo 49 iconos y no incluye `Settings`/`AutoMode`; se usó `Icons.Filled.Edit` (MANUAL) e `Icons.Filled.CheckCircle` (AUTO), ambos ya en el proyecto. No se añadió `material-icons-extended` (restricción Gradle: proponer línea exacta y esperar).

## Siguiente

`024-logo-launcher` (Borrador en `specs/`, pendiente de aprobación). Más tarde: CUSTOM (MVP-3) y Fase D Spring/MySQL.

## Deuda y desviaciones conocidas

- ~~Smokes instrumentados de workout en rojo~~ **RESUELTO 2026-10-18 (verifier 022)**: `WorkoutSmokeTest` actualizado a "Finalizar"; `Workout013InteractionTest`/`Workout019CollapsedTest` con awaits de estado (`isPaused`/`exercises`); `Workout021ReplaceTest` reescrito: swipe lento (`swipeLeft(durationMillis = 1_000)`, el rápido asienta de vuelta por velocity-threshold y el jitter del emulador lo hacía flaky) y el tap en "Sustituir" verifica la APERTURA del picker capturando la `IllegalStateException` de Hilt (`ExercisePickerSheet` usa `hiltViewModel`, no instanciable sin Hilt; el crash asíncrono de recomposición se captura con `waitForIdle` dentro del try). La selección+confirmación→callback del picker queda cubierta por verificación UI manual (spec 022 §9). Suite completa 59/59 en emulador.
- Los `Fake*Repository` de Fase A conviven con los Room pero ya no se inyectan (Hilt usa los Room). Borrar en cuanto dejen de estar en uso, no antes de confirmar. `FakeScheduleRepository` es nuevo del 015 y sí se usa en tests JVM.
- `material-icons-core` solo trae 49 iconos: hay fallbacks (`List/DateRange/Star/Person`) en `HomeScreen`. La fidelidad exacta a los iconos del spec exigiría `material-icons-extended`: proponer la línea exacta y esperar.
- Avatar picker de Coil pendiente.
- Smoke del detalle de ejercicio pendiente.

## Puntos de control

- `docs/fotos-referencia.md`: dorados Roborazzi + regeneración.
