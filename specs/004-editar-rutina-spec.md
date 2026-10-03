# Constructor de Rutina – Crear/Editar (recablea el sheet de ejercicios)
Estado: aprobado
Depende de: specs/003-lista-ejercicios-spec.md (reutiliza `ExercisePickerSheet` + `ExercisesViewModel` + `FakeExerciseRepository`; recablea su apertura provisional desde `Crear Rutina +`) y specs/002-vista-principal-spec.md (`NavHost` de `MainActivity`, patrón stateful+stateless, tokens locales)
Fecha de creación: 2026-10-03
Descripción: Maquetar en Kotlin + Compose la vista `references/plantilla-editar-rutina` como constructor de rutina en memoria (título, duración, tags auto por músculo, acordeón de ejercicios con matriz de series y tipos calentamiento/normal/al fallo, `+ Añadir Nuevo Ejercicio a la Rutina`, footer `Finalizar y Guardar Rutina`), abierto desde `Crear Rutina +` de la Home en ruta nueva, con el sheet del 003 anidado solo desde el botón de añadir. Sin Room, sin persistencia real (Fase A).
## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-editar-rutina` como constructor verificable en móvil físico: nueva ruta `routineEdit?routineId={routineId}` (crear nueva; el arg deja lista la edición futura), cabecera con título y duración editables + tags automáticos por músculo, acordeón de ejercicios con matriz de series editables (reps, peso con signo, descanso por serie, nota, tipo), reglas de propagación de `docs/MODELO_DE_DATOS.md:81-87`, botón `+ Añadir Nuevo Ejercicio a la Rutina` que abre el `ExercisePickerSheet` del 003 (cada id confirmado nace con 1 serie vacía) y footer `Finalizar y Guardar Rutina` que persiste en el fake en memoria, vuelve a Home y la rutina aparece en el feed. `Crear Rutina +` deja de abrir el sheet y navega al constructor.
## 2. Alcance (entra / no entra)
Entra:
- Nueva feature `feature/routines` con `RoutineEditScreen` (stateful + contenido stateless, patrón de `HomeScreen.kt`/`ExercisePickerSheet.kt`) + `RoutineEditViewModel` fake con `StateFlow<RoutineEditUiState>` (instanciado con `viewModel()` de `lifecycle-viewmodel-compose`, ya declarado).
- Dominio en memoria `domain/model/Routine.kt` (`Routine`, `RoutineExercise`, `PlannedSet`, `SetType`, `RoutineEditUiState`); IDs UUID generados en cliente; `updatedAt/deleted` sync-ready; la UI solo ve `domain/model`.
- Repo fake nuevo `data/repository/FakeRoutineRepository.kt` con interfaz `RoutineRepository` de firma futura (`observeRoutine(id)`, `observeRoutines()`, `save`, `addExercises`, `updateSet`, `deleteExercise`…); dataset inicial vacío (crear parte de cero) o con 1 rutina de ejemplo si ayuda a la copia visual.
- Reglas de series completas (`docs/MODELO_DE_DATOS.md:81-87`): al crear series se copian valores de la 1ª; editar propaga solo a siguientes no editadas a mano (estado solo de pantalla); tipos calentamiento/normal/al fallo por serie; `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `loadNote` + `restSeconds` por serie.
- Reutilización del sheet 003 sin cambios internos: apertura solo desde `+ Añadir Nuevo Ejercicio a la Rutina` (`code.html:217`); `onConfirm(ids)` añade 1 serie vacía por ejercicio; cerrar/X descarta sin crash.
- Recableado: `HomeScreen.onCreate` (`Crear Rutina +`) navega a `routineEdit` (nueva rutina); el `TrainingNav` de `MainActivity.kt:52-89` gana la ruta sin romper las existentes (`home/calendar/progress/profile`).
- Guardado: `Finalizar y Guardar Rutina` persiste en el fake, navega a Home y la rutina aparece en el feed (`HomeViewModel` combina `observeRoutines()` del nuevo repo; atrás/salir sin guardar descarta).
- Verificación de iconos nuevos (`AddCircle` y los que se usen) contra el sources.jar de `material-icons-core`; fallbacks si falta alguno (precedente `HomeScreen.kt:67-82`, paso 6 del 003).

No entra:
- Persistencia Room (`RoutineEntity`, `RoutineExerciseEntity`, `PlannedSetEntity`, DAOs, `AppDatabase`, `Migration`, `exportSchema`, `schemas/`), repositorio real, Hilt, DataStore.
- Descarga del catálogo (Retrofit, DTOs, `CATALOG_TAG`, `CatalogPrefs`) — Fase C; ejercicios propios `source=CUSTOM` persistidos (sin DB no hay dónde guardarlos).
- Detalle de ejercicio (GIF grande + instrucciones, MVP item 2) — spec `detalle-ejercicio`.
- Sesión en vivo (`rutina`): acordeón de ejecución, cronómetro de descanso, `Finalizar y Guardar` de sesión, prefill con última sesión (`docs/MODELO_DE_DATOS.md:86`).
- Abrir el constructor para una rutina existente desde la Home (opciones de card = TODO del 002); la ruta acepta `routineId` opcional pero su consumo desde Home queda para futuro spec.
- Duplicar/eliminar rutinas, reordenar ejercicios por drag, accesorios reutilizables (`docs/MODELO_DE_DATOS.md:105).
- Cambios en Gradle (`gradlew`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` raíz y `app/`): prohibidos.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo final exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`AGENTS.md`, `docs/ARQUITECTURA.md:11-17,25-30`). En este spec Fase A solo se hace `UI + ViewModel fake + repo fake en memoria`, sin Room.
- Real hoy (rama `003-lista-ejercicios`): `feature/profile`, `feature/home` (`HomeScreen.kt:90-112` stateful+stateless, `HomeViewModel.kt:20-30` StateFlow + repo fake), `feature/exercises` (`ExercisePickerSheet`, `ExercisesViewModel`, `FakeExerciseRepository` con `CATALOG_BASE` jsDelivr pineado), `domain/model/{Profile,Home,Exercise}.kt`. Patrón a reutilizar en `feature/routines`.
- Paquete fijado `com.mytrainingplan.app` (`app/build.gradle.kts:7,13`), `minSdk 26`, `compileSdk/targetSdk 37`, Java 11, Kotlin `2.2.10`, AGP `9.4.1`, Compose BOM `2026.02.01`. Deps suficientes ya declaradas: `navigation-compose 2.7.7`, `coil-compose 2.6.0`, `material-icons-core`, `lifecycle-viewmodel-compose` (`app/build.gradle.kts:42-45`, `gradle/libs.versions.toml:9-12,28-32`).
- Referencia visual: `references/plantilla-editar-rutina/screen.png`, `code.html:142` (Guardar header), `code.html:152` (input `Nombre de la Rutina`, ej. `Pierna & Glúteos Hipertrofia`), `code.html:157` (`3 ejercicios configurados`), `code.html:217` (botón `+ Añadir Nuevo Ejercicio a la Rutina`), `code.html:254` (serie tipo calentamiento), `code.html:338` (`Añadir Serie`), `code.html:368,389` (descansos `90s`/`60s`), `code.html:422` (footer `Finalizar y Guardar Rutina`).
- Sets: reglas intactas según `docs/MODELO_DE_DATOS.md:49-87`; entidades objetivo Room ya definidas en `docs/MODELO_DE_DATOS.md:39-58` (Fase B las implementa tal cual).
- Corrección de flujo (decisión de conversación 2026-10-03): el cableado del 003 (sheet directo desde `Crear Rutina +`, con `INTERNET` ya declarado en `AndroidManifest.xml` por el crash de Coil) era provisional; este spec lo mueve al botón de añadir del constructor.
## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Pulsar `Crear Rutina +` navega al constructor vacío (título placeholder `Nombre de la Rutina`, duración por defecto, 0 ejercicios), sin abrir el sheet.
- Editar título y duración en cabecera en vivo; tags automáticos por músculos de los ejercicios añadidos (no editables).
- Botón `+ Añadir Nuevo Ejercicio a la Rutina` abre el sheet del 003 sobre el constructor; `Listo (N)` añade N ejercicios cada uno con 1 serie vacía (reps/peso/descanso a configurar); cerrar/X no añade nada y no crashea.
- Acordeón por ejercicio (expandir/colapsar, eliminar ejercicio) con matriz de series: nº, reps, peso (con signo), descanso (s), nota, tipo (calentamiento/normal/al fallo); `Añadir Serie` copia valores de la 1ª; editar celda propaga a siguientes no editadas a mano.
- Footer fijo `Finalizar y Guardar Rutina` → persiste en fake, vuelve a Home, la rutina aparece en el feed con sus tags/duración; salir atrás sin guardar descarta sin crash.
- Rotar conserva todo el estado en sesión (`StateFlow` en `ViewModel`).

No-funcionales:
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, volt `#CCFF00` para PR/añadidos, radios y espaciados de `DESIGN.md`).
- Sin jank en scroll (listas perezosas), hit targets >= 48dp, footer con padding para no tapar contenido, prueba en móvil físico sin emulador.
- Dominio y repo fake listos para Fase B: UUIDs, `updatedAt + deleted`, futuro `@Upsert` nunca `@Insert(REPLACE)`.
## 5. Criterios de aceptación verificables
- En móvil físico el constructor es copia de `screen.png`: cabecera (título, Guardar), tags auto, acordeón de ejercicios, matriz de series, botón añadir, footer.
- Pulsar `Crear Rutina +` abre el constructor vacío y NO abre el sheet.
- El botón `+ Añadir Nuevo Ejercicio` abre el sheet; `Listo (N)` añade N ejercicios con 1 serie vacía cada uno; cerrar/X no añade y no crashea.
- Editar una celda (reps/peso/descanso) propaga a las series siguientes no editadas a mano; una serie editada a mano ya no se sobrescribe.
- Cambiar el tipo de serie (calentamiento/normal/al fallo) se refleja por fila sin crash.
- `Añadir Serie` crea la serie copiando valores de la 1ª; eliminar ejercicio lo quita sin crash.
- `Finalizar y Guardar Rutina` vuelve a Home y la rutina aparece en el feed con título, duración y tags correctos.
- Salir atrás sin guardar descarta los cambios sin crash ni rutina fantasma en el feed.
- Rotar conserva título, duración, ejercicios, series y ediciones en sesión.
- Paquete `com.mytrainingplan.app`, sin `.debug`.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `domain/model/Routine.kt` nuevo: `Routine(id: String /* UUID */, name, durationMin, position, createdAt, updatedAt = 0L, deleted = false)` + `RoutineExercise(id /* UUID */, routineId, exerciseId, position, note, updatedAt, deleted)` + `enum SetType { WARMUP, NORMAL, FAILURE }` + `PlannedSet(id /* UUID */, routineExerciseId, setNumber, targetReps, weightKg: Double, restSeconds: Int, loadNote: String?, setType = NORMAL, updatedAt, deleted)` + `RoutineEditUiState(routineId, name, durationMin, tags, exercises: List<RoutineExerciseUi>, isSaving)` donde `RoutineExerciseUi` lleva `Exercise` (dominio del 003) + `sets` + `expanded` + `handEdited: Set<String>` (solo pantalla). Tags = `MUSCLE_LABELS` de los ejercicios (ver `ExercisePickerSheet.kt`). UI solo ve dominio.
- `data/repository/FakeRoutineRepository.kt` nuevo + interfaz `RoutineRepository` de firma futura (`observeRoutine(id): Flow<RoutineEdit?`, `observeRoutines(): Flow<List<Routine>>`, `createRoutine(): String`, `saveRoutine`, `addExercises(routineId, ids)`, `updateSet`, `addSet`, `deleteSet`, `deleteExercise`, `discard(routineId)`); en memoria con `MutableStateFlow`; misma firma que el futuro repositorio real.
- `feature/routines/RoutineEditViewModel.kt` nuevo (fake Fase A): `StateFlow<RoutineEditUiState>`; crea rutina al iniciar (o carga `routineId` si viene); eventos `onNameChange`, `onDurationChange`, `onAddExercises(ids)` (1 serie vacía: `targetReps = 0`, `weightKg = 0.0`, `restSeconds = 90`, `setType = NORMAL`), `onToggleExpanded`, `onDeleteExercise`, `onSetFieldChange` (con propagación), `onSetTypeChange`, `onAddSet` (copia 1ª), `onDeleteSet`, `onSave`, `onDiscard`; sin Hilt.
- `feature/routines/RoutineEditScreen.kt` nuevo: `RoutineEditScreen(viewModel, onSaved, onBack)` stateful + `RoutineEditContent` stateless; `EditHeader` (atrás + input título + Guardar), `MetaRow` (duración editable + tags auto), `ExerciseAccordion` (`ExerciseHeader` con thumbnail Coil 56dp reutilizando patrón del 003 + expandir + eliminar), `SetsMatrix` (filas editables + selector de tipo + `Añadir Serie`), `AddExerciseButton` (dashed, abre sheet), `SaveFooter` (`Finalizar y Guardar Rutina`). Sheet 003 anidado con estado hoisted en la Screen. Tokens locales como en 001/002/003; iconos `material-icons-core` (verificar `AddCircle` en el sources.jar 1.7.8 como en el paso 6 del 003; fallback listo si falta).
- `MainActivity.kt`: ruta `routineEdit?routineId={routineId}` con `navArgument(nullable, defaultValue = null)`; `onCreate` = `navigate("routineEdit")`; `onSaved` = `popBackStack + navigate(home)`; `onDismiss/back` sin guardar = `popBackStack`. Resto del `TrainingNav` intacto.
- `HomeScreen.kt`/`HomeViewModel.kt`: `HomeViewModel` combina `FakeRoutineRepository.observeRoutines()` mapeadas a `RoutineSummary` (tags = músculos, duración y nº de ejercicios reales) junto al feed fijo; las 3 rutinas de ejemplo del 002 se mantienen o se sustituyen según convenga (decidir en implementación sin romper 002).
- Room/DAO/Migration/mapper/DTOs/`ExerciseApi`/catálogo/`CATALOG_TAG`/Hilt: NO se tocan.
- Qué NO se toca (gotchas `AGENTS.md`): no cambiar `applicationId`, no `applicationIdSuffix ".debug"`, misma keystore fuera del repo + bump `versionCode` solo en release, `gradle-wrapper.jar` ausente (abrir en Android Studio primero), sin emulador, `dynamicColor=false` para fidelidad.
## 7. Plan de tareas
1. Dominio en memoria `domain/model/Routine.kt` (`Routine` + `RoutineExercise` + `PlannedSet` + `SetType` + `RoutineEditUiState`, UUIDs + `updatedAt/deleted`).
2. Crear `data/repository/FakeRoutineRepository.kt` + interfaz `RoutineRepository` de firma futura, en memoria.
3. Crear `feature/routines/RoutineEditViewModel.kt` fake con `StateFlow<RoutineEditUiState>` (cabecera + ejercicios + series con propagación + tipos + guardar/descartar).
4. Crear `feature/routines/RoutineEditScreen.kt` (cabecera + tags auto + acordeón + matriz + botón añadir + footer) como copia Compose de `screen.png` / `code.html`.
5. Reutilizar `ExercisePickerSheet` desde `+ Añadir Nuevo Ejercicio` (estado hoisted, `onConfirm` → 1 serie vacía por id, `onDismiss` sin crash).
6. Navegación: ruta `routineEdit?routineId={routineId}` en `MainActivity`; recablear `onCreate` (ya no abre el sheet); guardar → Home con la rutina en el feed (combinar `observeRoutines()` en `HomeViewModel`).
7. Verificar iconos (`AddCircle` y los usados) contra el sources.jar de `material-icons-core`; aplicar fallbacks si falta alguno.
8. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico (constructor vacío, añadir desde sheet, propagación, tipos, guardar visible en Home, descartar, rotación).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo ni Room).
- Prueba en móvil físico (sin emulador): comparar con `screen.png`; abrir desde `Crear Rutina +` (vacío, sin sheet); añadir ejercicios desde el botón (1 serie vacía c/u); editar con propagación y tipos; `Añadir Serie`/eliminar; guardar → rutina en el feed de Home; atrás sin guardar descarta; rotar conserva estado en sesión.
## 9. Riesgos / No romper
- El recableado cambia el comportamiento aceptado del 003 (`Crear Rutina +` abría el sheet): es intencional y queda registrado aquí (§3, §6.5); el 003 no se reabre.
- `HomeViewModel` pasa a combinar dos repos fake: no romper el feed del 002 (las 3 rutinas de ejemplo deben seguir o migrarse de forma visible y documentada en el informe del paso).
- Reglas de propagación: el estado "editada a mano" es solo de pantalla, jamás se persiste (ni siquiera en el fake).
- `weightKg` con signo desde el día uno (`0.0` corporal por defecto); `restSeconds` por serie (defecto 90), nunca global del ejercicio.
- En Fase B: solo `@Upsert`, nunca `@Insert(REPLACE)`; `Migration` real + `exportSchema=true` con schemas en `app/schemas/`; update de catálogo solo filas `source=CATALOG`.
- No cambiar `applicationId/com.mytrainingplan.app` ni añadir `.debug`; no mover keystore al repo; bump `versionCode` solo en release.
- UI solo ve `domain/model`; mapping en `data/mapper` cuando exista.
- No añadir dependencias Gradle en este spec (material3 + iconos core + Coil ya están); si hiciera falta otra, proponer línea exacta y esperar (restricción `AGENTS.md`).
- Sheet anidado: padding inferior para no tapar el footer; hit targets >= 48dp.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- `rutina` (sesión en vivo) desde `references/plantilla-rutina`: acordeón de ejecución + tabla de series + cronómetro de descanso + prefill última sesión (`docs/MODELO_DE_DATOS.md:86`) + `Finalizar y Guardar` de sesión.
- `detalle-ejercicio`: tap en la fila abre detalle fake (GIF grande + instrucciones + secundarios); base MVP item 2.
- Abrir el constructor para rutina existente desde opciones de la card de Home (el 002 dejó `onOptions` como TODO; la ruta ya acepta `routineId`).
- Duplicar/eliminar rutinas (`code.html:342` sugiere Duplicar), reordenar ejercicios, accesorios reutilizables (`docs/MODELO_DE_DATOS.md:105`).
- Fase B Room rutinas: `RoutineEntity` + `RoutineExerciseEntity` + `PlannedSetEntity` (misma forma) + DAOs `@Upsert/observe/softDelete` + `AppDatabase Migration` + repositorio real.
- Tema: migrar tokens locales a `ui/theme/` y tipografías Outfit/Plus Jakarta Sans/Space Grotesk (pendiente desde 001/002/003).
## Preguntas abiertas
- ¿El constructor vacío debe traer 1 ejercicio de ejemplo o empezar con 0 ejercicios y solo el botón de añadir? Propuesta: 0 ejercicios.
- ¿Duración por defecto de rutina nueva (p. ej. 45 min) o vacía hasta guardar? Propuesta: 45 min editable.
- ¿Las 3 rutinas de ejemplo del feed del 002 se mantienen junto a las creadas o se retiran al guardar la primera? Propuesta: se mantienen (el feed combina fijas + creadas).
- ¿Descanso por defecto de la serie vacía (90 s) o configurable en ajustes? Propuesta: 90 s fijo en Fase A.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Constructor copia `screen.png` verificada en móvil físico (cabecera, tags, acordeón, matriz, botón, footer)
- [x] `Crear Rutina +` abre el constructor vacío sin abrir el sheet
- [x] `+ Añadir Nuevo Ejercicio` abre el sheet; `Listo (N)` añade 1 serie vacía por ejercicio
- [x] Cerrar/X del sheet no añade nada y no crashea
- [x] Editar celda propaga a siguientes no editadas; la editada a mano no se sobrescribe
- [x] Tipo de serie (calentamiento/normal/al fallo) cambia por fila sin crash
- [x] `Añadir Serie` copia valores de la 1ª; eliminar ejercicio no crashea
- [x] `Finalizar y Guardar Rutina` vuelve a Home con la rutina en el feed
- [x] Salir atrás sin guardar descarta sin rutina fantasma y sin crash
- [x] Rotación conserva título, duración, ejercicios, series y ediciones en sesión
- [x] Paquete `com.mytrainingplan.app`, sin `.debug`
- [x] `./gradlew assembleDebug` OK
- [x] Prueba en móvil físico OK
