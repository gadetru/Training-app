# Ceros a la izquierda en los campos numéricos
Estado: implementado
Depende de: specs/013-sesion-vivo-fixes-spec.md (crea el modal de serie) y specs/016-sesion-edicion-spec.md (edición en vivo que usa esos campos)
Fecha de creación: 2026-10-15
Descripción: Escribir "012" debe quedar "12". Una sola regla de limpieza de números, compartida por el modal de sesión, las celdas del constructor y el perfil, aplicada mientras se teclea.
## 1. Objetivo
Que ningún campo numérico muestre ni guarde ceros a la izquierda, con una regla única y probada, sin cambiar el aspecto de los campos.
## 2. Alcance (entra / no entra)
Entra:
- Limpiar al teclear en el modal de serie (KG/REPS/PAUSA), en las celdas del constructor (KG/REPS/DESCANSO) y en la duración de rutina.
- Alinear el perfil (edad/altura/peso) a la misma regla compartida.
- `0` suelto se mantiene; `0.5` / `0,5` se respetan.
No entra:
- El RIR (es lista de opciones, no texto).
- Cambiar qué valores son válidos ni el signo del KG (sigue `+` lastre / `0` corporal / `-` ayuda).
- Retomar sesiones, sustituir ejercicios, duración auto/manual (otros specs).
- Cambios de datos, Room, catálogo o Gradle.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/workout` (`SetEditDialog` en `WorkoutScreen.kt:1107-1148`), `feature/routines` (`CellInput` `RoutineEditScreen.kt:948-967` y duración `:411-415`) y `feature/profile` (`ProfileScreen.kt:105-112`). Regla pura en `core/ui/NumberInput.kt` (nueva). Hoy `formatWeight` está duplicada (`WorkoutScreen.kt:111` y `RoutineEditScreen.kt:104`). Sin repositorio ni Room: el guardado ya valida (`RoutineEditViewModel.kt:130-144`).
## 4. Requisitos funcionales + no-funcionales
- Al teclear, el texto nunca muestra ceros iniciales; se corrige en el momento.
- `012` → `12`; `00` → `0`; `0` → `0`; `0.5`/`0,5` intactos; coma decimal aceptada y mostrada como punto.
- Máximo de dígitos igual que hoy en cada campo (p. ej. perfil `take(3)`).
- Sin cambios visuales: mismos textos, foco y tamaños.
## 5. Criterios de aceptación verificables
- En el modal de serie, escribir `012` deja `12` en REPS/PAUSA y `0.5` en KG se mantiene.
- En las celdas del constructor, `012` deja `12` y `0,5` deja `0.5`.
- En la duración de rutina, `012` deja `12`.
- En el perfil, el comportamiento no cambia (sigue sin ceros iniciales).
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `core/ui/NumberInput.kt`: `sanitizeInt(raw)` (solo dígitos, quita ceros iniciales salvo un `0`) y `sanitizeDecimal(raw)` (dígitos + un separador, `,`→`.`, respeta `0.x` y un punto final mientras se escribe). Funciones puras, sin Compose.
- `SetEditDialog`: envolver `onValue` de KG/REPS/PAUSA con el helper.
- `CellInput`: envolver `onValueChange` (borrador) con el helper según `keyboardType`.
- Duración (`RoutineEditScreen.kt:413`) y perfil (`ProfileScreen.kt:103-112`): usar el mismo helper.
- Sin cambios en ViewModel, repositorios, Room (sin migración), DTOs ni Gradle.
- No se toca: `applicationId`, keystore/`versionCode`, header, diálogos ajenos, picker.
## 7. Plan de tareas
1. Crear `core/ui/NumberInput.kt` con `sanitizeInt`/`sanitizeDecimal`.
2. Aplicar en `SetEditDialog` (`WorkoutScreen.kt`).
3. Aplicar en `CellInput` y duración (`RoutineEditScreen.kt`).
4. Alinear perfil (`ProfileScreen.kt`) al helper.
5. Test JVM del helper (`012→12`, `0→0`, `00→0`, `0.5`/`0,5`, vacío, límite de dígitos).
6. Smoke del modal/celdas sin ceros (`androidTest`).
7. Verificación = `assembleDebug` + `testDebugUnitTest` + `connectedDebugAndroidTest` (emulador y físico).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK; `CatalogMappingTest` como regresión (no se toca mapper).
- `./gradlew testDebugUnitTest` verde con el test del helper.
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico (modal, celdas, duración).
## 9. Riesgos / No romper
- No romper la propagación ni el commit al perder el foco de `CellInput` (`RoutineEditScreen.kt:1069-1093`): el helper solo limpia el borrador.
- No romper el signo del KG ni el `0` corporal válido.
- No cambiar el borrador a vacío de forma que se revierta o se pierda lo tecleado.
- Nada de Gradle, `applicationId`, keystore ni migraciones.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Unificar `formatWeight`/`formatClock` duplicadas en un helper común (detectado en `WorkoutScreen.kt:111` y `RoutineEditScreen.kt:104`).
- Retomar minimizadas → `specs/022-...-spec.md`.
- Sustituir ejercicio → `specs/021-...-spec.md`.
- Duración auto/manual → `specs/023-...-spec.md`.
## Preguntas abiertas
- Decidido: `0` suelto se mantiene; `0.5`/`0,5` se respetan; helper compartido; se normaliza al teclear; JVM + smoke; depende de 013 + 016.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Modal de serie: `012` → `12` y `0.5` se mantiene en KG.
- [x] Celdas del constructor: `012` → `12` y `0,5` → `0.5`. (código confirmado `RoutineEditScreen.kt:1088-1090`; `RoutineEditSmokeTest` 3/3 pasando en físico POCO F6)
- [x] Duración de rutina: `012` → `12`. (código confirmado `RoutineEditScreen.kt:439`; sin test dedicado de duración en físico)
- [x] Perfil: sigue sin ceros iniciales. (código confirmado `ProfileScreen.kt:103-105`; `WorkoutSmokeTest.profile_muestra_titulo_y_cta_guardar` pasando en físico POCO F6)
- [x] `./gradlew assembleDebug` termina OK.
- [x] `./gradlew testDebugUnitTest` termina en verde.
- [x] `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 (48/48).
- [x] `./gradlew connectedDebugAndroidTest` en verde en dispositivo físico POCO F6 (ae4a376e, Android 16): 48/48 tests OK, incluido `NumberInputSmokeTest`.
