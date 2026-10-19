# Propagación de series + duración legible en crear/editar rutina
Estado: aprobado
Depende de: specs/023-duracion-rutina-spec.md (la fila de duración AUTO/MANUAL ya existe; este spec la retoque)
Fecha de creación: 2026-10-19
Descripción: Dos mejoras en crear/editar rutina: (1) la propagación de pesos/repeticiones/segundos solo parte de la serie 1 y al pulsar "Añadir Serie" copia de la última; editar series intermedias no toca las demás. (2) la fila de duración deja de recortar el texto del botón AUTO/MANUAL (pasa a icono solo) y se redistribuye para que botón, valor y "min" sean legibles.

## 1. Objetivo
Evitar que editar una serie intermedia sobrescriba las filas de abajo, y hacer legible la fila de duración sin texto recortado dentro de botones.

## 2. Alcance (entra / no entra)
Entra:
- RoutineEditViewModel.onSetFieldChange: propagar a siguientes series SOLO cuando se edita la serie 1 (y solo a las no editadas a mano, manteniendo handEdited); editar serie N>1 modifica solo esa celda.
- addSet (fake + Room): copiar valores de la ÚLTIMA serie, no de la primera.
- RoutineEditScreen.MetaCard: botón modo AUTO/MANUAL pasa a icono solo (sin texto interno); redistribuir fila [Duración][icono modo][valor][min] para que quepa sin recorte.
- Tests de rutina (JVM + instrumentados) que cubran propagación y duración: revisar y actualizar si afectan.
No entra:
- Home/feed, calendario/progreso.
- Migración Room ni cambio de esquema de DB.
- Catálogo / CUSTOM / Retrofit / DTOs.
- Detalle de ejercicio / GIF.
- Gradle ni dependencias.

## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Feature tocada: feature/routines (RoutineEditScreen.kt, RoutineEditViewModel.kt).
- Datos: data/repository/RoutineRepository.kt (interfaz, addSet), FakeRoutineRepository.kt (addSet, :214-226 copia de minByOrNull = primera), RoomRoutineRepository.kt (addSet, mismo patrón).
- Dominio: RoutineEditUiState, PlannedSet; durationManual/durationMin vienen del 023, no se tocan.
- Flujo: Screen → ViewModel (StateFlow) → Repository → (fake o Room). Sin catálogo, sin red, sin Retrofit.
- Gotchas: handEdited es solo pantalla, nunca se persiste; @Upsert nunca REPLACE; no cambiar applicationId.

## 4. Requisitos funcionales + no-funcionales
- Editar reps/peso/descanso de la serie 1 → propaga a series 2..N aún no editadas a mano.
- Editar una celda de serie N>1 → solo esa celda; filas inferiores intactas.
- "Añadir Serie" → copia los valores de la última serie de ese ejercicio a la nueva.
- Una celda editada a mano ya no se sobrescribe al editar otra serie.
- Botón modo sin texto interno (icono solo), nunca "MANU" recortado.
- Fila de duración distribuida: botón, valor y "min" legibles sin recorte.
- handEdited no se persiste (ni fake ni Room).
- Sin migración de esquema.

## 5. Criterios de aceptación verificables
- Editar reps/peso/descanso de la serie 1 propaga a series 2..N no editadas a mano.
- Editar celda de serie N>1 modifica solo esa celda; las de abajo no cambian.
- Pulsar "Añadir Serie" copia los valores de la última serie a la nueva.
- Celda editada a mano no se sobrescribe al editar otra serie (handEdited).
- Botón modo AUTO/MANUAL muestra solo icono (no texto truncado).
- Fila de duración legible: botón, valor y "min" sin recorte.
- handEdited no se persiste.
- ./gradlew assembleDebug OK.
- Tests de rutina (propagación + duración) verdes.
- Prueba en móvil/emulador: propagación serie 1, no-propagación serie 2+, añadir-copiar-última, botón modo sin recorte, fila duración legible.

## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- RoutineEditViewModel.onSetFieldChange: si edited.setNumber == 1, propagar a siguientes con handKey !in editedKeys (como hoy); si setNumber > 1, solo actualizar la fila editada (no propaga).
- Repository.addSet (fake y Room): newSet = last.copy(id = UUID, setNumber = next) donde last = maxByOrNull { setNumber }, en vez de first = minByOrNull.
- RoutineEditScreen.MetaCard: el toggle modo es un Box icon-only (.clickable, sin Text dentro); el icono varía según durationManual; el campo de valor y el "min" recuperan el ancho sobrante; durationDisplay sigue viniendo del 023.
- Depende del 023 (fila de duración ya existe con durationManual).

## 7. Plan de tareas
1. RoutineEditViewModel.kt: onSetFieldChange propaga solo desde serie 1.
2. FakeRoutineRepository.kt: addSet copia de la última serie.
3. RoomRoutineRepository.kt: addSet copia de la última serie.
4. RoutineEditScreen.kt: MetaCard botón modo icono solo + redistribución de fila duración.
5. Tests: revisar/actualizar tests de rutina afectados (propagación y duración).
6. Verificación: ./gradlew assembleDebug OK + tests de rutina verdes.

## 8. Verificación (assembleDebug + tests + móvil físico)
- ./gradlew assembleDebug OK.
- ./gradlew testDebugUnitTest verde (tests de rutina; CatalogMappingTest no rompe, no toca mapper/DTO de catálogo).
- Emulador Pixel 6 API 34 + móvil físico si hay: editar serie 1 propaga, editar serie 2 no, añadir serie copia última, botón modo sin recorte, fila duración legible.

## 9. Riesgos / No romper
- Cambiar addSet de "primera" a "última": al crear un ejercicio con N series seguidas (todas vacías) el resultado es idéntico; solo difiere si se editan valores antes de añadir. Documentar en informe.
- onDeleteSet ya limpia claves handEdited de la serie borrada; mantener.
- No tocar durationManual/durationMin del 023.

## 10. Futuros specs (no entran aquí)
- Botón "Añadir Serie" por fila (per-row) en vez de uno solo al final.
- Ícono exacto del modo contra sources.jar de material-icons-core (solo 49 iconos); si falta el ideal, proponer material-icons-extended y esperar (línea exacta de Gradle).

## Preguntas abiertas
- Ninguna bloqueante tras el interrogatorio (respuestas del usuario incorporadas).

## 11. Checklist verificación (últimas, checkboxes listos para /verifier)
- [ ] 1. Editar serie 1 propaga a siguientes no editadas a mano.
- [ ] 2. Editar serie N>1 no toca las de abajo.
- [ ] 3. Añadir Serie copia de la última serie.
- [ ] 4. handEdited evita sobrescribir celdas editadas.
- [ ] 5. Botón modo icono solo, sin texto recortado.
- [ ] 6. Fila duración legible (botón + valor + min).
- [ ] 7. handEdited no se persiste.
- [ ] 8. assembleDebug OK.
- [ ] 9. Tests de rutina verdes.
- [ ] 10. Prueba en móvil/emulador.
