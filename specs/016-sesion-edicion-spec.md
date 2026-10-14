# Edición de rutina desde la sesión en vivo
Estado: implementado
Depende de: specs/009-sesion-persistida-spec.md (la sesión viva y su cierre), specs/004-editar-rutina-spec.md (el constructor que se abre desde el menú) y specs/015-calendario-progreso-spec.md (los apuntes que se auto-sustituyen)
Fecha de creación: 2026-10-13
Descripción: El ⋮ de la sesión deja de ser un botón muerto y permite editar la rutina o salir. Cada ejercicio gana un botón para añadir una serie calcada de la última y el swipe a la izquierda borra pendientes con confirmación. Añadir y borrar editan también la rutina guardada. Si se termina haciendo otra rutina, el apunte del día se actualiza solo.
## 1. Objetivo
Editar la rutina sin salir del entreno y que lo cambiado en vivo quede en la rutina. El ⋮ hace algo útil. Lo hecho manda sobre lo apuntado.
## 2. Alcance (entra / no entra)
Entra:
- Menú ⋮ con "Editar rutina" (abre el constructor) y "Salir del entrenamiento" (vuelve atrás sin finalizar; la sesión sigue viva).
- Botón "+ Añadir serie" bajo la última serie de cada ejercicio; copia reps, kg y pausa de la última serie viva.
- Añadir y borrar en vivo editan la rutina guardada (al abrirla otro día trae lo último hecho).
- Swipe a la izquierda en serie pendiente revela "¿Borrar?": tap borra, otro gesto cancela.
- Al finalizar con una rutina distinta a la apuntada, el apunte del día pasa a ser la realizada.
No entra:
- Picker a alto completo (va en el spec 017, separado).
- Re-escribir la sesión viva con lo editado en el constructor (aplica desde el próximo día).
- Gráficas, avisos ni rutinas automáticas.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/workout` (menú en `WorkoutScreen.kt:396-407`, hoy con `onClick` vacío; botón añadir y swipe en las filas de serie; `WorkoutViewModel.kt` solo llama al repo) y `feature/routines` (reutiliza el constructor, sin cambios). Flujo sin cambios: pantalla → caja observable del ViewModel → repositorio → base local. Lógica nueva en `WorkoutRepository` con métodos nuevos y espejo falso (opción B decidida: la doble escritura vive en el repo, no en el ViewModel). Lo "hecho" sigue deduciéndose del cierre (`endedAt`). Catálogo y DTOs no se tocan.
## 4. Requisitos funcionales + no-funcionales
- El ⋮ abre menú; "Editar rutina" navega al constructor con esa rutina; "Salir" vuelve atrás sin finalizar ni descartar.
- "+ Añadir serie" crea la serie viva (nº siguiente, sin marcar) y la serie planificada en la rutina con reps, kg y pausa calcados de la última viva; luego se puede modificar con el modal existente.
- "¿Borrar?" solo en pendientes; borra la serie viva y la planificada (borrado lógico en ambas).
- Descartar la sesión conserva la edición de la rutina; el diálogo lo avisa en una línea.
- Auto-reemplazo: al finalizar, si la rutina hecha difiere del apunte del día, el apunte pasa a ser la hecha (misma fila, sin duplicar).
- Botes de `ui/theme`, sin iconos nuevos (texto "¿Borrar?"), reglas edge-to-edge.
## 5. Criterios de aceptación verificables
- El ⋮ muestra "Editar rutina" y "Salir del entrenamiento".
- "Editar rutina" abre el constructor y al volver la sesión sigue con su tiempo y series.
- "+ Añadir serie" crea una serie con los valores de la última.
- Al abrir la rutina otro día trae las series añadidas en vivo.
- Swipe en pendiente muestra "¿Borrar?"; tap borra; soltar antes cancela.
- Swipe en serie hecha no ofrece borrar.
- Descartar tras editar conserva las series en la rutina.
- Finalizar otra rutina sustituye el apunte del día sin duplicar.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `WorkoutScreen`: `DropdownMenu` en el ⋮ (patrón del ⋯ de Home); botón añadir bajo cada ejercicio; `SwipeToDismissBox` de Material3 solo dirección fin-a-inicio (docs oficiales vía Context7: sin `confirmValueChange`, patrón `onDismiss` + `reset()`; verificar contra el BOM declarado al implementar).
- `WorkoutViewModel`: `onAddLiveSet`, `onDeleteLiveSet`, navegación del menú; sin lógica de datos.
- `WorkoutRepository`: `addLiveSet(sessionId, routineExerciseId)` y `deleteLiveSet(sessionId, entryId)` nuevos que escriben serie viva + serie planificada juntas (transacción Room en el real; espejo en el falso). Reutilizan `SetEntry`/`PlannedSet` y `@Upsert`; borrado lógico.
- Reutiliza `updateSet/deleteSet` existentes del repo de rutinas donde encaje; sin cambio de esquema (DB sigue v3, sin `Migration`).
- Auto-reemplazo en `RoomScheduleRepository.setScheduled` llamado al finalizar (detalle al implementar).
- No se toca: catálogo, DTOs, `applicationId`, dependencias Gradle.
## 7. Plan de tareas
1. Métodos `addLiveSet/deleteLiveSet` en `WorkoutRepository` + espejo falso (`data/repository/`).
2. Doble escritura con transacción en `RoomWorkoutRepository.kt` (serie viva + planificada, borrado lógico).
3. Menú ⋮ en `WorkoutScreen.kt:396-407` (Editar rutina + Salir) + navegación en `MainActivity.kt`.
4. Botón "+ Añadir serie" por ejercicio + `onAddLiveSet` en `WorkoutViewModel.kt`.
5. Swipe "¿Borrar?" solo pendientes + `onDeleteLiveSet` (`feature/workout/WorkoutScreen.kt`).
6. Auto-reemplazo del apunte al finalizar (`data/repository/RoomScheduleRepository.kt`).
7. Aviso en diálogo Descartar (`feature/workout/WorkoutScreen.kt`).
8. Tests JVM (clonado de última, simetría borrar, prefill con 5 al día siguiente, auto-reemplazo) en su espejo + smoke del menú y del swipe en `feature/workout`.
9. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración).
- `./gradlew testDebugUnitTest` verde (tests nuevos; `CatalogMappingTest` como regresión, no se toca mapper).
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico (menú, añadir, swipe, auto-reemplazo).
- En ambos: editar desde sesión y volver, series persistidas al día siguiente, `statusBarsPadding` sin dp extra.
## 9. Riesgos / No romper
- No cambiar `applicationId`, keystore/`versionCode` ni dependencias Gradle.
- La doble escritura debe ser atómica: jamás serie viva sin planificada ni al revés.
- Lo editado en el constructor no re-escribe la sesión viva (copia al iniciar); documentarlo en la UI si confunde.
- El ⋮ no finaliza ni descarta por accidente (sin acción destructiva en el menú).
- Fechas del auto-reemplazo en día local (`yyyy-MM-dd`), como el spec 015.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Picker a alto completo al crear rutina → `specs/017-...-spec.md` (pendiente de planificar; detectado en `ExercisePickerSheet.kt:159` con `fillMaxHeight(0.88f)`).
- Aplicar ediciones del constructor a la sesión viva en curso (hoy aplica desde el próximo día).
- Más opciones en el ⋮ (ver ficha de rutina, temporizador, etc.).
## Preguntas abiertas
- Decidido (opción B): métodos nuevos en `WorkoutRepository` con la doble escritura dentro (recomendación Context7, ID `/websites/developer_android_topic_libraries_architecture`).
- Supuesto: "Salir del entrenamiento" = volver atrás sin finalizar (la sesión sigue y se retoma); "Descartar" sigue siendo el borrado. Confirmar que no duplica el minimizar existente.
- Supuesto: el modal de serie sirve para retocar la añadida sin cambios.
- ¿El "¿Borrar?" necesita segundo tap de confirmación o basta con el tap único?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] El ⋮ muestra "Editar rutina" y "Salir del entrenamiento".
- [x] "Editar rutina" abre el constructor y al volver la sesión sigue con su tiempo y series.
- [x] "+ Añadir serie" crea una serie con los valores de la última.
- [x] Al abrir la rutina otro día trae las series añadidas en vivo.
- [x] Swipe en pendiente muestra "¿Borrar?"; tap borra; soltar antes cancela.
- [x] Swipe en serie hecha no ofrece borrar.
- [x] Descartar tras editar conserva las series en la rutina.
- [x] Finalizar otra rutina sustituye el apunte del día sin duplicar.
- [x] `./gradlew assembleDebug` termina OK.
- [x] `./gradlew testDebugUnitTest` termina en verde.
- [x] `./gradlew connectedDebugAndroidTest` en verde en emulador y en físico.
