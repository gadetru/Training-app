# Punto actual — Revisión 2026-10-08

> Fecha: 2026-10-08
> Rama en revisión: `010-detalle-ejercicio` (con `main` hasta PR #10 incluido)
> Origen: revisión manual de `docs/`, `specs/`, `README.md`, `AGENTS.md` y estructura de `app/src/main`.

## 0. Cómo leer este documento (para novatos)

Piensa en la app como una **tienda pequeña**:

- **UI (`feature/*/Screen`) = escaparate.** Solo enseña y recoge toques. No guarda ni calcula.
- **ViewModel = dependiente.** Guarda "qué se ve ahora" en una caja observable (`StateFlow`) y pasa recados.
- **Repository (`data/repository`) = almacén central.** Única puerta a los datos.
- **Room/SQLite (`data/local`) = trastienda.** 7 estanterías (tablas) que sobreviven aunque apagues el móvil.
- **Retrofit (`data/remote`) = camión proveedor.** Solo trae catálogo y lo deja en la trastienda. Nunca pinta directo en el escaparate.
- **`domain/model` = idioma común.** La UI solo habla este idioma.
- **`data/mapper` = traductor.** Traduce `DTO (camión) / Entity (trastienda) ⇄ dominio (tienda)`.

## 1. Arquitectura planteada (`docs/ARQUITECTURA.md`) vs real

### 1.1. Lo que pide el doc

Flujo oficial (`docs/ARQUITECTURA.md:5-17`):

```
UI (Compose) → ViewModel → Repository → Room (SQLite)
                                  └──→ Retrofit (red, solo rellena)
```

Reglas (`docs/ARQUITECTURA.md:75-83`):

- UI solo ve `domain/model`, nunca Entity ni DTO.
- Mapeo `DTO / Entity ⇄ dominio` en `data/mapper`.
- IDs usuario = UUID; catálogo conserva `id` (`músculo/slug`).
- `updatedAt` + `deleted` (borrado lógico) desde el día uno.
- Siempre `@Upsert`, nunca `@Insert(onConflict = REPLACE)`.
- `exportSchema = true` + `Migration` real por cambio de esquema.
- Catálogo y propios comparten tabla con `source` (`CATALOG`/`CUSTOM`); sync solo toca `CATALOG`.
- Sin casos de uso ni módulos extra hasta que duelan.
- Catálogo: fork por jsDelivr con **tag fijo**, nunca rama; solo URLs GIF en DB (Coil cachea).

### 1.2. Lo que hay en código (2026-10-08)

| Capa pedida | Estado | Evidencia |
|---|---|---|
| Un solo módulo `app`, `Screen → ViewModel(StateFlow) → Repository → Room` | Cumple | `feature/profile,home,exercises,routines,workout` con `Screen + ViewModel`; `TrainingApp.kt` + `core/di/{Repository,Database,Network,DataStore}Module` |
| `data/local/entity+dao`, `data/remote/dto`, `data/mapper`, `domain/model` | Cumple | 7 entidades + 7 DAOs; `ExerciseApi.kt + dto/ExerciseDto.kt`; 4 mappers; 5 modelos (`Profile, Home, Exercise, Routine, Workout`) |
| `core/di,network,datastore,ui` | Cumple | `CatalogConfig.kt` (tag fijo), `CatalogTagStore.kt` (DataStore), `ui/theme/` |
| UI solo ve dominio | Cumple | Mappers aíslan Entity/DTO |
| Retrofit solo rellena vía `CatalogSync` | Cumple | `data/repository/CatalogSync.kt:28`, distingue 404 vs sin-red |
| Hilt + DataStore + Retrofit+Gson | Cumple | Declarados y en uso (lotes A y B consumidos) |
| `MainActivity` con `NavHost` condicional `¿hay perfil? home:profile` | Cumple | Rutas `home/calendar/progress/profile + routineEdit?routineId + workout?routineId` |
| Edge-to-edge (`statusBarsPadding` / `navigationBarsPadding`) | Parcial | Aplicado en vistas hechas; pendiente verificar en emulador con gestos y 3 botones |

### 1.3. Inventario de código (61 `.kt` en `app/src/main`)

- Raíz (2): `MainActivity.kt`, `TrainingApp.kt`
- `core/di` (4): `RepositoryModule, DatabaseModule, NetworkModule, DataStoreModule`
- `core/datastore` (1): `CatalogTagStore.kt`
- `core/network` (1): `CatalogConfig.kt`
- `ui/theme` (3): `Theme, Color, Type`
- `domain/model` (5): `Profile, Home, Exercise, Routine, Workout`
- `data/local` (2): `AppDatabase.kt` (v2, `exportSchema=true`), `Converters.kt`
- `data/local/entity` (7): `Profile, Exercise, Routine, RoutineExercise, PlannedSet, WorkoutSession, SetEntry`
- `data/local/dao` (7): uno por tabla, todos `@Upsert` + `@Query ... deleted=0`
- `data/mapper` (4): `Profile, Exercise, Routine, WorkoutMapper`
- `data/remote` (2): `ExerciseApi.kt`, `dto/ExerciseDto.kt` (`CatalogResponse{count,exercises}`, camelCase)
- `data/repository` (10): 4 Fake + 4 Room + `RoomProfileRepository` + `CatalogSync`
- `feature/*`: `profile (2)`, `home (3)`, `exercises (4: PickerSheet, ExercisesViewModel, DetailScreen, DetailViewModel)`, `routines (2)`, `workout (2)`

## 2. Modelo de datos (`docs/MODELO_DE_DATOS.md`) vs real

### 2.1. Jerarquía pedida

```
Routine → RoutineExercise → PlannedSet (reps, peso, descanso por serie)
WorkoutSession → SetEntry (lo realmente hecho)
```

- **Cumple.** El descanso, peso y reps viven en la **serie planificada de ese ejercicio en ese día**, no en el ejercicio. Día A y Día B no comparten valores.

### 2.2. Campos clave

| Entidad | Regla | Estado |
|---|---|---|
| `ExerciseEntity` | `id` catálogo `músculo/slug`, propios UUID; `source CATALOG/CUSTOM`; `secondaryMuscles/instructions` JSON; `gifUrl` solo URL | Cumple (incluye `instructions` añadido en 010) |
| `RoutineEntity` | `id` UUID, `name, position, createdAt, updatedAt, deleted` | Cumple |
| `RoutineExerciseEntity` | `id` UUID, `routineId, exerciseId, position, note, updatedAt, deleted` | Cumple |
| `PlannedSetEntity` | `weightKg` con signo (+ lastre, 0 corporal, - ayuda), `loadNote` libre, `restSeconds` por serie | Cumple |
| `WorkoutSessionEntity` | `id` UUID, `routineId?`, `startedAt, endedAt, updatedAt, deleted` | Cumple |
| `SetEntryEntity` | `sessionId, exerciseId, plannedSetId?, setNumber, reps, weightKg, loadNote, restSeconds?` | Cumple |

### 2.3. Comportamiento de series (`docs/MODELO_DE_DATOS.md:81-87`)

1. Al crear N series se copian valores de la 1ª → implementado (pantalla).
2. Editar propaga solo a siguientes **no editadas a mano** (estado solo de pantalla) → implementado.
3. Día nuevo = valores por defecto, sin copiar de otros días → implementado.
4. Al iniciar sesión, prefill con **última sesión** de ese ejercicio en ese día, si no con lo planificado → implementado en `FakeWorkoutRepository` y `RoomWorkoutRepository` (spec 009 persiste la copia al iniciar).

### 2.4. Adaptación justificada

El spec 006 pedía DTO `snake_case` (`body_part, gif_url`), pero el JSON real del fork es `camelCase` (`bodyPart, secondaryMuscles, gifUrl`) + envoltorio `{"count","exercises"}` + `gifUrl` absolutas al upstream. Se adaptó en 007/010 y quedó cubierto por test. No es desvío, es corrección contra la realidad.

## 3. Lo que tenemos

- [x] Fase A 5/5 UI-first: `001` perfil, `002` home + dock 4 tabs + `NavHost`, `003` picker + `FakeExerciseRepository`, `004` editar rutina (borradores vs guardadas), `005` sesión en vivo (prefill última-vs-plan).
- [x] Fase B Room local (PR #7, mergeada): 7 entidades/DAOs `@Upsert`, repositorios Room misma firma que fakes, Hilt, DataStore tag, Retrofit+Gson vía `CatalogSync`, `CatalogMappingTest`.
- [x] 007 fork `gadetru v1.1.0` (PR #8, mergeado): 1323 entradas, picker se llena, 404 vs sin-red con `Log`, `getById suspend`, DTO `alternate` camelCase + `CatalogResponse`.
- [x] 008 feed único + descarte seguro + `position` real (PR #9, mergeado).
- [x] 009 sesión persistida (PR #10, mergeado): persiste prefill al iniciar, lee desde DB, `endedAt`/sin fantasmas, log interno sin UI de error.
- [~] 010 detalle ejercicio (rama `010-detalle-ejercicio`, **sin mergear**, 9 commits + `M specs/010-detalle-ejercicio-spec.md` sin commitear): `ExerciseDetailScreen + ViewModel`, ruta `exerciseDetail`, `instructions` en modelo/entidad/mapper/DTO, DB v2 + `MIGRATION_1_2`, re-descarga con mismo tag. Checklist 6/10 en `[x]`.

## 4. Lo que nos falta

Tomado de `README.md:148-171` (TODO vivo) + MVP (`README.md:84-96`):

### P0 — Rutinas (menú ···)
- [x] Menú `Editar / Eliminar`, `AlertDialog` confirmación, borrado lógico + `renumber` sin huecos.
- [ ] Verificado en emulador Pixel 6 API 34: menú, cancelar, eliminar, editar, rotación, gestos/3 botones.

### P1 — MVP bloqueantes
- [ ] Detalle ejercicio (MVP-2): falta verificar sin-red, rotación, edge-to-edge.
- [ ] Ejercicios propios CUSTOM (MVP-3): crear/editar desde app (`insert` en `ExerciseRepository` + formulario). `CatalogSync` ya protege `CUSTOM`.
- [ ] Historial + `home-post-sesion` (MVP-7): ruta `history`, lista sesiones, `lastDoneLabel`/racha/progreso reales (hoy fijos `Home.kt:40-47`, `RoomHomeRepository.kt:35,38-43`); `Ver mes` cableado.

### P2 — Sesión en vivo
- [ ] `Añadir Ejercicio/Serie en vivo` (reutilizar picker 003).
- [ ] Notas/`loadNote` + RPE/DROP en vivo (modelo `Workout.kt:45` lo tiene, UI no).
- [ ] Descanso avanzado: sonido/vibración, presets (hoy bloqueante + `±10s`).
- [ ] Minimizar-background real (hoy solo visual, timer en RAM).

### P3 — Deuda UX/técnica
- [ ] Avatar picker + Coil (hoy placeholder `ProfileScreen.kt:212`).
- [ ] Tema: migrar tokens a `ui/theme/` + fuentes Outfit/Jakarta/Space Grotesk.
- [ ] `material-icons-extended` (proponer línea, esperar) o mantener fallbacks.
- [ ] Catálogo `en` + reintento manual picker vacío (hoy solo `es` + auto).
- [ ] Pulidos Fase A con datos reales; actualizar `AGENTS.md` y notas snake_case en `002/006`.
- [ ] Fuera MVP (no tocar): accesorios tabla propia, gráficas, Spring/MySQL Fase 2.

## 5. Desviaciones del plan

| # | Desviación | Gravedad | Acción |
|---|---|---|---|
| 1 | `AGENTS.md:5-23,58` dice "007 en curso, pendiente Fase B" cuando `main` ya lleva 006+007+008+009 mergeados (PR #7-#10) | Media (docs, no código) | Alinear `AGENTS.md` con `README.md` |
| 2 | `material-icons-core` (49 iconos) → fallbacks `List/DateRange/Star/Person` en `HomeScreen.kt:67-82` | Baja (conocida) | Proponer `material-icons-extended` y esperar; no añadir sin permiso Gradle |
| 3 | DTO `snake_case` (spec 006) vs JSON real `camelCase` | Nula (corrección) | Ya adaptado y testeado en 007/010 |
| 4 | Fakes conviven con Rooms (código muerto Fase A) | Baja | Mantener de momento; plantear borrado cuando Room sea estable |
| 5 | Rama `010` con `M specs/010-detalle-ejercicio-spec.md` sin commitear | Baja | Commitear antes de `/verifier` |
| 6 | Muchos `[ ]` sin verificar en móvil físico / edge-to-edge / rotación / sin-red (006/007/008/009/010) | Alta | No dar specs por cerrados sin `assembleDebug + tests + emulador` |

Fuente de verdad recordada: si un spec contradice a `README.md` / `docs/` / código, avisar y seguir al código (`AGENTS.md:30`).

## 6. Sistema verificador de tests (¿tenemos?)

**Sí, pero cubre poco. Es como tener alarma solo en la puerta delantera.**

### 6.1. Lo que existe

**Unitarios JVM** (`app/src/test/`, `./gradlew testDebugUnitTest`):
- `CatalogMappingTest.kt:18-166` — 5 tests reales, sin Android/Room: `DTO→dominio→entidad`, camelCase real, envoltorio `count`, `source=CATALOG`, round-trip `instructions`.
- `ExampleUnitTest.kt` — plantilla `2+2=4`, sin valor de dominio.

**Instrumentados** (`app/src/androidTest/`, requieren emulador `emulator-5554`): `./gradlew connectedDebugAndroidTest`:
- `HomeNavTest` (smoke `HomeContent` stateless sin Hilt), `RoutineEditSmokeTest`, `WorkoutSmokeTest` con `testTag` (`profileRoot/profileSave`, `homeRoot/homeDock/routineCard:<id>`, `routineEditRoot/routineEditAddExercise/routineEditSave`, `workoutRoot/workoutFinish/restOverlay`).
- `ExampleInstrumentedTest` — plantilla.

**Puertas mínimas:**
- `./gradlew assembleDebug` OK.
- `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"` en verde.
- Skill `/verifier`: assemble + código + docs + mobile-mcp/adb en emulador Pixel 6 API 34.
- Cada spec trae `§8 Verificación + §11 Checklist` listo para `/verifier`.

### 6.2. Huecos (lo que NO protege hoy)

- Nada testea `discard()` seguro, `position/renumber`, prefill última-vs-plan, persistencia/cierre de sesión, ni borrado lógico. Si se rompe, ningún test pita.
- No hay CI que corra `testDebugUnitTest` en cada push.
- Los smoke no cubren historial, detalle, ni picker con/sin red.
- `Coil` declarado pero sin uso real en picker (foto pendiente).

### 6.3. Cómo usarlo para mantener funcionalidad

1. Antes de cada merge: `assembleDebug` + `testDebugUnitTest` en verde.
2. Tras cada spec con UI: `connectedDebugAndroidTest` en emulador Pixel 6 API 34.
3. Marcar `[x]` en specs solo con evidencia (no por intención).
4. Próxima mejora propuesta: 2-3 unitarios puros de `RoomRoutine/WorkoutRepository` (lógica sin Android) + correr tests como guardián.

---

*Generado como punto de control para revisión posterior. No reescribir specs cerrados; actualizar este fichero en la próxima revisión.*
