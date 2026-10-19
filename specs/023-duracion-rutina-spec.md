# Duración de rutina: vacía, estimada y real
Estado: implementado
Depende de: specs/020-ceros-numericos-spec.md + specs/022-sesion-en-marcha-spec.md (020 aporta el campo numérico; 022 el cronómetro y el tiempo real de la sesión)
Fecha de creación: 2026-10-18
Descripción: Al crear una rutina su duración sale vacía y se estima sola (descansos + 30 s por serie). Si la editas a mano se congela; al terminar una sesión pasa al tiempo real medido. Ese valor queda guardado en la rutina para usarlo más adelante en estadísticas.
## 1. Objetivo
Que la duración de la rutina no sea un número fijo inventado (hoy 45 min), sino algo que la app calcula y aprende: aprox mientras la diseñas, y real cuando ya la has entrenado. Ver `docs/02-Ideas-preguntas.md:19-25`. Se apoya en el tiempo real que ya mide la sesión en vivo (022). Nada de esto toca el catálogo ni las pantallas de Progreso/Calendario.
## 2. Alcance (entra / no entra)
Entra:
- Rutina nueva sin duración (campo vacío, sin heredar 45).
- Modo AUTO: estimación = suma de descansos + 30 s por serie, redondeo al minuto más cercano (mínimo 1).
- Modo MANUAL: el usuario escribe el número y se congela.
- Al guardar una rutina AUTO, la estimación se guarda en la rutina.
- Al finalizar una sesión de una rutina AUTO, la rutina adopta el tiempo real medido.
- Columna nueva `durationManual` en `routines` + migración v4→v5.
- Tests JVM y smoke instrumentado.
No entra:
- Estadísticas/medias de duración real (eso es Progreso, 015): spec futuro.
- Cambios en Calendario/Progreso.
- Catálogo, CATALOG_TAG, DTOs.
- Rediseño visual de la ficha de rutina más allá de la fila de duración.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Feature tocada: `feature/routines` (`RoutineEditScreen.kt:238-247` invoca MetaCard; `:362-460` fila de duración con el texto `:425` y el campo `:439`; `:1206-1237` pie con `~$durationMin min`). ViewModel `RoutineEditViewModel.kt:58` y `:88-98`.
- Capa de datos: `data/repository/RoutineRepository` (interfaz en `FakeRoutineRepository.kt:50-52`, implementaciones fake `:119-141` y Room `RoomRoutineRepository.kt:84-100`) y `RoomWorkoutRepository.finishSession` `:317-329` (ya recibe `RoutineDao`, `:39`).
- Room: `routines` con `durationMin` (`RoutineEntity.kt:16`), mapeo en `RoutineMapper.kt:18-28`, `AppDatabase.kt` en v4. `@Upsert` y borrado lógico ya vigentes.
- Dominio: `Routine.durationMin` (`Routine.kt:27`), `RoutineEditUiState.durationMin=45` (`:83`). El tiempo real existe como `WorkoutSession.timerElapsedSec` (`Workout.kt`).
- Flujo intacto: `Screen → ViewModel (StateFlow) → Repository → Room`. Retrofit no interviene.
- Sin catálogo/DTO/red. Sin Gradle, sin keystore, sin iconos, sin colores nuevos.
## 4. Requisitos funcionales + no-funcionales
- Todo el cálculo de duración es lógica de dominio pura y testeable, sin Android.
- La estimación no se guarda por cada tecla: se calcula en pantalla y se persiste al guardar.
- `durationMin = 0` significa "sin valor" en modo AUTO; nunca se muestra "0 min".
- El modo (auto/manual) y la duración viajan juntos a la base de datos.
- Migración sin pérdida: lo ya guardado se conserva.
- Botones/CTAs con `testTag` para el smoke (`routineEditRoot`, `routineEditSave` ya existen).
## 5. Criterios de aceptación verificables
1. Rutina nueva: la fila de duración sale vacía ("—"), no "45".
2. AUTO con series: muestra "≈ X min" con X = redondeo((Σ descansos + 30·nº series)/60), mínimo 1.
3. AUTO sin series: muestra "—".
4. MANUAL: el número se puede editar y la estimación no lo pisa.
5. Pasar de MANUAL a AUTO recalcula la estimación.
6. Guardar rutina AUTO persiste la estimación en `routines.durationMin`.
7. Finalizar una sesión de rutina AUTO deja `durationMin` = redondeo(tiempo real/60), mínimo 1.
8. Finalizar una sesión de rutina MANUAL no cambia su duración.
9. Home muestra "X min" para rutinas con duración y oculta la línea cuando es 0.
10. Tras migrar de v4 a v5, las rutinas existentes quedan MANUAL conservando su duración.
11. No se alteran Calendario/Progreso ni el catálogo.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Dominio nuevo `domain/model/RoutineDurationRules.kt`: `estimate(sets)` → null si no hay series; si no, redondeo((Σ `restSeconds` + 30·n)/60) con mínimo 1; `fromRealSeconds(sec)` → mínimo 1 si hubo sesión. Puro, sin dependencias.
- `Routine` + `durationManual: Boolean = false`; `RoutineEditUiState` + `durationManual` (default false) y el valor efectivo mostrado (si auto, la estimación; si manual, el número).
- Room: `RoutineEntity` + `durationManual`; `RoutineMapper` ida y vuelta; `AppDatabase` v5 + `MIGRATION_4_5` que añade la columna con DEFAULT 1 (las filas existentes pasan a MANUAL y conservan su minuto).
- Repositorio: `createRoutine(name="", durationMin=0, durationManual=false)`; `updateMeta(routineId, name, durationMin, durationManual)`; `saveRoutine` recalcula y persiste la estimación si la rutina es AUTO; `finishSession` (Room y fake) actualiza la rutina con el tiempo real solo si es AUTO, dentro de la misma transacción.
- UI `RoutineEditScreen`: en la fila de duración un interruptor de dos estados AUTO/MANUAL; AUTO → campo no editable "≈ X min" ("—" si no hay series); MANUAL → campo numérico editable (reusa el campo de 020) y congelado. El pie (`SaveFooter`) usa el valor efectivo, "≈" solo en AUTO.
- Home (`HomeScreen.kt:482`): usa `durationMin` tal cual; si es 0, no pinta la línea. `RoomHomeRepository.kt:59` sin cambios.
- Sets: se usa `restSeconds` de cada serie planificada; no se toca `weightKg`/`loadNote`/propagación.
- Gotchas a respetar: no cambiar `applicationId`, ni keystore/`versionCode`, ni dependencias; UI solo ve `domain/model`; mapeo en `data/mapper`.
## 7. Plan de tareas
1. `domain/model/RoutineDurationRules.kt`: crear la regla pura de estimación y de tiempo real.
2. `domain/model/Routine.kt`: añadir `durationManual` a `Routine` y a `RoutineEditUiState`; ajustar el default de duración a 0.
3. `data/local/entity/RoutineEntity.kt` + `data/mapper/RoutineMapper.kt`: nueva columna y su mapeo.
4. `data/local/AppDatabase.kt`: subir a v5 + `MIGRATION_4_5`.
5. `data/repository/FakeRoutineRepository.kt`: firma nueva de `createRoutine`/`updateMeta` + `durationManual`.
6. `data/repository/RoomRoutineRepository.kt`: mismas firmas + persistir estimación en `saveRoutine` cuando es AUTO.
7. `data/repository/RoomWorkoutRepository.kt`: en `finishSession`, aplicar tiempo real si la rutina es AUTO (misma transacción) y reflejarlo también en el fake de workout.
8. `feature/routines/RoutineEditViewModel.kt`: `onDurationChange` (pasa a MANUAL), `onDurationModeChange` (volver a AUTO recalcula), exponer modo y valor efectivo.
9. `feature/routines/RoutineEditScreen.kt`: interruptor AUTO/MANUAL y campo de duración según modo; `SaveFooter` con el valor efectivo.
10. `feature/home/HomeScreen.kt`: ocultar la línea de duración cuando es 0.
11. Tests JVM: `RoutineDurationRulesTest`, `RoutineDurationFlowTest` (fake: auto guarda estimación, manual congela) y `FinishSessionDurationTest` (auto→real, manual intacta).
12. Tests instrumentados: `Migration45Test` + smoke `RoutineEdit023DurationTest` (vacío → ≈ al añadir series → MANUAL → AUTO → guardar).
13. Verificación: `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK; `./gradlew testDebugUnitTest` verde (incluye `CatalogMappingTest`, que no debe romperse aunque no se toquen sus ficheros).
- Emulador Pixel 6 API 34 + móvil físico (automático; físico puede quedar bloqueado por MIUI y se anota): crear rutina y ver vacío/≈, cambiar a MANUAL, volver a AUTO, guardar, finalizar sesión y comprobar que la duración pasa a la real, y que Home la muestra.
- `connectedDebugAndroidTest` como red de seguridad (migración + smoke).
## 9. Riesgos / No romper
- Cambio de firma de `updateMeta` (3 usos): actualizar los tres de una vez (ViewModel y repos).
- `createRoutine` pasa de 45 a 0: solo lo llama `openRoutine`; los `Routine(durationMin=45)` de previews/tests siguen compilando (el flag tiene default).
- Si se sube a v5 sin la migración real, se pierden datos: van juntas.
- Paridad Room/fake en `finishSession` y `saveRoutine` (los tests JVM usan el fake).
- No mostrar "0 min" en Home (ocultar línea).
- No tocar `durationMin` ya guardado de rutinas MANUAL.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Usar la duración real para medias/gráficas en Progreso (detectado en `ProgressScreen.kt:161`, ahora muestra `progress.minutes`).
- Guardar la duración real por día/sesión (histórico) en vez de solo "la última".
- Enseñar "≈" también en las tarjetas de Home cuando la rutina nunca se ha entrenado.
- Sugerir duración a partir de la media histórica cuando la rutina es nueva.
## Preguntas abiertas
- Ninguna bloqueante. Redondeo fijado al más cercano, mínimo 1 cuando hay series o hubo sesión.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] 1. Rutina nueva sin duración ("—"). — MCP emulador: fila "—" + resumen "0 ejercicios configurados • —"; smoke `rutina_nueva_duracion_vacia_guion` OK.
- [x] 2. AUTO con series: "≈ X min" = redondeo((Σ descansos + 30·n)/60), mínimo 1. — MCP: 1 serie DESC 90 → "≈ 2 min" (fila+resumen+pie); smoke `auto_con_series_muestra_estimacion` OK; `RoutineDurationRulesTest` 8/8.
- [x] 3. AUTO sin series: "—". — MCP: fila y pie "—" (fix `SaveFooter` que pintaba "≈ 0 min"); smoke OK.
- [x] 4. MANUAL: editable y congelado. — MCP: campo editable (tecleado 0→5), pie "5 min" sin "≈"; smoke `manual_muestra_numero_escrito` OK; JVM `manual_guardar_congela_duracion`.
- [x] 5. MANUAL → AUTO recalcula. — MCP: 5 min → "≈ 2 min" al volver a AUTO.
- [x] 6. Guardar AUTO persiste la estimación. — MCP: guardada "Rutina 3" en Home con "2 min"; JVM `auto_guardar_persiste_estimacion_de_series`.
- [x] 7. Finalizar sesión AUTO → duración = tiempo real (mín. 1). — MCP: sesión de 115 s → Home "2 min" (redondeo, no truncado); JVM `finalizar_auto_adopta_tiempo_real_medido` + `finalizar_sin_cronometro_auto_minimo_1`.
- [x] 8. Finalizar sesión MANUAL → duración intacta. — MCP: "Prueba Tint" (MANUAL) tras finalizar sesión de ~184 min sigue "45 min"; JVM `finalizar_manual_no_cambia_duracion`.
- [x] 9. Home muestra "X min" u oculta la línea si 0. — MCP: "45 min"/"2 min" visibles; ocultar si 0 por código `HomeScreen.kt:480-483`.
- [x] 10. Migración v4→v5 conserva datos (existentes = MANUAL). — `Migration45Test` OK (instrumentado); migración EN VIVO en emulador (APK v4→v5: filas conservan "45 min"); schema `5.json` con `durationManual`.
- [x] 11. Calendario/Progreso y catálogo intactos. — git diff sin ficheros de history/catálogo; `CatalogMappingTest` 5/5; picker "1323 DISPONIBLES" y Progreso/Calendario renderizan en emulador.
- [x] 12. `./gradlew assembleDebug` OK. — BUILD SUCCESSFUL.
- [x] 13. `./gradlew testDebugUnitTest` verde. — 110/110 (023: 8+3+3; `CatalogMappingTest` 5/5).
- [x] 14. Smoke en emulador Pixel 6 API 34 (+ físico si se puede). — MCP (vía primaria) + `Migration45Test`/`RoutineEdit023DurationTest` 4/4 instrumentados en `emulator-5554`; físico no disponible (no hay dispositivo en `adb devices`) → anotado.
