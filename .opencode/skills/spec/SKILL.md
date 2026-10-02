---
name: spec
description: Genera spec de implementación (plan) sin escribir código. Usar cuando se pida planificar en specs/, plan de implementación o Spec-Driven antes de picar código.
---

Genera un plan de implementación Spec-Driven. NO escribas código.

Nombre: usa `$ARGUMENTS` si viene (ej: `/spec filtro-ejercicios`). Si viene vacío, deriva un nombre corto kebab-case del contexto de la conversación (ej: `filtro-ejercicios`, `sync-catalogo`).

Sanea el nombre: minúsculas, espacios/_ a `-`, solo `[a-z0-9-/.]`, sin espacios.

Numeración: cada spec lleva prefijo secuencial de 3 dígitos. Lista con `glob` los `specs/*-spec.md`, extrae los prefijos `^(\d+)-` y elige el menor `NNN` libre empezando en `001` (los números de specs borrados se reutilizan). Si `$ARGUMENTS` ya trae prefijo numérico (`/spec 002-mi-cambio`), respétalo tras sanear.

Destino: `specs/<NNN>-<nombre>-spec.md` (ej: `specs/002-filtro-ejercicios-spec.md`). Si `specs/` no existe, créalo. Si el destino ya existe, no sobrescribas: avisa y detente hasta tener otro nombre.

Ejecuta estos 5 pasos, en orden, solo con herramientas de lectura (`read`, `glob`, `grep`, `bash` read-only):

### 1. Contexto (solo lectura)
Lee obligatoriamente antes de preguntar:
- `AGENTS.md` (stack, arquitectura, reglas que el agente suele romper)
- `docs/ARQUITECTURA.md` y `docs/MODELO_DE_DATOS.md` (capas, tablas, flujo offline-first)
- `app/build.gradle.kts` y `gradle/libs.versions.toml` (única fuente de versiones)
- `TrainingApp.kt` (@HiltAndroidApp + Coil) y `MainActivity.kt` (bottom nav: exercises, routines, workout, history)
- Feature/s afectada/s en `feature/{exercises,routines,workout,history}/` (Screen + ViewModel por feature)
- Si toca datos: `data/local/` (entities, `dao/`, `AppDatabase.kt`, `Converters.kt`), `data/remote/` (`ExerciseApi.kt` + `dto/`), `data/repository/`, `data/mapper/`, `domain/model/`, `core/network/CatalogConfig.kt`, `core/datastore/`

Resume en 5-10 líneas: qué features/capas toca, flujo `UI → ViewModel (StateFlow) → Repository → Room`, si Retrofit solo rellena DB, fuente de datos. Cita `fichero:línea`.

### 2. Interrogatorio obligatorio (arquitectura + alcance)
Antes de redactar, llama a `question` con 5-10 preguntas. Mitad arquitectura, mitad alcance. Adapta según la idea, pero cubre siempre:
- Arquitectura: ¿qué `feature/*` toca? ¿qué Screen/ViewModel? ¿lógica en Repository o solo UI?
- Datos: ¿toca `exercises` (CATALOG vs CUSTOM), rutinas, `PlannedSet`, sesiones/`SetEntry`? ¿upsert o borrado lógico (`updatedAt` + `deleted`)?
- Catálogo: ¿toca `CATALOG_TAG`, `CatalogPrefs`, `GET api/{lang}/exercises.json`, DTOs snake_case (`body_part`, `gif_url`)?
- Room: ¿cambio de esquema? (versión + `Migration`, `exportSchema=true`, schemas en `app/schemas/`, `@Upsert` nunca `@Insert(REPLACE)`).
- Sets: ¿toca `weightKg` con signo (`+` lastre, `0` corporal, `-` asistencia), `loadNote`, `restSeconds` por serie? ¿propagación/prefill solo de pantalla (no persistido)?
- Alcance: objetivo, no-objetivos, criterios de aceptación verificables en móvil físico, verificación `./gradlew assembleDebug` (+ `CatalogMappingTest` si toca mapper).
- Dependencias: ¿depende este spec de otro spec anterior de `specs/`? ¿cuál y por qué (orden de implementación)?
- Alcance fino: ¿qué entra y qué NO entra explícitamente en este spec?
- Futuro: ¿qué mejoras, errores extra o ideas detectadas quedan fuera para un posible spec futuro?

