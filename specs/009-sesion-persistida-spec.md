# Sesión persistida + error interno registrado
Estado: implementado
Depende de: specs/008-rutinas-feed-spec.md (asume feed y rutinas estables en `main`)
Fecha de creación: 2026-10-05
Descripción: Iniciar una rutina falla y lo hecho no se guarda en ningún sitio. Este spec persiste la copia de la sesión al iniciar (para el relleno con la última sesión y la futura gráfica de progreso) y registra el fallo de forma interna, sin mostrar nada al usuario.
## 1. Objetivo
Sesiones que abren sin fallar y dejan guardado lo hecho, con diagnóstico interno del fallo original.
## 2. Alcance (entra / no entra)
Entra:
- Persistir al iniciar la copia de series (relleno con la última sesión, si no lo planificado).
- Leer la sesión desde DB (IDs estables; marcar ✓ actualiza la copia).
- Cierre con `endedAt`; cancelada no deja fantasma.
- Registro interno del fallo (log, sin UI de error).
No entra:
- Historial visible ni gráfica de progreso (futuro; los datos quedan guardados para eso).
- Cambios de esquema Room.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/workout` (`WorkoutViewModel`, misma forma) y `data/repository/RoomWorkoutRepository` (hoy fabrica series con IDs nuevos en cada lectura y jamás las persiste; el prefill última-vs-plan no se usa al arrancar). Causa exacta del fallo pendiente de confirmar con el log del móvil.
## 4. Requisitos funcionales + no-funcionales
- Pulsar Iniciar abre la sesión sin fallar.
- Cada serie hecha queda guardada con su `plannedSetId`, `weightKg` con signo, `loadNote` y `restSeconds`.
- La próxima sesión del mismo día propone lo último hecho, si no lo planificado.
- El fallo original queda registrado en log interno; la UI no muestra errores.
- Borrado lógico y `@Upsert` siempre.
## 5. Criterios de aceptación verificables
- Iniciar una rutina abre la sesión sin fallar.
- Marcar series y terminar deja la sesión cerrada con sus datos tras reinicio.
- La siguiente sesión del mismo día propone lo último hecho.
- Cancelar no deja sesión fantasma.
- El fallo queda en el log interno sin mostrarse en UI.
- `./gradlew assembleDebug` termina OK.
- En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `data/repository/RoomWorkoutRepository`: al iniciar, persiste las entradas prefilladas una sola vez; `observeSession` lee DB; `toggle/update/adjust` actualizan la copia; `finish` pone `endedAt`; `discard` borra lógico.
- `feature/workout/WorkoutViewModel`: misma forma; sin IDs inventados en UI.
- Registro interno con log del sistema (sin dependencias nuevas, sin UI).
- Sets: se guardan tal cual; la copia de la 1ª y la propagación siguen solo en pantalla.
## 7. Plan de tareas
1. Confirmar causa con el log del móvil (tú: `adb logcat` del fallo).
2. Persistir prefill al iniciar en `data/repository/RoomWorkoutRepository.kt`.
3. Leer sesión desde DB + cierre sin fantasmas en `data/repository/RoomWorkoutRepository.kt`.
4. Registro interno del fallo (log, sin UI).
5. Verificación = `./gradlew assembleDebug` OK + prueba en móvil físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK.
- En móvil: iniciar, marcar, terminar y reabrir propone lo último hecho; sin fantasmas.
- Edge-to-edge sin cambios.
## 9. Riesgos / No romper
- No cambiar `applicationId`; misma keystore + subir `versionCode`.
- Sin migración (sin cambio de esquema).
- La UI solo ve `domain/model`.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Historial visible de sesiones.
- Gráfica de progreso por ejercicio (usa los datos guardados aquí).
## Preguntas abiertas
- Causa exacta del fallo (pendiente de tu log).
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Iniciar una rutina abre la sesión sin fallar.
- [x] Marcar series y terminar deja la sesión cerrada con sus datos tras reinicio.
- [x] La siguiente sesión del mismo día propone lo último hecho.
- [x] Cancelar no deja sesión fantasma.
- [x] El fallo queda en el log interno sin mostrarse en UI.
- [x] `./gradlew assembleDebug` termina OK.
- [x] En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
