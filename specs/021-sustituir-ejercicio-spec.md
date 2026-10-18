# Sustituir ejercicio la sesión en vivo
Estado: aprobado
Depende de: specs/016-sesion-edicion-spec.md (patrón de swipe en vivo) y specs/019-sesion-colapsado-spec.md (colapso por defecto, gesto diseñado sobre tarjetas colapsadas)
Fecha de creación: 2026-10-18
Descripción: Deslizar a la izquierda una tarjeta de ejercicio COLAPSADA revela un botón "Sustituir"; elegir un ejercicio del picker lo sustituye y queda guardado en la rutina de forma inmediata y permanente, como la edición de series. La sesión en vivo sigue sin perder series.
## 1. Objetivo
Poder cambiar un ejercicio de la rutina desde la sesión en vivo con un gesto, sin entrar a editar la rutina, y que el cambio persista en la rutina y siga la sesión activa.
## 2. Alcance (entra / no entra)
Entra:
- Gesto swipe izquierda SOLO en tarjetas colapsadas, con botón revelado "Sustituir" (se pulsa aparte, no al soltar).
- Picker reutilizado tal cual (`ExercisePickerSheet`); se toma el PRIMER seleccionado.
- Persistencia inmediata en la rutina (slot `routine_exercise` cambia de ejercicio; series planificadas se conservan).
- Sesión en vivo: reescribir el `exerciseId` de TODAS sus entradas del old al nuevo (hechas incluidas).
- Duplicados permitidos (elegir un ejercicio ya presente = como el constructor).
- Al confirmar: tarjeta queda colapsada, swipe vuelve a reposo.
No entra:
- Picker de selección única (hoy lista+confirm; se apunta a futuros).
- Re-prefill de la última sesión real del nuevo ejercicio (solo ocurre al iniciar sesión).
- Gesto en tarjetas expandidas ni en cabeceras de ejercicio expandidas.
- Editar series planificadas al sustituir, mover/ordenar ejercicios, borrar ejercicios desde aquí.
- Enrutado, catálogo, Gradle, migraciones de Room.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
`feature/workout`: cabecera colapsada en `ExerciseAccordion` rama else (`WorkoutScreen.kt:634+`); patrón de gesto a copiar `SwipeableSetRow` (`WorkoutScreen.kt:830-931`, `AnchoredDraggable` anclas Settled/Revealed, fondo con alfa 0 en reposo, tap en revelado = cancelar). Toda card de ejercicio muestra el nombre del ejercicio via item.exercise; la sesión viva agrupa entradas por `exerciseId` (`RoomWorkoutRepository.kt:125,134`) y al iniciar copia lo planificado (`:82-101`). Cambio de exercise_id en `RoutineExerciseEntity` + reescritura de `SetEntryEntity.exerciseId` en la MISMA transacción (`db.withTransaction`, patrón doble escritura 016) o la sesión pierde el ejercicio. Sin migraciones: solo updates sobre tablas existentes.
## 4. Requisitos funcionales + no-funcionales
- Solo tarjetas colapsadas deslizables a la izquierda (EndToStart), recorrido topado al ancho de la píldora "Sustituir" + aire.
- Botón revelado "Sustituir" (texto píldora, como "¿Borrar?"); tap lo pulsa: abre el picker.
- Confirmar en el picker ejecuta el swap con el primer id; dismiss cancela sin cambios.
- Tarjeta muestra el nuevo ejercicio (nombre referenciado por item.exercise); misma posición en la rutina; series planificadas intactas.
- Dobles escrituras atómicas; tap del nombre sigue abriendo la ficha; expand-per-tap intacto (013).
- No toca Gradle/keystore/versionCode; sin iconos nuevos; botes de `ui/theme`/`AppDimens` existentes.
## 5. Criterios de aceptación verificables
- Deslizar colapsada revela píldora "Sustituir"; tap en la fila o soltar cancela a reposo.
- Elegir ejercicio nuevo → la tarjeta (colapsada) muestra el nombre nuevo y su ficha (tap nombre) es la del nuevo.
- Sustituir uno CON series ya hechas: las entradas siguen contando y visibles bajo el nuevo ejercicio.
- Sustituir está guardado en la rutina (persiste tras salir y reabrir rutina/sesión).
- Sustituir por un ejercicio ya presente se permite (queda duplicado).
- `./gradlew assembleDebug` OK; `./gradlew testDebugUnitTest` verde; `./gradlew connectedDebugAndroidTest` verde.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `WorkoutRepository` (interface en `FakeWorkoutRepository.kt:43`): nuevo `suspend fun replaceExercise(sessionId, routineExerciseId, newExerciseId)`.
  - Room (`RoomWorkoutRepository`): en `db.withTransaction`, copia `RoutineExerciseEntity` con `exerciseId = new` (@Upsert, mismo id/position, updatedAt=nuevo) y reescribe `exerciseId` de las `SetEntryEntity` de ESA sesión del old al nuevo (leer+upsert, patrón 016). Sin nuevos DAO @Query (lotes @Upsert existentes); si resulta pesado, nueva @Query dedicada documentada.
  - Fake (`FakeWorkoutRepository`): misma regla en memoria (reescribe entradas + expone el swap para assertions).
