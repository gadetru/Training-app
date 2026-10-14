# Guardar rutina con barra auto-oculta
Estado: implementado
Depende de: specs/004-editar-rutina-spec.md (el constructor con su pie fijo); se implementa sobre la rama 016-sesion-edicion
Fecha de creación: 2026-10-14
Descripción: El pie fijo "Finalizar y Guardar Rutina" tapa contenido al editar. La barra se oculta al bajar y reaparece al subir (patrón oficial M3), sin mover el botón de sitio.
## 1. Objetivo
Editar rutinas largas sin que el pie tape las series. El botón sigue siempre accesible sin ir al final.
## 2. Alcance (entra / no entra)
Entra:
- El pie del constructor se oculta al bajar y reaparece al subir.
- El Guardar rápido de la cabecera sigue igual.
- Retoque de los smokes del constructor si el ocultado los rompe.
No entra:
- Mover el botón dentro del flujo ni quitar el pie fijo.
- La barra de la sesión en vivo (queda para futuro spec).
- Lógica de datos, Room, catálogo ni dependencias.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Solo UI de `feature/routines` (`RoutineEditScreen.kt:199-262`, columna con scroll + `SaveFooter` fijo en `:255-262`). Sin ViewModel ni repositorio: el ocultado es estado solo de pantalla. Flujo de guardado intacto. Retrofit y catálogo no se tocan.
## 4. Requisitos funcionales + no-funcionales
- Al bajar en la lista, el pie se esconde (animado); al subir, vuelve.
- Con poco contenido (sin scroll) el pie siempre visible.
- El teclado abierto mantiene su comportamiento (`imePadding` no se rompe).
- Botes de `ui/theme`, regla edge-to-edge (el pie conserva `navigationBarsPadding`).
## 5. Criterios de aceptación verificables
- Al bajar, el pie desaparece y la última serie queda a la vista.
- Al subir, el pie reaparece sin pulsar nada.
- "Finalizar y Guardar Rutina" guarda igual que antes.
- El Guardar de la cabecera sigue funcionando.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Solo `RoutineEditScreen.kt`: comportamiento tipo `exitAlwaysScrollBehavior` de M3 (verificado en Context7, ID `/websites/developer_android_reference_kotlin_androidx_compose_material3`, https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomAppBar.composable) aplicado al pie propio: se conecta al scroll de la columna y desplaza/oculta el pie según la dirección.
- Sin cambios en ViewModel, repositorios, Room (sin migración), DTOs ni Gradle.
- No se toca: `applicationId`, keystore/`versionCode`, cabecera, diálogos.
## 7. Plan de tareas
1. Ocultado del pie según dirección del scroll en `RoutineEditScreen.kt`.
2. Retoque de smokes del constructor en `feature/routines` si lo necesitan.
3. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración; `CatalogMappingTest` como regresión).
- `./gradlew testDebugUnitTest` verde.
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico (bajar/subir, guardar desde el pie y desde la cabecera, con teclado abierto).
## 9. Riesgos / No romper
- Con lectores de pantalla el M3 no reacciona al scroll: el pie debe quedar visible (no ocultar por defecto).
- No romper el guardado ni el `imePadding` con teclado.
- No cambiar `applicationId`, keystore/`versionCode` ni dependencias Gradle.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Mismo auto-ocultado para la barra de la sesión en vivo (`SessionBar` en `WorkoutScreen.kt`) → futuro spec.
- Picker a alto completo → `specs/017-picker-alto-completo-spec.md`.
## Preguntas abiertas
- Decidido: número 018 (el 017 queda reservado al picker); base la rama 016; solo el editor; verificación en ambos dispositivos.
- ¿Animación de ocultado: deslizamiento (defecto M3) o fundido?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Al bajar, el pie desaparece y la última serie queda a la vista.
- [x] Al subir, el pie reaparece sin pulsar nada.
- [x] "Finalizar y Guardar Rutina" guarda igual que antes.
- [x] El Guardar de la cabecera sigue funcionando.
- [x] `./gradlew assembleDebug` termina OK.
- [x] `./gradlew testDebugUnitTest` termina en verde.
- [x] `./gradlew connectedDebugAndroidTest` en verde en emulador y en físico.
