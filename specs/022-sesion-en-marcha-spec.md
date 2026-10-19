# Rutina en marcha (retomar sin sesiones fantasma)
Estado: implementado
Depende de: specs/016-sesion-edicion-spec.md (el ⋮ ya trae "Salir del entrenamiento") y specs/021-sustituir-ejercicio-spec.md (misma tela repo/VM; implementar 021 antes)
Fecha de creación: 2026-10-18
Descripción: Salir de la sesión (∨/←/⋮ "Salir del entrenamiento") sin finalizar ni descartar deja constancia: la card de esa rutina en Home pasa a EN MARCHA con contador, Reanudar, Terminar y Descartar (con confirmación); los Iniciar del resto se deshabilitan hasta cerrar. El cronómetro sobrevive a salir, minimizar y matar la app (migración v4). Iniciar naranja solo para la rutina programada hoy.
## 1. Objetivo
Que salir de una sesión sin cerrarla no la pierda: Home la muestra, permite retomarla donde se dejó (series y cronómetro reales) y evita crear sesiones duplicadas por descuido.
## 2. Alcance (entra / no entra)
Entra:
- Detección de sesión viva: `endedAt IS NULL AND deleted = 0` (estado real actual).
- Card EN MARCHA en Home: la fila CTA de esa rutina pasa a Reanudar + Terminar + Descartar y muestra contador.
- Todos los demás Iniciar deshabilitados mientras hay una en marcha (la única salida es cerrar la viva).
- Terminar desde Home = `finishSession` (guarda, marca `endedAt`; el apunte/auto-reemplazo ya vive en el repo); reactiva los Iniciar vía Flow, sin recarga.
- Descartar desde Home con diálogo de confirmación (mismo patrón que el Descartar de la sesión, con su aviso).
- Guardia anti-doble: `startSession` no crea una segunda si ya hay viva (devuelve la viva + `Log`).
- Cronómetro persistente: migración DB v3→v4 con 2 columnas (`timerBaseSec`, `runningSince`) + fórmula de reloj; persiste en Comenzar/Pausar/Reanudar y cada 15 s de tick (kill-safe aproximado).
- Iniciar naranja solo si la rutina está programada HOY (sustituye el ciclo decorativo del CTA; la barrita lateral de acento no se toca).
- Renombrar el CTA de la sesión: "Finalizar y Guardar" → "Finalizar".
No entra:
- Notificaciones/alarmas del descanso o recordatorios de rutina programada.
- Sesiones múltiples simultáneas (solo una viva por definición).
- Pausa automática al cerrar la app (si estaba corriendo, sigue corriendo: es "en marcha").
- Cambios en el descanso flotante, colapso (019) o edición/sustitución en vivo (016/021).
- Botón ← del sistema (navega a Home igual que hoy: minimizar es el efecto natural de `popBackStack`).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Hoy: `WorkoutSessionDao` no tiene consulta de vivas; `startSession` (`RoomWorkoutRepository.kt:60-105`) crea SIEMPRE una nueva (cada Iniciar = otra fantasma); `HomeUiState` no sabe de sesiones vivas (`domain/model/Home.kt:57-65`); el CTA naranja es el ciclo decorativo (`RoomHomeRepository.kt:36,49` + `HomeScreen.kt:471` `primary = routine.accent == AccentColor.ORANGE`); el cronómetro solo vive en `Ephemeral.elapsedSec/isPaused` (`WorkoutViewModel.kt:30-40`) y `pausedAccumSec` existe pero sin uso; el ⋮ ya ofrece "Salir del entrenamiento" sin finalizar (`WorkoutScreen.kt:497-503`). Capas: DAL (DAO/entidad/mapper) solo sobre `workout_sessions` (DB v4); dominio limpio en `WorkoutSession`/`WorkoutUiState`/`HomeUiState`; la UI solo ve modelos de dominio. Para la programada-hoy existe `ScheduledDayDao.observeByDate` (`ScheduledDayDao.kt:22-23`) y el patrón de zona ya está en `RoomWorkoutRepository.kt:51` (`zone = ZoneId.systemDefault()` + `HistoryCalcs.dateKeyOf`).
## 4. Requisitos funcionales + no-funcionales
- Card en marcha: contador "EN MARCHA hh:mm" (reloj: `timerBaseSec + (now - runningSince)/1000`, tick local de 1 s sin lecturas DB), Reanudar (estilo primario, tag `resumeWorkout:<routineId>`), Terminar (tag `finishWorkout:<routineId>`), Descartar (tag `discardWorkout:<routineId>` + AlertDialog de confirmación).
- Reanudar navega a `workout` de esa rutina con `sessionId`: mismas series conservadas y cronómetro real por la fórmula (ACTIVO si `runningSince != null`; EN PAUSA si `base > 0` y `running = null`; SIN EMPEZAR si `base = 0` y `running = null`, de modo que una sesión fresca conserva el arranque del 013: 00:00 + Comenzar).
- Terminar desde Home sin entrar a la sesión; Descartar pide confirmación idéntica a la del Descartar dentro de la sesión.
- Migración sin pérdida: DB v4, `MIGRATION_3_4` con `ADD COLUMN` (defaults neutrales, no tocan filas existentes); `exportSchema = true` y JSON regenerado.
- Edge-to-edge: el diálogo lo gestiona Material3 (exención de insets); la card vive en el listado existente.
- Sin Gradle/keystore/versionCode; sin iconos nuevos (píldoras de texto, patrón del Iniciar); botes `ui/theme`.
## 5. Criterios de aceptación verificables
- Crear sesión → salir con ←: la card de esa rutina muestra EN MARCHA + contador corriendo + Reanudar/Terminar/Descartar; los Iniciar del resto no son clicables.
- Reanudar: mismas series (hechas y pendientes) y cronómetro continuado si corría / congelado si estaba en pausa / 00:00 Comenzar si nunca se empezó.
- Terminar: la sesión queda finalizada (Calendario y prefill la ven), la card vuelve a "Iniciar" y el resto se reactiva.
- Descartar: diálogo → sí → sin sesión viva; Iniciar reactivados.
- Con una sesión viva, "Iniciar" no crea una segunda (guardia con `Log`: devuelve la viva).
- Pausar → matar la app → volver: EN PAUSA con el tiempo congelado persistido.
- `./gradlew assembleDebug` OK; `./gradlew testDebugUnitTest` verde; `./gradlew connectedDebugAndroidTest` verde (emulador Pixel 6 API 34 + físico; nota MIUI).
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `WorkoutSessionDao`: `@Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL AND deleted = 0 ORDER BY startedAt DESC") fun observeLive(): Flow<List<WorkoutSessionEntity>>`.
- Entidad + dominio: `WorkoutSessionEntity` y `WorkoutSession` ganan `timerBaseSec: Int = 0` y `runningSince: Long? = null` (+ mapper `toEntity/toDomain`); `pausedAccumSec` no se toca (queda como histórico).
- Dominio puro `WorkoutSession.timerElapsedSec(nowMs)` y `WorkoutSession.timerState` (SIN EMPEZAR / EN PAUSA / ACTIVO): fórmula en `domain/model/Workout.kt`, JVM-testable.
- `WorkoutRepository` + Fake: nuevos `suspend fun timerStart(sessionId, nowMs)`, `suspend fun timerPause(sessionId, nowMs)`, `suspend fun timerSync(sessionId, nowMs)` (Room: `@Upsert` con la fórmula; Fake: espejo en memoria; 3 métodos explícitos).
- Guardia: `startSession` consulta `observeLive().first()`; si hay viva → `Log.w` + devuelve su id sin crear fila (comportamiento de retomar).
- `WorkoutViewModel`: `openSession(routineId, sessionId?)` (si `sessionId` != null observa la existente; si no, crea); `Ephemeral` pierde `elapsedSec/isPaused` (derivados del detail + tick de 1 s que sigue gobernando la cuenta atrás del descanso); `onPauseToggle` llama `timerStart`/`timerPause` según estado derivado; tick de 15 s llama `timerSync` si corre; el CTA pasa a "Finalizar".
- `Home.kt`: `HomeUiState.liveRoutineId: String?`, `HomeUiState.liveElapsedSec: Int` y `HomeUiState.scheduledTodayId: String?` (dominio puro; snapshots).
- `HomeRepository` (+ Fake espejo): `observeHome()` mantiene firma; `RoomHomeRepository` añade `sessions.observeLive()`, `scheduleDao` y `zone` al constructor (patrón vigente) → combina y marca `liveRoutineId`, su contador, y `scheduledTodayId` (primera fila de hoy con `HistoryCalcs.dateKeyOf(now, zone)`).
- `HomeScreen`: en la rutina viva la fila CTA (`HomeScreen.kt:470-494`) pasa a Reanudar (primario) + Terminar + Descartar, con pie "EN MARCHA hh:mm" (tick con `LaunchedEffect` de 1 s solo en esa card); el resto usa `primary = routine.id == scheduledTodayId`; los Iniciar no clicables vía `clickable(enabled = false)` + colores de deshabilitado.
- `MainActivity`: `ROUTE_WORKOUT = "workout?routineId={routineId}&sessionId={sessionId}"` (ambos nullable) y callbacks de Home: `onLiveResume` navega con `routineId` + `sessionId`, `onLiveFinish` → `HomeViewModel.onFinishLive()`, `onLiveDiscard` → diálogo → `HomeViewModel.onDiscardLive()`.
## 7. Plan de tareas
1. DB v4: entidad + dominio + mapper + `MIGRATION_3_4` (ADD COLUMN x2) + `Migration34Test` (espejo `src/androidTest/`, patrón `Migration23Test`).
2. `WorkoutSessionDao.observeLive` + guardia en `startSession` (devuelve la viva, no crea fila).
3. Timer en repo: fórmula/estados en dominio + `timerStart/timerPause/timerSync` en interface, Room y Fake (un paso que compile).
4. `WorkoutViewModel`: `openSession(routineId, sessionId)`, estados derivados, pausa/reanudar persistidos, tick de 15 s y CTA "Finalizar".
5. `Home.kt` + `RoomHomeRepository` (viva + programada hoy + zona) + `HomeRepository`/Fake.
6. `HomeScreen`: card en marcha (Reanudar/Terminar/Descartar + contador + tags), `primary` = programada hoy, resto no clicable.
7. `MainActivity`: ruta con `sessionId` y wiring de callbacks de Home.
8. Tests JVM: `TimerRulesTest` (fórmula + estados), `HomeLiveTest` (flags del feed con fakes: viva, hoy, contador) y guardia anti-doble en fake.
9. Smoke instrumentado `Workout022ResumeTest` (salir → card viva → Reanudar conserva series y cronómetro → Terminar reactiva) + tags de la card de Home.
10. Verificación triple: `assembleDebug` + `testDebugUnitTest` + `connectedDebugAndroidTest` (emulador y físico; en MIUI, instalación manual + `am instrument`).
## 8. Verificación (assembleDebug + CatalogMappingTest + prueba en móvil físico)
- `./gradlew assembleDebug` OK; `CatalogMappingTest` como regresión (no se toca catálogo).
- `./gradlew testDebugUnitTest` verde con las nuevas reglas JVM.
- `./gradlew connectedDebugAndroidTest` verde: DAO de vivas, `Migration34Test` y smokes del flujo con la migración aplicada (patrón 015/016).
- Prueba manual: pausar → matar la app → volver; salir → card viva; Terminar desde Home; Reanudar conserva series y cronómetro, en emulador y dispositivo físico.
## 9. Riesgos / No romper
- Migración v3→v4 OBLIGATORIA preservando datos (ADD COLUMN con defaults neutrales; `fallbackToDestructiveMigration()` solo en desarrollo, nunca en la migración real).
- 013: una sesión fresca mantiene 00:00 + Comenzar (no auto-arranca); los cambios de cronómetro solo derivan del estado de DB.
- 016: "Salir del entrenamiento" sigue minimizando (no toca `endedAt` ni colores).
- 019/021: sin cambios en colapso ni sustitución; solo se añade la derivación del cronómetro.
- Home: no se toca el layout de la card fuera de la fila CTA; la barrita de acento y el pie quedan intactos.
- Tests existentes (`WorkoutLiveEditTest` 7/7, `Workout016LiveEditTest` 6/6, picker 4/4, 015) no deben depender de `Ephemeral.elapsedSec/isPaused`; si alguno lo hace, se ajusta en el paso 4 y se documenta.
- Sin multi-sesión: la guardia evita duplicados; las vivas históricas se ordenan por `startedAt DESC` (la más reciente manda).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Notificaciones/alarmas del descanso y recordatorio de "rutina programada hoy".
- Instantánea del estado de la sesión al matar la app a nivel de UI (la migración v4 ya lo deja cerca).
- Duración auto/manual de la rutina (`023-...-spec.md`): la sesión real finalizada alimenta `durationMin`.
- Reanudar desde el propio aviso de "EN MARCHA" con horas y progreso (x/N series) en la card.
## Preguntas abiertas
- Decidido con el usuario: migración v4 sí; todos los Iniciar bloqueados con Reanudar/Terminar/Descartar en la card en marcha; Descartar con confirmación; naranja = programada hoy; deps 016+021; pruebas JVM + smoke.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Salir con ←/∨/⋮ tras marcar series: card de Home EN MARCHA con contador corriendo. (UI adb: back tras marcar → "EN MARCHA 01:46"→02:38 corriendo; `build/022-01-home-en-marcha.png`)
- [x] Reanudar conserva series hechas/pendientes y cronómetro (activo sigue, pausa congelada). (UI adb: Reanudar → ACTIVO 03:21, "100% completado", "1 de 1 series" (`build/022-02-reanudada.png`); instrumentado `Workout022ResumeTest` 2/2: doneCount=1 y reloj que crece tras reanudar)
- [x] Los Iniciar del resto quedan deshabilitados hasta cerrar la viva. (Instrumentado: `assertCountEquals(1)` + `assertIsNotEnabled()` con r1 viva; UI adb: Rutina 2 con Iniciar atenuado/gris con viva en Rutina 1, `build/022-07-cta-colores.png`)
- [x] Terminar desde Home finaliza la sesión (Calendario y prefill la ven) y reactiva los Iniciar. (UI adb: Terminar → "Iniciar" de vuelta + "Hoy" + racha 1, `build/022-04-terminada-iniciar.png`; instrumentado: `observeSession(sid)==null` y `startSession` crea id nuevo)
- [x] Descartar desde Home pide confirmación y borra la sesión viva. (UI adb: diálogo "¿Descartar entrenamiento?" → Descartar → Iniciar de vuelta, `build/022-05-dialogo-descarte.png` + `build/022-06-descartada.png`)
- [x] Un Iniciar no crea segunda sesión (guardia devuelve la viva). (JVM `StartSessionGuardTest` 2/2 dentro de 110 verdes; guardia en `RoomWorkoutRepository.kt:62-68` con `Log.w`; UI adb: doble tap en Iniciar → una sola sesión/card EN MARCHA. Nota: el `Log.w` no es disparable por UI — el 2º tap cae ya en la pantalla de sesión; cubierto por el test JVM)
- [x] CTA naranja solo si la rutina está programada para HOY (barrita de acento intacta). (UI adb: Rutina 1 programada hoy tras Terminar → Iniciar naranja; Rutina 2 sin programar → Iniciar gris atenuado; lógica `HomeScreen.kt:619-623` `primary = id == scheduledTodayId`; `RoomWorkoutRepository.kt:339` programa al finalizar)
- [x] Pausar y matar la app → al abrir: tiempo congelado persistido (migración v4). (UI adb: pausa 03:53 → force-stop → relanzar → Reanudar → "ENTRENAMIENTO EN PAUSA" 03:53 congelado (2 lecturas a 3 s), `build/022-03-en-pausa-congelada.png`; `Migration34Test` verde en instrumentados)
- [x] `./gradlew assembleDebug` OK. (BUILD SUCCESSFUL)
- [x] `./gradlew testDebugUnitTest` verde. (110 tests, 0 fallos)
- [X] `./gradlew connectedDebugAndroidTest` verde en emulador y en físico. (Emulador Pixel 6 API 34: 59/59 verde. Físico: bloqueado — `ae4a376e` desapareció de `adb devices` a mitad de sesión; reintentar cuando reconecte, con instalación manual + `am instrument` si MIUI rechaza el `-g` de AGP)
