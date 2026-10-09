# Fixes sesión en vivo: modal numérico + Comenzar + tap único
Estado: implementado
Depende de: Ninguno (sigue a 005/009/011 ya en main)
Fecha de creación: 2026-10-09
Descripción: Quitar los steppers −/+ de KG/REPS/PAUSA por un modal de edición al pulsar el número, que el cronómetro arranque detenido con botón Comenzar, y que abrir un ejercicio colapsado cueste un solo tap.
## 1. Objetivo
Al pulsar un número de serie se edita en un modal aparte. El contador no auto-arranca. Abrir colapsado cuesta un tap.
## 2. Alcance (entra / no entra)
Entra:
- Modal que edita KG (con signo), REPS (entero) y PAUSA (segundos) con teclado numérico y confirmación.
- Cronómetro a 00:00 detenido al abrir; botón Comenzar lo arranca y pasa a Pausar/Reanudar.
- Colapsado: 1 tap en tarjeta expande; solo el nombre abre la ficha sin expandir a la vez.
- Textos Comenzar/Pausar/Reanudar + estado ENTRENAMIENTO ACTIVO/EN PAUSA coherentes.
No entra:
- Cambio de esquema Room, DTOs, catálogo, CUSTOM, reglas de propagación/prefill.
- Rest overlay, finalizar/descartar, iconos extended, fotos Roborazzi nuevas.
- Spec 014 (perfil/home).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/workout`: `WorkoutScreen.kt` (SetRow, Stepper, SessionBar, ExerciseAccordion) + `WorkoutViewModel.kt` (estado efímero de pantalla, tick, onKgChange/onRepsChange/adjustRest). Flujo pantalla → ViewModel con datos observables → repositorio → base local; red solo rellena. Sin Room ni catálogo. Modelos `WorkoutUiState/WorkoutExerciseUi/SetEntry` (`domain/model/Workout.kt`). Guardado con `updateEntry/adjustRest` existentes.
## 4. Requisitos funcionales + no-funcionales
- Pulsar KG/REPS/PAUSA abre un modal (diálogo Material3, gestiona insets solo) con los 3 campos y teclado numérico.
- Confirmar guarda de golpe (KG con signo, REPS ≥ 0, PAUSA ≥ 0) vía rutas existentes; cancelar no guarda.
- Al abrir sesión: `elapsedSec` quieto en 0 y botón `Comenzar`; tras pulsar, cuenta y ofrece `Pausar`; pausado no avanza.
- 1 tap en tarjeta colapsada la expande; pulsar el nombre abre la ficha y no expande a la vez.
- Hit targets ≥ 48dp; edge-to-edge intacto (cabecera con statusBarsPadding, barra con navigationBarsPadding).
## 5. Criterios de aceptación verificables
- Pulsar un KG abre el modal con el valor actual; confirmar cambia el valor en la fila.
- REPS y PAUSA se editan igual desde el mismo modal.
- Al abrir sesión el cronómetro marca 00:00 quieto y el botón dice Comenzar.
- Pulsar Comenzar arranca el conteo y el botón pasa a Pausar.
- Pausar congela el conteo; Reanudar lo retoma.
- 1 tap en tarjeta colapsada la abre; el nombre abre la ficha detalle.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `WorkoutScreen`: sustituir `Stepper` de SetRow por celdas de valor pulsables que abren un diálogo con 3 campos; diálogo con estado de pantalla, sin tocar Room directo.
- `WorkoutViewModel`: arranque con pausa activa (`isPaused=true`, `elapsedSec=0`); `onPauseToggle` alterna Comenzar→Pausar→Reanudar sin lógica nueva de repo; edición por `updateEntry` (KG+REPS) y `adjustRest` (PAUSA) o equivalente existente.
- Colapsado: un solo `clickable` por tarjeta que expande; el nombre con su propio tap a ficha que consume el evento para no expandir.
- No se toca: entidades/DAOs/migraciones, DTOs, `CATALOG_TAG`, mapeos, iconos.
## 7. Plan de tareas
1. Modal de edición (3 campos + validación + confirmar/cancelar) en `feature/workout/WorkoutScreen.kt`.
2. Cablear guardado a `WorkoutViewModel` vía `updateEntry/adjustRest` existentes.
3. Arranque detenido (`isPaused=true`, `elapsedSec=0`, texto Comenzar) en `WorkoutViewModel.kt` + `WorkoutScreen.kt` (SessionBar).
4. Arreglar tap único en `ExerciseAccordion` colapsado (`WorkoutScreen.kt`).
5. Coherencia de textos de estado (ACTIVO/EN PAUSA) en `SessionHeader`.
6. Verificación = `./gradlew assembleDebug` OK + prueba en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin `CatalogMappingTest`: no toca mapper).
- Emulador Pixel 6 API 34 + físico, automático: modal edita, Comenzar arranca, 1 tap expande, arriba/abajo nada bajo barras.
## 9. Riesgos / No romper
- No cambiar `applicationId`, ni keystore/`versionCode`, ni dependencias Gradle.
- No romper relleno con la última sesión, descanso flotante, finalizar/descartar, navegación a ficha con `Uri.encode`.
- UI solo ve `domain/model`; mapeo en `data/mapper`.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Stepper fino dentro del modal (±) si se echa de menos — detectado en `WorkoutScreen.kt:781`.
- Pausar también el descanso flotante — estado en `WorkoutViewModel.kt:111`.
## Preguntas abiertas
Ninguna (interrogatorio respondido 2026-10-09).
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Pulsar KG abre modal con valor actual y confirmar lo cambia.
- [x] REPS y PAUSA editables desde el mismo modal.
- [x] Cronómetro quieto en 00:00 al abrir con botón Comenzar.
- [x] Comenzar arranca conteo y pasa a Pausar.
- [x] Pausar congela y Reanudar retoma.
- [x] 1 tap expande colapsado; nombre abre ficha.
- [x] `./gradlew assembleDebug` OK.
- [x] Probado en emulador Pixel 6 API 34 + físico automático.