Si falta info, pregunta en vez de asumir. No avances al paso 3 sin respuestas o sin que el usuario te diga "sigue con supuestos".

### 3. Diseño mínimo
Describe solo lo necesario, sin código:
- Features/capas a tocar/crear, ViewModels (`StateFlow`), Repository, DAOs, DTOs.
- Room: entidades, `@Upsert`, borrado lógico, migraciones.
- Catálogo: tag jsDelivr pineado (nunca rama), qué rows `source=CATALOG` toca, GIFs solo URL (Coil cachea).
- Qué NO se toca (gotchas `AGENTS.md`): no cambiar `applicationId`, no `applicationIdSuffix ".debug"`, misma keystore fuera del repo + bump `versionCode`, UI solo ve `domain/model`, mapping en `data/mapper`, `gradle-wrapper.jar` ausente (abrir en Android Studio primero), sin emulador.

### 4. Plan de tareas
Lista numerada de tareas pequeñas, cada una con fichero/s implicado/s. Última tarea siempre:
- Verificación = `./gradlew assembleDebug` OK (+ `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"` si toca mapper/DTO). Sin lint/typecheck extra en este repo.

### 5. Generar spec y parar
Escribe `specs/<NNN>-<nombre>-spec.md` con esta plantilla (la checklist con checkboxes es siempre la ÚLTIMA sección):

```md
# <título>
Estado: Borrador
Depende de: <Ninguno | specs/NNN-<nombre>-spec.md (+ motivo)>
Fecha de creación: <YYYY-MM-DD, fecha actual>
Descripción: <1-3 líneas: qué se busca hacer>
## 1. Objetivo
## 2. Alcance (entra / no entra)
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
## 4. Requisitos funcionales + no-funcionales
## 5. Criterios de aceptación verificables
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
## 7. Plan de tareas
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
## 9. Riesgos / No romper
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
## Preguntas abiertas
## 11. Checklist verificación (última, checkboxes listos para /verifier)
```

Cabecera:
- `Estado`: nace como `Borrador`. Solo el usuario lo cambia a mano a `Aprobado` para desbloquear `/spec-impl`. El agente nunca cambia el `Estado`.
- `Depende de`: sale del interrogatorio (paso 2). Si no depende de ningún spec anterior, escribe `Ninguno`.
- `Fecha de creación`: fecha actual en formato `YYYY-MM-DD` (obtenla con `bash` read-only, ej: `date +%F`, o del contexto del sistema).
- `Descripción`: 1-3 líneas, sin código.

Alcance (`## 2`): dos listas explícitas, `Entra:` y `No entra:`. Lo que no entra no se implementa en este spec.

Futuros specs (`## 10`): cada mejora, error extra o idea detectada que quede fuera, en una línea con dónde se detectó (`fichero:línea` si aplica). Sirve de cantera para futuros specs; al convertirse en spec, enlázalo (`specs/NNN-...-spec.md`).

Checklist final (`## 11`): un `- [ ]` por cada criterio de `## 5` + uno para `assembleDebug` y otro para `CatalogMappingTest` (si aplica) / prueba en móvil. Todos nacen en `[ ]`: los marca `/verifier` con evidencia o el usuario a mano. Nada fuera de la checklist se verifica.

Reglas estrictas:
- NO uses `edit`, `write` (salvo para el spec), ni `bash` que modifique. No hagas `checkout`, `switch`, commit ni cambios en `app/src/`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Una vez escrito el spec, detente e informa: path del spec + resumen de 3-5 líneas + `Responde OK para implementar o dime qué ajustar.`
- Prohibido seguir a código sin aprobación explícita del usuario.
