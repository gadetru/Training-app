# Sesión con ejercicios colapsados y avance automático
Estado: aprobado
Depende de: specs/016-sesion-edicion-spec.md (el mapa de expandidos y su defecto) y specs/013-sesion-vivo-fixes-spec.md (el overlay del descanso con su cronómetro)
Fecha de creación: 2026-10-15
Descripción: Al abrir la sesión todos los ejercicios nacen colapsados y el usuario expande donde va a empezar. Al acabarse el descanso de la última serie, el ejercicio se colapsa solo y el siguiente pendiente se expande para seguir en orden.
## 1. Objetivo
Que la sesión muestre poco de un vistazo y avance sola: colapsados al abrir, colapso auto al completar y siguiente expandido sin taps extra. El tap manual siempre manda.
## 2. Alcance (entra / no entra)
Entra:
- Todos colapsados al abrir la sesión, sin excepción.
- Al cerrarse el descanso (a 0 o con Terminar), si el ejercicio queda sin pendientes se colapsa y el siguiente pendiente se expande.
- Sin descanso (0 s): el colapso y el avance ocurren al marcar la última serie.
- Re-expandir o colapsar a mano con un tap sigue funcionando siempre.
No entra:
- Retomar sesiones minimizadas (va en el spec 022).
- Sustituir ejercicios (va en el spec 021).
- Cambios visuales: se reutiliza la tarjeta colapsada actual, sin distintivo nuevo.
- Lógica de datos, Room, catálogo ni dependencias.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Solo `feature/workout` (`WorkoutViewModel.kt:75-88`: defecto primer-pendiente-expandido; `:110-131` el tick que cierra el descanso a 0; `onToggleDone:199-221`, `onRestFinish:234-236`). El colapso auto escribe en el mismo mapa de expandidos de pantalla que el toggle manual (estado solo de pantalla, no se guarda). Sin repositorio ni Room. Catálogo y DTOs no se tocan.
## 4. Requisitos funcionales + no-funcionales
- Al abrir, ningún ejercicio nace expandido aunque tenga pendientes.
- Cierre del descanso con ejercicio sin pendientes: colapsa ese y expande el primer pendiente siguiente en orden.
- Si ya no quedan pendientes, solo colapsa (no expande nada).
- El tap manual posterior pisa lo automático sin coste extra (sin doble tap).
- Botes de `ui/theme`, sin iconos nuevos; regla edge-to-edge intacta.
## 5. Criterios de aceptación verificables
- Al abrir la sesión todos los ejercicios están colapsados.
- Al cerrarse el descanso de la última serie, el ejercicio se colapsa.
- Tras el colapso auto, el siguiente pendiente aparece expandido.
- Sin pendientes restantes, nada se expande.
- Un tap re-expande un ejercicio auto-colapsado.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `WorkoutViewModel`: el defecto del mapa pasa a colapsado (`combine` en `WorkoutViewModel.kt:81-88` y `onToggleExpanded:155-168`); al cerrar el descanso (`onRestFinish` + rama a 0 del tick) y al marcar última serie sin descanso (`onToggleDone`), si el ejercicio queda sin pendientes se guarda colapsado y se guarda expandido el primer pendiente siguiente.
- Sin cambios en repositorios, Room (sin migración), DTOs ni Gradle.
- No se toca: `applicationId`, keystore/`versionCode`, cabecera, diálogos, picker.
## 7. Plan de tareas
1. Defecto colapsado al abrir en `WorkoutViewModel.kt` (combine + `onToggleExpanded`).
2. Colapso auto al cerrar descanso y avance al siguiente en `WorkoutViewModel.kt` (cierre a 0, `onRestFinish`, `onToggleDone` sin descanso).
3. Tests JVM de apertura colapsada, colapso, avance y re-expansión manual (fakes, espejo `data/repository` o junto a `WorkoutLiveEditTest`).
4. Smoke del auto-colapso en `feature/workout` (`androidTest`).
5. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración; `CatalogMappingTest` como regresión, no se toca mapper).
- `./gradlew testDebugUnitTest` verde (tests nuevos).
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico (apertura colapsada, colapso al cerrar descanso, avance al siguiente, re-expansión con tap).
## 9. Riesgos / No romper
- No cambiar `applicationId`, keystore/`versionCode` ni dependencias Gradle.
- El auto-avance no debe pelear con el tap: el manual posterior siempre gana.
- No colapsar mientras el descanso sigue abierto ni con pendientes restantes.
- No romper el fix de los 2 taps del 013 (el defecto que asume cambia).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Retomar minimizadas con su estado de colapsado → `specs/022-...-spec.md` (el mapa es de pantalla; al retomar se redefine).
- Sustituir ejercicio desde colapsado → `specs/021-...-spec.md`.
- Ceros a la izquierda en el modal → `specs/020-...-spec.md`.
## Preguntas abiertas
- Decidido: disparador al cerrar el descanso (con 0 s, al marcar); estado en mapa de pantalla re-expandible; se reutiliza el colapsado sin distintivo; todos colapsados sin excepción + avance auto al siguiente; JVM + smoke nuevo; depende de 016 + 013.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Al abrir la sesión todos los ejercicios están colapsados.
- [x] Al cerrarse el descanso de la última serie, el ejercicio se colapsa.
- [x] Tras el colapso auto, el siguiente pendiente aparece expandido.
- [x] Sin pendientes restantes, nada se expande.
- [x] Un tap re-expande un ejercicio auto-colapsado.
- [x] `./gradlew assembleDebug` termina OK.
- [x] `./gradlew testDebugUnitTest` termina en verde.
- [x] `./gradlew connectedDebugAndroidTest` en verde en emulador y en físico.