- `WorkoutViewModel`: `onReplaceExercise(routineExerciseId, newExerciseId)` → repo (sessionId del uiState actual); sin estado extra de pantalla (el flujo de detail recompone solo).
- `WorkoutScreen`: rama colapsada envuelta en `SwipeableExerciseCard` (clona `SwipeableSetRow`): fondo detrás naranja suave con píldora "Sustituir" testTag `replaceLiveExercise:<routineExerciseId>`; swipe solo si `!item.expanded` (un wrapper que decide); tap en fila revelada = volver a reposo (igual que el reveal de 016-series); tap nombre/expansión intactos.
- Picker: mostrar al pulsar la píldora (`showReplacePicker by rememberSaveable` + id objetivo), `onConfirm = ids.firstOrNull()?.let { vm.onReplaceExercise(id, it) }`, `onExerciseClick` abre la ficha existente.
- Sin cambios en Profile/Home/RoutineEdit; el constructor no cambia (sustituir ahí = borrar+añadir ya posible).
## 7. Plan de tareas
1. Interface + Room: `replaceExercise` transaccional (swap rutina + reescritura de entradas vivas).
2. Fake + reglas JVM espejadas.
3. `WorkoutViewModel.onReplaceExercise`.
4. `SwipeableExerciseCard` para rama colapsada (clon del patrón 016) + píldora Sustituir + testTags.
5. Wiring del picker en WorkoutScreen (estado + confirm + onExerciseClick).
6. Tests JVM: reescritura total (hechas incluidas), planificados intactos, duplicado permitido, dismiss sin cambios.
7. Smoke instrumentado: swipe→sustituir→tarjeta nueva (persistencia reconsultada).
8. Verificación triple: assembleDebug + testDebugUnitTest + connectedDebugAndroidTest (emulador y físico).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK; `CatalogMappingTest` verde como regresión (no se toca mapper).
- `./gradlew testDebugUnitTest` verde con las nuevas reglas.
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y device físico; smoke de swipe+sustituir dentro del espejo de `feature/workout`.
## 9. Riesgos / No romper
- 013: el expand-per-tap y tap-nombre ficha intactos en la tarjeta colapsada deslizable.
- 016: swipe de series (dentro de expandidas) no se toca; no chocar gestos.
- Scroll vertical vs swipe horizontal: `AnchoredDraggable` de 016 ya lo resuelve; copiar tal cual.
- Reescritura de entradas SIEMPRE dentro de la transacción (o la sesión pierde la agrupación por `exerciseId`).
- No migraciones; nada de Gradle/estilo/keystore; sin tocar el picker (confirm devuelve lista).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Picker en modo selección única (param `singleSelection = true`).
- Re-prefill del ejercicio sustituido con su última sesión real.
- Sustituir desde el constructor de rutina (borrar+añadir ya lo cubre).
- Sesión en marcha → `specs/022-...-spec.md`; duración auto/manual → `specs/023-...-spec.md`.
## Preguntas abiertas
- Decidido con el usuario: solo colapsado, reescribir todo (hechas incluidas), duplicados OK, deps 016+019, JVM+smoke, queda colapsada tras confirmar.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Swipe izquierda en colapsada revela píldora "Sustituir"; tap/cancel vuelve a reposo; en expandidas no hay gesto.
- [x] Confirmar picker → tarjeta muestra nuevo ejercicio en misma posición.
- [x] Serie hecha sigue visible y contando tras sustituir.
- [x] El swap persiste en la rutina (reabrir sesión muestra el nuevo).
- [x] Sustitución por ejercicio ya presente queda permitida.
- [x] `./gradlew assembleDebug` OK.
- [x] `./gradlew testDebugUnitTest` verde.
- [x] `./gradlew connectedDebugAndroidTest` verde en emulador y en físico.
