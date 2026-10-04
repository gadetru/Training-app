# Sesión en vivo – Rutina en entrenamiento (cronómetro + descanso bloqueante)
Estado: aprobado
Depende de: specs/004-editar-rutina-spec.md (reutiliza `RoutineDetail`/`FakeRoutineRepository` + patrón stateful+stateless; recablea el `onStart` que el 002 dejó como TODO) y specs/002-vista-principal-spec.md heredado (`NavHost` `TrainingNav`)
Fecha de creación: 2026-10-03
Descripción: Maquetar en Kotlin + Compose la vista `references/plantilla-rutina` como sesión de entrenamiento en vivo en memoria (header con cronómetro, progreso, acordeón de ejercicios con tabla de series KG/REPS editables + checks, overlay de descanso bloqueante con ±10s, barra fija Pausar/Finalizar/Descartar), abierta desde `Iniciar` de la Home en ruta nueva `workout?routineId`, con prefill última-sesión-vs-planificado. Sin Room, sin persistencia real (Fase A).
## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-rutina` como sesión verificable en móvil físico: nueva ruta `workout?routineId={routineId}`, cabecera de sesión activa con cronómetro contando el tiempo de entreno, tarjeta de progreso (`Ejercicio X de N`, `%`), acordeón de ejercicios con tabla de series (`SET/OBJETIVO/KG/REPS/PAUSA/ESTADO`), edición de KG/REPS con steppers, check por serie que dispara el descanso flotante de `references/plantilla-contador-descanso` (bloqueante hasta fin de tiempo o botón terminar, con botones `+10s/-10s`), footer fijo `Pausar / Finalizar y Guardar / minimizar` y acción secundaria `Descartar Entrenamiento`. Al iniciar, cada serie se rellena con la última sesión de ese ejercicio en ese día si existe, si no con lo planificado (`docs/MODELO_DE_DATOS.md:86`). `Iniciar` de la Home deja de ser TODO y navega a la sesión; `Finalizar` guarda en el fake y vuelve a Home.
## 2. Alcance (entra / no entra)
Entra:
- Nueva feature `feature/workout` con `WorkoutScreen` (stateful + contenido stateless, patrón de `HomeScreen.kt`/`RoutineEditScreen.kt`) + `WorkoutViewModel` fake con `StateFlow<WorkoutUiState>` (instanciado con `viewModel()` de `lifecycle-viewmodel-compose`, ya declarado en `app/build.gradle.kts:43`).
- Dominio en memoria `domain/model/Workout.kt` (`WorkoutSession`, `SetEntry`, `WorkoutExerciseUi`, `WorkoutUiState`); IDs UUID generados en cliente; `updatedAt/deleted` sync-ready; la UI solo ve `domain/model`.
- Repo fake nuevo `data/repository/FakeWorkoutRepository.kt` con interfaz `WorkoutRepository` de firma futura (`startSession(routineId): String`, `observeSession(id)`, `toggleSetDone`, `updateEntry`, `finishSession`, `discardSession`, `observeLastEntries` para prefill); en memoria con `MutableStateFlow` + `shared` como en `FakeRoutineRepository.kt:297-299`.
- Prefill al iniciar (`docs/MODELO_DE_DATOS.md:86`): última sesión de ese ejercicio en ese día si existe, si no valores planificados (`targetReps/weightKg/restSeconds/loadNote` de `Routine.kt:46-61`).
- Edición en vivo KG/REPS con steppers por serie + check por serie (volt `#CCFF00` al completar); `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `restSeconds` por serie intactos.
- Cronómetro de sesión arriba contando el tiempo de entreno (`code.html:144-151`, `28:45`) con `Pausar/Reanudar`; al completar una serie salta el descanso flotante (plantilla-contador-descanso) con cuenta atrás de `restSeconds` de esa serie, botones `+10s/-10s`, bloqueante (no se puede quitar hasta pasar el tiempo o pulsar `Terminar descanso`).
- Navegación: ruta `workout?routineId={routineId}` con `navArgument(nullable, defaultValue = null)` en `MainActivity.kt`; `HomeScreen.onStart` navega a la sesión; `Finalizar y Guardar` persiste en el fake y vuelve a Home; `Descartar Entrenamiento`/atrás sin finalizar descarta sin sesión fantasma.
- Edge-to-edge: cabecera con `statusBarsPadding()`, barra inferior fija con `navigationBarsPadding()` (conservando margen 16dp), sin dp fijos que imiten barras.
- Verificación de iconos nuevos contra el sources.jar de `material-icons-core`; fallbacks si falta alguno (precedente `HomeScreen.kt:69-84`).
No entra:
- Persistencia Room (`WorkoutSessionEntity`, `SetEntryEntity` de `docs/MODELO_DE_DATOS.md:62-79`, DAOs, `AppDatabase`, `Migration`, `exportSchema`, `schemas/`), repositorio real, Hilt (`TrainingApp.kt`, módulos DI), DataStore (`CATALOG_TAG`, `CatalogPrefs`).
- Descarga del catálogo (Retrofit, `ExerciseApi.kt`, DTOs, `GET api/{lang}/exercises.json`) — Fase C; ejercicios propios `source=CUSTOM` persistidos.
- `Añadir Ejercicio` en vivo (`code.html:181-184`) ni `Añadir Serie` / quitar serie en vivo — la sesión corre con lo planificado; reutilizar el picker del 003 queda para futuro spec.
- RPE (`RPE 8.5`), badges `DROP`, tipos calentamiento/normal/al fallo editables en vivo, historial/notas (`edit_note`), menús (`more_vert/more_horiz`), minimizar a segundo plano real / picture-in-picture (`code.html:452-454` solo como TODO visual sin crash).
- Duplicar/eliminar rutinas, reordenar ejercicios por drag, accesorios reutilizables (`docs/MODELO_DE_DATOS.md:105`).
- Detalle de ejercicio (GIF grande + instrucciones, MVP item 2).
- Cambios en Gradle (`gradlew`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` raíz y `app/`): prohibidos.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo final exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`docs/ARQUITECTURA.md:11-17,25-30`, `AGENTS.md`). En este spec Fase A solo se hace `UI + ViewModel fake + repo fake en memoria`, sin Room.
- Real hoy: `feature/home` (`HomeScreen.kt:93-114` stateful+stateless, `HomeViewModel.kt:31-50` combina `FakeHomeRepository + FakeRoutineRepository.shared`), `feature/routines` (`RoutineEditScreen.kt:1-86` + `RoutineEditViewModel` + `FakeRoutineRepository.kt:39-79` firma futura con borradores separados de guardadas), `feature/exercises` (picker + `FakeExerciseRepository`), `domain/model/{Home,Exercise,Routine}.kt`. No existe `feature/workout/` ni `domain/model/Workout.kt` (glob `app/src/main` solo tiene `feature/{home,profile,routines,exercises}/`).
- `MainActivity.kt:52-89` (`TrainingNav` con rutas `home/calendar/progress/profile` + `routineEdit?routineId`); `HomeScreen` se llama en `MainActivity.kt:65-77` solo con `onCreate`, `onStart` queda por defecto `{}` (TODO a cablear aquí).
- Paquete fijado `com.mytrainingplan.app` (`app/build.gradle.kts:7,13`), `minSdk 26`, `compileSdk/targetSdk 37`, Java 11, Kotlin `2.2.10`, AGP `9.4.1`, Compose BOM `2026.02.01`. Deps suficientes ya declaradas: `navigation-compose 2.7.7`, `coil-compose 2.6.0`, `material-icons-core`, `lifecycle-viewmodel-compose` (`app/build.gradle.kts:42-45`, `gradle/libs.versions.toml:9-12,28-32`).
- Referencia visual: `references/plantilla-rutina/screen.png`, `code.html:137-170` (header sesión activa: minimizar, timer `28:45`, título `Torso - Fuerza & Hipertrofia`), `code.html:174-190` (progreso `Ejercicio 2 de 6`, `45%`, `Añadir Ejercicio`), `code.html:194-209` (ejercicio 1 completado colapsado), `code.html:211-368` (ejercicio 2 activo expandido con tabla `SET/OBJETIVO/KG/REPS/PAUSA/ESTADO` y serie 3 `ACTIVA`), `code.html:369-428` (ejercicios 3-6 contraídos), `code.html:431-435` (`Descartar Entrenamiento`), `code.html:439-455` (barra fija `Pausar / Finalizar y Guardar / minimizar`). Descanso: `references/plantilla-contador-descanso/` (flotante con cuenta atrás).
- Sets: reglas intactas según `docs/MODELO_DE_DATOS.md:49-87`; entidades objetivo Room ya definidas en `docs/MODELO_DE_DATOS.md:62-79` (Fase B las implementa tal cual). Este spec aplica el punto 4 (`docs/MODELO_DE_DATOS.md:86`, prefill última sesión) en memoria.
- Corrección de flujo: el `onStart(routineId)` del 002 era TODO sin navegación; este spec lo cablea a la ruta nueva sin romper `onCreate → routineEdit` del 004.
## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Pulsar `Iniciar` en cualquier card de Home navega a `workout?routineId={id}` con la rutina resuelta (nombre + ejercicios + series prefilladas); sin `routineId` o id inexistente muestra estado vacío sin crash y permite volver.
- Mostrar header de sesión activa: botón minimizar/volver, cronómetro contando (`mm:ss`, punto vivo), título de la rutina + etiqueta `Entrenamiento Activo`, como en `code.html:137-170`.
- Mostrar tarjeta de progreso global: `Ejercicio X de N` + `% completado` + barra de la rutina completa, como en `code.html:174-190` pero sin botón `Añadir Ejercicio` (no entra).
- Mostrar acordeón por ejercicio: completado colapsado (`4 de 4 series completadas • 85 kg máx`, check volt), activo expandido con tabla de series, pendientes contraídos (`N series programadas`), como en `code.html:194-428`.
- Tabla por serie: nº, objetivo (`70 kg x 8`), KG editable con stepper, REPS editable con stepper, pausa (`90s`), estado (check pendiente → botón `done` naranja en activa → check volt al completar), como en `code.html:243-348`.
- Completar una serie (`done`) la marca volt y dispara el overlay de descanso con la cuenta atrás de `restSeconds` de esa serie; el overlay tiene `+10s/-10s` y `Terminar descanso`; no se puede descartar por toque fuera ni por atrás hasta que termine el tiempo o se pulse terminar (decisión de interrogatorio 2026-10-03).
- Barra fija inferior: `Pausar/Reanudar` (congela el cronómetro), `Finalizar y Guardar` (persiste la sesión en el fake, vuelve a Home), minimizar (TODO visual: vuelve atrás sin cerrar la sesión en memoria, sin crash).
- `Descartar Entrenamiento` pide confirmación y descarta la sesión sin guardar y sin crash ni sesión fantasma.
- Rotar conserva todo el estado en sesión (`StateFlow` en `ViewModel`: timer, checks, KG/REPS, descanso restante, pausa).
No-funcionales:
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, volt `#CCFF00`, radios y espaciados de `DESIGN.md`).
- Sin jank en scroll (listas perezosas), hit targets >= 48dp, bottom padding para que la barra fija no tape contenido, prueba en móvil físico sin emulador.
- Dominio y repo fake listos para Fase B: UUIDs, `updatedAt + deleted`, futuro `@Upsert` nunca `@Insert(REPLACE)`.
- Edge-to-edge: header con `statusBarsPadding()`, barra fija con `navigationBarsPadding()`; prohibido compensar con dp fijos extra.
## 5. Criterios de aceptación verificables
- En móvil físico la sesión es copia de `screen.png`: header con timer contando, progreso, acordeón (1 completado, 1 activo expandido, resto contraídos), tabla de series, `Descartar Entrenamiento`, barra fija `Pausar / Finalizar y Guardar / minimizar`.
- Pulsar `Iniciar` en una card de Home abre la sesión de esa rutina con sus ejercicios y series prefilladas, sin crash.
- Al iniciar, una serie con historial previo muestra los valores de la última sesión; sin historial muestra lo planificado.
- Editar KG/REPS con steppers cambia solo esa serie sin crash y se conserva al rotar.
- Pulsar `done` en la serie activa la marca volt y abre el descanso flotante con la cuenta atrás de su `restSeconds`.
- El descanso no se puede quitar por toque fuera ni por atrás; `+10s/-10s` ajustan de 10 en 10; al llegar a 0 o pulsar terminar se cierra sin crash.
- `Pausar` congela el cronómetro y cambia a `Reanudar`; el tiempo no avanza en pausa.
- `Finalizar y Guardar` vuelve a Home sin crash y la sesión queda guardada en el fake (verificable en la siguiente apertura como prefill).
- `Descartar Entrenamiento` (con confirmación) o salir atrás sin finalizar descarta sin guardar y sin sesión fantasma, sin crash.
- Rotar conserva timer, checks, KG/REPS, descanso restante y pausa en sesión.
- Paquete `com.mytrainingplan.app`, sin `.debug`.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `domain/model/Workout.kt` nuevo: `WorkoutSession(id: String /* UUID */, routineId: String, startedAt: Long, endedAt: Long? = null, pausedAccumSec: Int = 0, updatedAt = 0L, deleted = false)` + `SetEntry(id /* UUID */, sessionId, exerciseId: String, plannedSetId: String?, setNumber: Int, reps: Int, weightKg: Double /* con signo */, loadNote: String? = null, restSeconds: Int? = null, done: Boolean = false, updatedAt, deleted)` + `WorkoutExerciseUi(routineExercise: RoutineExercise, exercise: Exercise, entries: List<SetEntry>, expanded: Boolean = true)` + `WorkoutUiState(sessionId, routineName: String, elapsedSec: Int, isPaused: Boolean, exercises: List<WorkoutExerciseUi>, doneCount: Int, totalCount: Int, restRemainingSec: Int? = null, restTotalSec: Int = 0, isSaving: Boolean = false)` con `progressFraction` y `currentExerciseIndex`. UI solo ve dominio.
- `data/repository/FakeWorkoutRepository.kt` nuevo + interfaz `WorkoutRepository` de firma futura (`startSession(routineId: String): String`, `observeSession(id): Flow<WorkoutSessionDetail?>`, `toggleSetDone(sessionId, entryId, done: Boolean)`, `updateEntry(sessionId, entryId, reps: Int, weightKg: Double)`, `adjustRest(entryId, deltaSec)` solo pantalla o persistido mínimo, `finishSession(sessionId)`, `discardSession(sessionId)`, `observeLastEntries(routineId, exerciseId): Flow<List<SetEntry>>` para prefill). En memoria con `MutableStateFlow<Map<String, WorkoutSessionDetail>>` + historial de finalizadas; `shared` como en 004. Misma firma que el futuro repositorio real (Room).
- `feature/workout/WorkoutViewModel.kt` nuevo (fake Fase A): crea `sessionId` al iniciar con `startSession(routineId)` (prefill: `observeLastEntries` si hay, si no `PlannedSet` de `RoutineDetail`); `StateFlow<WorkoutUiState>` con timer (`elapsedSec` vía ticker, pausable); eventos `onKgChange/onRepsChange` (solo esa entrada), `onToggleDone` (marca volt + arranca `restRemainingSec = entry.restSeconds`), `onRestPlusMinus10`, `onRestFinish`, `onPauseToggle`, `onFinish`, `onDiscard`; sin Hilt.
- `feature/workout/WorkoutScreen.kt` nuevo: `WorkoutScreen(sessionId: String?, routineId: String?, onFinished, onDiscard, onBack)` stateful + `WorkoutContent` stateless; `SessionHeader` (minimizar + timer + `more_vert` TODO), `ProgressCard` (sin añadir), `ExerciseAccordion` (`ExerciseHeader` con estado completado/activo/pendiente + expandir), `SetsTable` (filas con steppers + `done`/check), `DiscardButton` (con diálogo de confirmación), `SessionBar` fija (`Pausar / Finalizar y Guardar / minimizar`), `RestOverlay` (flotante bloqueante: cuenta atrás + `+10s/-10s` + `Terminar descanso`, sin dismiss por fuera). Tokens locales como en 001/002/003/004; tipografías de sistema; iconos `material-icons-core` (verificar `timer/pause/flag/done/check/expand_more` en el sources.jar; fallback listo si falta).
- `MainActivity.kt`: ruta `workout?routineId={routineId}` con `navArgument(nullable, defaultValue = null)`; `HomeScreen.onStart = { navController.navigate("workout?routineId=$it") }`; `onFinished/onDiscard = popBackStack + navigate(home)` (mismo patrón que `ROUTINE_EDIT` en `MainActivity.kt:78-96`). Resto del `TrainingNav` intacto.
- `HomeScreen.kt`/`HomeViewModel.kt`: solo cablear `onStart`; el feed no cambia (las rutinas creadas en 004 ya aparecen vía `observeDetails()` en `HomeViewModel.kt:36-44`).
- Room/DAO/Migration/mapper/DTOs/`ExerciseApi`/catálogo/`CATALOG_TAG`/Hilt: NO se tocan. Sets (`weightKg` signo, `loadNote`, `restSeconds` por serie): NO cambian de semántica, solo se ejecutan en vivo.
- Qué NO se toca (gotchas `AGENTS.md`): no cambiar `applicationId`, no `applicationIdSuffix ".debug"`, misma keystore fuera del repo + bump `versionCode` solo en release, `gradle-wrapper.jar` ausente (abrir en Android Studio primero), sin emulador, `dynamicColor=false` para fidelidad.
## 7. Plan de tareas
1. Dominio en memoria `domain/model/Workout.kt` (`WorkoutSession` + `SetEntry` + `WorkoutExerciseUi` + `WorkoutUiState`, UUIDs + `updatedAt/deleted`).
2. Crear `data/repository/FakeWorkoutRepository.kt` + interfaz `WorkoutRepository` de firma futura, en memoria con `shared` (start con prefill última-vs-plan, observe, toggle/update, finish/discard).
3. Crear `feature/workout/WorkoutViewModel.kt` fake con `StateFlow<WorkoutUiState>` (timer pausable + completar→descanso + `±10s` + bloqueo + finish/discard).
4. Crear `feature/workout/WorkoutScreen.kt` (header + progreso + acordeón + tabla con steppers + descartar + barra fija + overlay descanso bloqueante) como copia Compose de `screen.png` / `code.html`.
5. Navegación: ruta `workout?routineId={routineId}` en `MainActivity.kt`; cablear `HomeScreen.onStart` (hoy TODO) a la sesión; vuelta a Home al guardar/descartar.
6. Verificar iconos (`timer`, `pause`, `flag`, `done`, `check`, `expand_more` y los usados) contra el sources.jar de `material-icons-core`; aplicar fallbacks si falta alguno.
7. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico (sesión desde `Iniciar`, prefill, steppers, checks+descanso bloqueante, pausa/timer, finalizar visible como prefill siguiente, descartar, rotación).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo ni Room).
- Prueba en móvil físico (sin emulador): comparar con `screen.png`; abrir desde `Iniciar`; prefill última-vs-plan; steppers KG/REPS; `done` → descanso bloqueante con `±10s` y cierre solo por fin/terminar; pausar congela timer; finalizar guarda (siguiente apertura lo usa como prefill); descartar no deja fantasma; rotar conserva estado en sesión.
## 9. Riesgos / No romper
- `onStart` pasa de TODO a navegación real: no romper `onCreate → routineEdit` del 004 ni las rutas `home/calendar/progress/profile` de `MainActivity.kt:64-121`.
- Sesiones a medias: salir atrás/minimizar sin finalizar nunca deja sesiones fantasma en el feed ni corrompe el prefill (borrador separado como en `FakeRoutineRepository.kt:94-95`).
- Timer en `ViewModel` (no en Composable) para sobrevivir a recomposición y rotación; pausar congela el acumulado, no resetea.
- Overlay de descanso bloqueante: sin dismiss por toque fuera ni por atrás del sistema (`BackHandler` consumido mientras haya descanso); hit targets >= 48dp; padding inferior para no tapar la barra fija.
- `weightKg` con signo desde el día uno (`0.0` corporal por defecto heredado del plan); `restSeconds` por serie (el del plan), nunca global.
- En Fase B: solo `@Upsert`, nunca `@Insert(REPLACE)`; `Migration` real + `exportSchema=true` con schemas en `app/schemas/`; update de catálogo solo filas `source=CATALOG`.
- No cambiar `applicationId/com.mytrainingplan.app` ni añadir `.debug`; no mover keystore al repo; bump `versionCode` solo en release.
- UI solo ve `domain/model`; mapping en `data/mapper` cuando exista.
- No añadir dependencias Gradle en este spec (navigation + viewmodel + iconos core + Coil ya están); si hiciera falta otra, proponer línea exacta y esperar (restricción `AGENTS.md`).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- `anadir-en-sesion`: botón `Añadir Ejercicio` (`code.html:181-184`) reutilizando el picker del 003 + `Añadir Serie` en vivo — no entra aquí.
- `descanso-avanzado`: sonido/vibración al terminar, `+15s/-15s` configurables, presets por ejercicio — detectado en `references/plantilla-contador-descanso/`.
- `rpe-notas-historial`: RPE por serie, `DROP`, `edit_note`, `more_horiz`, badges `Enfoque: Hombro & Tríceps` (`code.html:224-228`) — no entra aquí.
- `sesion-background`: minimizar real a segundo plano con notificación persistente (el botón `code.html:452-454` queda como TODO visual).
- `home-post-sesion`: actualizar `lastDoneLabel`/progreso/racha de Home tras finalizar (hoy TODO Fase B) — detectado en `HomeViewModel.kt:56-72`.
- `detalle-ejercicio`: tap en la fila abre detalle fake (GIF grande + instrucciones + secundarios); base MVP item 2 — heredado del 003.
- Fase B Room sesiones: `WorkoutSessionEntity` + `SetEntryEntity` (`docs/MODELO_DE_DATOS.md:62-79`) + DAOs `@Upsert/observe/softDelete` + `AppDatabase Migration` + repositorio real.
- Tema: migrar tokens locales a `ui/theme/` y tipografías Outfit/Plus Jakarta Sans/Space Grotesk (pendiente desde 001/002/003/004).
## Preguntas abiertas
- ¿El descanso usa siempre `restSeconds` de la serie completada o hay valor global por defecto si es null? Propuesta: el de la serie; si null, 90s como en `FakeRoutineRepository.kt:285-295`.
- ¿El timer debe seguir con pantalla apagada en Fase A? Propuesta: no (solo mientras la pantalla está encendida; background real va en futuro spec).
- ¿`Finalizar` debe actualizar racha/progreso de Home en este spec o queda TODO Fase B? Propuesta: TODO (el feed no cambia salvo el prefill de la próxima sesión).
- ¿Minimizar cierra la vista manteniendo la sesión viva en el repo o la pausa automáticamente? Propuesta: mantiene viva sin pausar, vuelta sin pérdida.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Sesión copia `screen.png` verificada en móvil físico (header con timer, progreso, acordeón, tabla, descartar, barra fija)
- [ ] `Iniciar` en Home abre la sesión de esa rutina sin crash
- [ ] Prefill última sesión si existe, si no planificado
- [ ] Stepers KG/REPS editan solo esa serie sin crash y sobreviven a rotación
- [ ] `done` marca volt y abre descanso con la cuenta atrás de su `restSeconds`
- [ ] Descanso bloqueante (no cierra por fuera/atrás); `+10s/-10s` ajustan de 10 en 10; cierra por fin o terminar sin crash
- [ ] `Pausar` congela el cronómetro; `Reanudar` lo retoma
- [ ] `Finalizar y Guardar` vuelve a Home sin crash y la sesión sirve como prefill siguiente
- [ ] `Descartar`/atrás sin finalizar descarta sin fantasma y sin crash
- [ ] Rotación conserva timer, checks, KG/REPS, descanso y pausa en sesión
- [ ] Paquete `com.mytrainingplan.app`, sin `.debug`
- [ ] `./gradlew assembleDebug` OK
- [ ] Prueba en móvil físico OK
