# Picker de ejercicios a alto completo
Estado: aprobado
Depende de: specs/003-lista-ejercicios-spec.md (el picker con su buscador y filtros); se implementa sobre la rama 016-sesion-edicion
Fecha de creación: 2026-10-14
Descripción: La hoja del picker se queda al 88% y muestra poca lista. Pasa a alto completo (100%) manteniendo gestos y piezas.
## 1. Objetivo
Ver más ejercicios de un vistazo al añadir al constructor. Solo cambia la altura.
## 2. Alcance (entra / no entra)
Entra:
- Contenido de la hoja del 88% al 100% de alto.
No entra:
- Buscador, chips, filas, pie Confirmar ni ficha: intactos.
- Gestos (arrastre, cerrar por fuera) y tirador: intactos.
- Lógica de datos, Room, catálogo ni dependencias.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Solo UI de `feature/exercises` (`ExercisePickerSheet.kt:136-162`: `ModalBottomSheet` con `skipPartiallyExpanded` y contenido al 88% en `:159`). El contenido (`ExercisePickerContent`, `:165-234`) ya reparte con pesos (lista con `weight(1f)`), así que estira solo. Sin ViewModel ni repositorio. La hoja gestiona insets sola (excepción edge-to-edge).
## 4. Requisitos funcionales + no-funcionales
- Al abrir, la hoja ocupa todo el alto.
- Tirador, cierre por fuera y Confirmar funcionan igual.
- Con teclado abierto, la búsqueda sigue visible (`imePadding` si lo usa, no romperlo).
- Botes de `ui/theme`; sin iconos ni dependencias nuevas.
## 5. Criterios de aceptación verificables
- La hoja abre a alto completo con varias rutinas de lista visible.
- El arrastre cierra igual que antes.
- Buscar, filtrar por chip y Confirmar funcionan igual.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Solo `ExercisePickerSheet.kt:159`: el alto del contenido pasa del 88% al completo. Resto intacto.
- Sin cambios en ViewModel, repositorios, Room (sin migración), DTOs ni Gradle.
- No se toca: `applicationId`, keystore/`versionCode`, cabecera ni diálogos.
## 7. Plan de tareas
1. Alto completo del contenido en `ExercisePickerSheet.kt`.
2. Smoke de apertura a alto completo en `feature/exercises` si hace falta.
3. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador y físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (sin migración; `CatalogMappingTest` como regresión).
- `./gradlew testDebugUnitTest` verde.
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico (apertura, arrastre, buscar+Confirmar).
## 9. Riesgos / No romper
- A alto completo el gesto de arrastre debe seguir cerrando (no dejar hoja bloqueada).
- No romper el pie Confirmar ni la lista con pocos resultados.
- No cambiar `applicationId`, keystore/`versionCode` ni dependencias Gradle.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Reintento manual con picker vacío y catálogo en inglés (deuda P3 del README).
- Auto-ocultado del pie de la sesión en vivo → futuro spec (el 018 cubre solo el constructor).
## Preguntas abiertas
- Decidido: 100% (no 95%), gestos intactos, solo altura, verificación en ambos.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] La hoja abre a alto completo con varias rutinas de lista visible.
- [ ] El arrastre cierra igual que antes.
- [ ] Buscar, filtrar por chip y Confirmar funcionan igual.
- [ ] `./gradlew assembleDebug` termina OK.
- [ ] `./gradlew testDebugUnitTest` termina en verde.
- [ ] `./gradlew connectedDebugAndroidTest` en verde en emulador y en físico.
