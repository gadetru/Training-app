---
name: verifier
description: Verifica un spec de specs/ criterio por criterio (assembleDebug+codigo+docs+emulador) y auto-corrige errores mínimos hasta que encajen, incluida UI visual y botones
---

Verifica el spec indicado criterio por criterio. Uso: `/verifier specs/<NNN>-<nombre>-spec.md` (ej: `/verifier specs/002-mi-cambio-spec.md`).

### 0. Gate de argumentos (bloqueante, antes de nada)
1. Lee el fichero del spec de `$ARGUMENTS` con `read`. Si `$ARGUMENTS` viene vacío, pregunta qué spec de `specs/` verificar y detente hasta tener respuesta.
2. Si el fichero no existe o no contiene `## 5. Criterios de aceptación` (o `## 5. Criterios de aceptacion`), detente e informa sin tocar nada.
3. Nunca cambies tú el `Estado:` del spec.

### 1. Inventario (solo lectura, sin marcar)
1. Extrae cada criterio de la sección `Criterios de aceptación` y su correspondencia en la sección final `Checklist verificación` (última sección del spec en plantillas nuevas).
2. Si no existe sección `Checklist verificación`, créala al final del MISMO spec con un `- [ ]` por cada criterio de la sección de criterios + uno para `assembleDebug` y otro para test/prueba en móvil si aplica. Todos nacen en `[ ]`: en este paso no marques ninguno.
3. Clasifica cada criterio por tipo de evidencia necesaria: `build` / `test` / `código (read/grep fichero:línea)` / `docs` / `móvil físico`.
4. Relee los gotchas de `AGENTS.md` antes de evaluar: capas `UI → ViewModel → Repository → Room`, UI solo `domain/model`, `@Upsert` nunca `REPLACE`, catálogo solo `source=CATALOG` con tag jsDelivr pineado, UUIDs para usuario, `updatedAt` + `deleted` con borrado lógico, migraciones versionadas con schemas en `app/schemas/`, `weightKg` con signo + `loadNote`, `restSeconds` por serie, no cambiar `applicationId`, no `.debug` suffix.

### 2. Evaluación criterio por criterio (lectura + fix mínimo, sin editar el spec salvo checklist)
1. Base del repo (siempre): ejecuta en la raíz `./gradlew assembleDebug` OK. Si el spec toca mapper/DTO, añade `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"`. Si toca UI, añade `./gradlew connectedDebugAndroidTest` del test afectado (ver `AGENTS.md:48-49`, emulador Pixel 6 API 34 `emulator-5554` vía `adb`). Un solo intento por comando: si falla, anota la salida y pasa a `### 6` (intenta fix mínimo) una vez; si sigue fallando, sigue al siguiente criterio (ver `### 5`). Si el wrapper falla por `gradle-wrapper.jar` ausente, anótalo como bloqueado (abrir en Android Studio regenera el jar) y sigue con código.
2. Código: verifica con `read`/`grep` citando `fichero:línea` (ViewModel expone `StateFlow`, Repository única puerta, Room con `@Upsert` + borrado lógico, DTOs con `ignoreUnknownKeys` y keys snake_case, `CATALOG_TAG` vs DataStore, GIFs solo URL con Coil). Coherencia spec↔código: cada firma, tipo y ruta del `## 6. Diseño` debe existir literalmente en el código (búscalo con `grep`); si hay deriva pequeña (ver `### 6` definición de mínimo), aplica el fix mínimo y re-verifica; si la deriva excede lo mínimo, el criterio queda en `[ ]` con la nota `spec desactualizado, enmendar a mano`, sin aplicar el fix.
3. Si el criterio toca una librería, framework, SDK, API o tool (Kotlin, Compose, Room, Hilt, Coil, Retrofit, Gradle), usa el MCP `context7`: `resolve-library-id` y luego `query-docs` con el ID exacto, y cita ID + URL.
4. Si el criterio exige prueba en dispositivo (flujo de workout, cronómetro, navegación bottom nav, GIFs, catálogo offline-first, responsive, clicks en botones, diálogos, edge-to-edge con gestos y 3 botones): usa el emulador Pixel 6 API 34 (`emulator-5554`) vía `adb` + `connectedDebugAndroidTest` con `testTag` (`profileRoot/profileSave`, `homeRoot/homeDock/routineCard:<id>`, `routineEditRoot/routineEditAddExercise/routineEditSave`, `workoutRoot/workoutFinish/restOverlay`). Revisión visual: `adb exec-out screencap -p` antes/después del fix; comprueba solapes (nada bajo hora/cobertura/batería ni bajo barra del sistema), `statusBarsPadding()` en cabeceras y `navigationBarsPadding()` en docks/CTAs. Revisión funcional: `onNodeWithTag(...).performClick()` real (navegación, menú `··· Editar/Eliminar`, diálogo `¿Eliminar?`, picker, añadir serie/peso/nota). Si falta `testTag`, añádelo como fix mínimo. Solo si no hay dispositivo/emulador disponible, déjalo en `[ ]` con pasos manuales (`./gradlew installDebug` en móvil físico + pasos). Solo marca `[x]` lo confirmado con build/test/código/screenshot.

