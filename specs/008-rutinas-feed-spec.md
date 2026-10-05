# Feed único + descarte seguro + orden real
Estado: aprobado
Depende de: specs/007-catalogo-fork-spec.md (asume catálogo y repos Room ya en `main`)
Fecha de creación: 2026-10-05
Descripción: Cada rutina guardada aparece dos veces en home, salir sin guardar puede borrar la guardada y el orden no es real. Este spec deja un solo feed, un descarte que no pierde datos y posición de verdad. Sin cambios visuales salvo que todo sale una vez y en orden.
## 1. Objetivo
Un feed con cada rutina una sola vez y en orden, donde cancelar nunca destruya lo guardado.
## 2. Alcance (entra / no entra)
Entra:
- Feed desde una sola fuente (`RoomHomeRepository`).
- `discard()` solo borra el borrador vacío; la guardada con contenido queda intacta.
- `position` real (orden de creación; se recalcula sin huecos al borrar).
No entra:
- Reordenar rutinas a mano (futuro).
- Sesión en vivo (spec 009).
- Cambios de esquema Room.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/home` (`HomeViewModel`, solo expone) y `data/repository` (`RoomHomeRepository` ya trae todas las rutinas; `RoomRoutineRepository` para descarte y posición). La duplicación nace en `feature/home/HomeViewModel.kt:36-49` (suma dos fuentes del mismo depósito local). El descarte peligroso en `data/repository/RoomRoutineRepository.kt:181-186` (marca `deleted` siempre). La posición nace a 0 en `createRoutine` y se reinicia en cada `addExercises`.
## 4. Requisitos funcionales + no-funcionales
- Cada rutina guardada sale exactamente una vez en el feed.
- Salir del constructor sin guardar no toca la guardada anterior.
- El orden del feed es estable y sin huecos tras borrar.
- Borrado siempre lógico (`updatedAt` + `deleted`); `@Upsert`, nunca borrado-y-recreado.
## 5. Criterios de aceptación verificables
- Crear y guardar una rutina → una sola card en home.
- Editar y salir sin guardar → la anterior intacta.
- Borrar una rutina → el resto conserva orden sin huecos.
- `./gradlew assembleDebug` termina OK.
- En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/home/HomeViewModel`: expone `observeHome()` tal cual (fuera el `combine` con detalles).
- `data/repository/RoomRoutineRepository`: `discard` distingue borrador vacío (borra) de guardada (no toca); `position` = nº de rutinas vivas al crear y renumerado al borrar.
- Sets: no se toca nada.
## 7. Plan de tareas
1. Feed único en `feature/home/HomeViewModel.kt`.
2. Descarte seguro en `data/repository/RoomRoutineRepository.kt`.
3. `position` real al crear/borrar en `data/repository/RoomRoutineRepository.kt`.
4. Verificación = `./gradlew assembleDebug` OK + prueba en móvil físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK.
- En móvil: una card por rutina, orden estable, cancelar no destruye.
- Edge-to-edge sin cambios.
## 9. Riesgos / No romper
- No cambiar `applicationId`; misma keystore + subir `versionCode`.
- Sin migración (sin cambio de esquema).
- La UI solo ve `domain/model`.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Reordenar rutinas a mano.
- Historial y gráfica de progreso (tras 009).
## Preguntas abiertas
- Ninguna.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Crear y guardar una rutina → una sola card en home.
- [ ] Editar y salir sin guardar → la anterior intacta.
- [ ] Borrar una rutina → el resto conserva orden sin huecos.
- [ ] `./gradlew assembleDebug` termina OK.
- [ ] En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