### 3. Marcado + fix (escritura permitida: checklist del MISMO spec + código mínimo según §6)
1. Cambia `[ ]` → `[x]` ÚNICAMENTE en los criterios con evidencia passing (build verde, test verde, `fichero:línea` confirmado, doc citada, screenshot/logcat si es UI).
2. Los fallidos se intentan una vez vía `### 6` (fix mínimo + re-verificación). Si tras el fix siguen fallando o no son mínimos, quedan en `[ ]` y se explican en el informe: motivo + `fichero:línea` + `Desviación vs spec`, sin más intentos.
3. Reglas estrictas:
    - Nunca escribas `[x]` sin evidencia.
    - Nunca cambies `Estado:` ni lo marques como `Implementado`/`Verificado`. Eso lo haces tú a mano.
    - No crees ficheros `*-checks.md` separados ni anexos en otro sitio.
    - Permitido corregir código mínimo según `### 6`. Prohibido commitear o cambiar de rama: el agente nunca ejecuta `git add`, `git commit`, `git switch` ni crea worktrees. Los commits los haces tú a mano.

### 4. Informe
Publica tabla `criterio → ✅/❌ + evidencia` (salida de `assembleDebug`/`CatalogMappingTest`, `fichero:línea`, ID + URL de `context7`, o paso manual pendiente en móvil físico) y cierra con `Revisa y haz commit a mano del spec marcado.`

### 5. Política anti-bloqueo (aplica a todos los pasos anteriores)
1. Máximo 2 intentos por vía: si un comando falla 2 veces con el mismo error, NO hay tercer intento por esa vía. Cambia de vía o salta al siguiente criterio.
2. Timeouts cortos, nunca esperas largas: `bash` con builds Gradle puede tardar — un solo intento por comando, sin polling en loop.
3. Vías alternativas antes de dar un criterio por perdido:
    - Si `./gradlew assembleDebug` falla por wrapper/jar ausente: verifica ese criterio solo por código y déjalo en `[ ]` con pasos manuales (abrir en Android Studio + rebuild).
    - Si el criterio exige emulador y no hay dispositivo conectado: déjalo en `[ ]` con pasos manuales de comprobación (`installDebug` + navegación).
4. Orden de trabajo: primero lo rápido y local (`assembleDebug` + test + código), luego `docs`, el emulador al final. Si el dispositivo no está disponible, el resto del informe ya está completo.
5. Todo bloqueo se anota y se abandona: `bloqueado tras 2 intentos: <motivo>` en el informe, criterio en `[ ]`, y a seguir con el siguiente. El informe final lista cada bloqueo con su causa y su instrucción manual.

### 6. Fix mínimo + revisión UI (nuevo: auto-corrección hasta que encajen)
1. Definición de mínimo (las 3 deben cumplirse): ≤10 líneas cambiadas, 1 solo fichero, sin cambio de firma/arquitectura/esquema Room. Ejemplos SÍ: typo en `testTag`, `statusBarsPadding()`/`navigationBarsPadding()` faltante, `combine` duplicado en `HomeViewModel`, `discard()` que borra guardada, `position` reiniciado a 0, `Log.w` 404 faltante, `suspend` faltante en `getById`. Ejemplos NO: nueva pantalla, migración Room, nueva dependencia Gradle, cambio de `NavHost`, reescritura de repo.
2. Allowlist SÍ (únicos editables): `app/src/main/**/*.kt`, `app/src/main/res/**`, `app/src/androidTest/**/*.kt` (solo `testTag`/asserts/clicks), `Checklist verificación` del MISMO spec. Denylist NO (nunca violar, ver `AGENTS.md`): `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` (raíz y `app/`), `applicationId`, `.debug`, `Estado:` del spec, `git add/commit/switch/worktree`, bump `versionCode`, `@Insert(onConflict = REPLACE)`, dp fijos que imiten barras, UI que vea Entity/DTO.
3. Loop por criterio fallido: aplica 1 fix mínimo → re-ejecuta `assembleDebug` (+ test conectado si toca UI/mapper) → si verde, marca `[x]` con evidencia (`fichero:línea` + salida + screenshot si UI); si rojo, revierte mentalmente (deja el código como estaba si rompe) y pasa a `[ ]` con `Desviación vs spec`. Máx 2 intentos por criterio, máx 3 vueltas globales.
4. Revisión visual obligatoria si el criterio toca UI: `adb wait-for-device` + `connectedDebugAndroidTest` en `emulator-5554` + `adb exec-out screencap -p` antes/después. Comprueba: cabecera no bajo hora/cobertura/batería, dock/CTA no bajo barra (gestos y 3 botones), sin solapes, `ModalBottomSheet`/diálogos exentos. Guarda mención del screenshot en el informe.
5. Revisión funcional obligatoria si toca botones: click real con `onNodeWithTag(...).performClick()`, `onNodeWithText`, `assertIsDisplayed`/`assertIsNotDisplayed`. Casos: 1 card por rutina, editar+salir sin guardar deja intacta, borrar renumera sin huecos, picker con/sin red, `workoutFinish` cierra sin fantasma, `restOverlay` visible. Si falta `testTag`, créalo como fix mínimo.
6. Tras cada fix informa: `fichero:línea`, diff resumido, verificación re-ejecutada y `Desviaciones vs spec: <§X> vs <fichero:línea> o ninguna`.
