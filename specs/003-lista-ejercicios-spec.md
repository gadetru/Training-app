# Lista de Ejercicios – Selector Bottom-Sheet (copia imagen + GIFs reales)
Estado: aprobado
Depende de: specs/002-vista-principal-spec.md (reutiliza el `NavHost` de `MainActivity`, los tokens locales, el patrón stateful+stateless y el esquema fake-con-firma-futura; el CTA `Crear Rutina +` que aquí se cablea lo dejó como TODO el 002)
Fecha de creación: 2026-10-03
Descripción: Maquetar en Kotlin + Compose la vista `references/plantilla-lista-ejercicios` como bottom-sheet selector reutilizable de ejercicios (buscador, chips de músculo y equipamiento, filas con thumbnail GIF real vía Coil, `Añadir/Añadido`, pie `Listo (N)`), abierto desde `Crear Rutina +` de la Home, con datos en memoria de forma idéntica al fork `gadetru/ExerciseGymGifsDB`. Sin Room, sin descarga JSON, sin persistencia (Fase A).
## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-lista-ejercicios` como selector de ejercicios en memoria y verificable en móvil físico: `ModalBottomSheet` al 88vh con drag-handle, título + cerrar, buscador de texto, chips de grupo muscular y de equipamiento (MVP item 1 completo), filas con thumbnail GIF real cargado con Coil + nombre + meta + botón `Añadir/Añadido`, y pie de confirmación `Listo (N seleccionados)` que emite `onConfirm(ids)` sin crash. El dominio fake espeja campo a campo el JSON real del fork para que Fase B/C lo reutilice sin cambios de forma.
## 2. Alcance (entra / no entra)
Entra:
- Nueva feature `feature/exercises` con `ExercisePickerSheet` (stateful + contenido stateless, patrón de `HomeScreen.kt`/`ProfileScreen`) + `ExercisesViewModel` fake con `StateFlow<ExercisesUiState>` (instanciado con `viewModel()` de `lifecycle-viewmodel-compose`, ya declarado).
- Dominio en memoria `domain/model/Exercise.kt` (`Exercise`, `ExerciseSource`, `ExerciseFilter`, `ExercisesUiState`) con los 9 campos del JSON real + `source` + `updatedAt/deleted` sync-ready; la UI solo ve `domain/model`.
- Repo fake nuevo `data/repository/FakeExerciseRepository.kt` con interfaz `ExerciseRepository` de firma futura (`observeExercises(filter): Flow<List<Exercise>>`, `getById(id): Exercise?`); dataset de ~14 ejercicios con `gifUrl` reales y resolubles (ver §6).
- Filtros combinables en vivo: texto (nombre) + chips de músculo + chips de equipamiento, con contador de resultados (`N ejercicios`).
- Thumbnails con Coil `AsyncImage` (`coil-compose:2.6.0`, ya declarado en `app/build.gradle.kts:45`) + placeholder y error de color local si la carga falla.
- Selección múltiple en memoria (`Añadir` ↔ `Añadido` volt) + pie `Listo (N)` → `onConfirm(selectedIds)` TODO sin crash; cerrar/X descarta sin crash.
- Cableado: `HomeScreen.onCreate` (`Crear Rutina +`, hoy TODO) abre el sheet con estado hoisted; sin cambios Gradle (ModalBottomSheet es material3, Coil ya declarado).

No entra:
- Detalle de ejercicio, GIF grande e instrucciones (MVP item 2) — va en spec `detalle-ejercicio`.
- Crear ejercicios propios (`source=CUSTOM`, MVP item 3) — va en Fase B con Room (sin persistencia no hay dónde guardarlos).
- Descarga del catálogo: Retrofit, `ExerciseApi.kt`, DTOs, `GET api/{lang}/exercises.json`, `CATALOG_TAG`, DataStore/`CatalogPrefs`, `kotlinx.serialization` (ni declarado en `gradle/libs.versions.toml`) — todo va en Fase C catálogo.
- Persistencia Room (`ExerciseEntity`, DAOs, `AppDatabase`, `Migration`, `exportSchema`, `schemas/`), repositorio real, Hilt, DataStore de ajustes.
- Lógica de series (`weightKg` con signo, `loadNote`, `restSeconds` por serie, propagación/prefill) — intacto según `docs/MODELO_DE_DATOS.md:49-87`; va en specs de `rutina`/`editar-rutina`.
- Cambios en Gradle (`gradlew`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` raíz y `app/`): prohibidos. `coil-gif` (GIF animado) solo se propone como línea exacta a la espera de aprobación; sin él, Coil muestra el primer frame estático.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo final exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`AGENTS.md:38`, `docs/ARQUITECTURA.md:11-17,25-30`). En este spec Fase A solo se hace `UI + ViewModel fake + repo fake en memoria`, sin Room.
- Real hoy: no existe `feature/exercises` (glob `app/src` solo tiene `feature/profile` + `feature/home` + `domain/model/{Profile,Home}.kt` + `data/repository/FakeHomeRepository.kt`). Patrón a reutilizar: `HomeScreen.kt:90-112` (stateful que colecciona `uiState` + `HomeContent` stateless) y `HomeViewModel.kt:20-30` (`StateFlow` + repo fake por defecto). `MainActivity.kt:52-89` (`TrainingNav` con rutas `home/calendar/progress/profile`); el sheet vive sobre `home`, no necesita ruta nueva.
- Paquete fijado `com.mytrainingplan.app` (`app/build.gradle.kts:7,13`), `minSdk 26`, `compileSdk/targetSdk 37`, Java 11, Kotlin `2.2.10`, AGP `9.4.1`, Compose BOM `2026.02.01` (`specs/002-vista-principal-spec.md` §3). Deps ya declaradas y suficientes: `navigation-compose 2.7.7`, `coil-compose 2.6.0`, `material-icons-core`, `lifecycle-viewmodel-compose` (`app/build.gradle.kts:42-45`).
- Fuente de datos real (fork `gadetru/ExerciseGymGifsDB`, verificado 2026-10-03): API estática multilingüe `es/en`, 19 músculos, 1323 ejercicios. Esquema por ejercicio (camelCase, idéntico en ambos idiomas): `{id, slug, name, muscle, bodyPart, equipment, category, secondaryMuscles, instructions, file, gifUrl}`. Valores `equipment`: `barbell/dumbbell/cable/machine/bodyweight/band/kettlebell/smith/ez-bar/lever/other`; `bodyPart`: `arms/legs/chest/back/core/shoulders/cardio`; `category`: `strength/stretching/cardio/plyometrics`. Endpoints: `/api/<lang>/exercises.json`, `/api/<lang>/muscles.json`, `/api/<lang>/muscles/<muscle>.json`, `/api/<lang>/equipment[/<equipment>].json`, `/api/<lang>/bodyparts[/<bodyPart>].json`, `/api/<lang>/categories[/<category>].json`, `/api/<lang>/exercises/<muscle>/<slug>.json`.
- Corrección al spec 002: sus §6/§10 asumían DTOs snake_case (`body_part/gif_url`); el JSON real es camelCase (`bodyPart`, `secondaryMuscles`, `gifUrl`). Este spec fija camelCase como referencia para Fase C.
- Referencia visual: `references/plantilla-lista-ejercicios/screen.png` (copia exigida), `code.html:164-165` (bottom-sheet 88vh sobre constructor desenfocado), `code.html:182-187` (buscador, ej. `value="Pierna"`), `code.html:192-199` (chips de músculo, activo `Pierna`), `code.html:220-347` (7 filas de pierna con thumbnail + `Añadir/Añadido`), `code.html:351-355` (pie `Listo (2 ejercicios seleccionados)`). Desviación deliberada: segunda fila de chips de equipamiento (la referencia no la tiene; el MVP exige filtrar por equipamiento, `README.md` MVP item 1).
- Sets no se tocan (`docs/MODELO_DE_DATOS.md:49-87` intacto). MVP cubierto: item 1 completo en Fase A (filtro músculo + equipamiento); items 2-3 en specs futuros.
## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Mostrar `ModalBottomSheet` al ~88vh con drag-handle, título (p. ej. `Añadir ejercicios`) y botón cerrar/X, como en `code.html:164-181`.
- Mostrar buscador de texto con placeholder (p. ej. `Buscar ejercicio (ej. Sentadilla, Prensa...)`); filtrado en vivo insensible a mayúsculas/acentos sobre `name` (+ `muscle`/`equipment` como bonus si es trivial).
- Mostrar fila de chips de músculo ES (`Todos`, `Pierna`, `Pecho`, `Espalda`, `Hombros`, `Core`, …) mapeados a slugs API (`Pierna` → `quads/hamstrings/glutes/calves/abductors/adductors`, `Pecho` → `pectorals`, `Espalda` → `lats/traps/upper-back/spine`, `Hombros` → `delts`, `Core` → `abs`, …); selección única con activo naranja, como en `code.html:192-199`.
- Mostrar segunda fila de chips de equipamiento ES (`Todos`, `Barra`, `Mancuernas`, `Máquina`, `Corporal`, `Polea`, `Banda`, …) mapeados a `barbell/dumbbell/machine(bodyweight→Corporal)/cable/band/…`; selección única.
- Mostrar contador de resultados (`N ejercicios`) + lista de filas: thumbnail Coil 56dp (GIF real, placeholder/error local), nombre, meta `músculo · equipamiento`, botón `Añadir` → `Añadido` (texto volt, fondo tenue) con toggle por fila, como en `code.html:227-347`.
- Mostrar pie fijo `Listo (N seleccionados)` → `onConfirm(selectedIds: List<String>)` TODO sin crash; deshabilitado o con `N=0` si no hay selección. Cerrar/X descarta sin emitir.
- Abrir el sheet desde `Crear Rutina +` de la Home (`HomeScreen.onCreate`); el resto de la Home no cambia.

No-funcionales:
- Sin red funciona la lista y los filtros (todo en memoria); solo los GIFs requieren red, con placeholder y error locales si falla la carga.
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, radios y espaciados de `DESIGN.md`).
- Sin jank en scroll (lista perezosa), hit targets >= 48dp, bottom padding para que el pie no tape contenido, prueba en móvil físico sin emulador.
- Dominio y repo fake listos para Fase B/C: `id` `músculo/slug`, `updatedAt + deleted` (borrado lógico sync-ready) y `source` (`CATALOG`/`CUSTOM`) desde el día uno; futuro `@Upsert`, nunca `@Insert(REPLACE)`.
## 5. Criterios de aceptación verificables
- En móvil físico el sheet es copia de `screen.png`: header con drag-handle y cerrar, buscador, chips de músculo, filas con thumbnail + `Añadir`, pie `Listo (N)`.
- Escribir en el buscador filtra la lista en vivo (p. ej. `prensa` muestra solo coincidencias) sin crash.
- Cambiar el chip de músculo filtra por ese grupo (p. ej. `Pecho` oculta los de pierna); `Todos` restaura.
- Cambiar el chip de equipamiento combina con lo anterior (p. ej. `Pierna` + `Máquina` muestra solo prensa/extensiones).
- Pulsar `Añadir` en una fila la marca `Añadido` y suma al contador del pie; pulsar de nuevo la desmarca.
- Pulsar `Listo (N)` emite `onConfirm` con los ids seleccionados sin crash ni navegación rota.
- Pulsar cerrar/X cierra el sheet sin emitir y sin crash.
- Los GIFs cargan vía Coil; con modo avión (tras primera carga) o URL rota se ve el placeholder/error local, sin crash.
- Rotar el dispositivo conserva query, filtros y selección en sesión (`StateFlow` en `ViewModel`).
- Paquete `com.mytrainingplan.app`, sin `.debug`.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `domain/model/Exercise.kt` nuevo: `Exercise(id: String /* músculo/slug */, slug, name, muscle, bodyPart, equipment, category, secondaryMuscles: List<String>, gifUrl: String, source: ExerciseSource = CATALOG, updatedAt = 0L, deleted = false)` + `enum ExerciseSource { CATALOG, CUSTOM }` + `ExerciseFilter(query: String = "", muscles: Set<String> = emptySet(), equipment: Set<String> = emptySet())` + `ExercisesUiState(query, selectedMuscles, selectedEquipment, results, selectedIds, isLoading = false)`. UI solo ve dominio.
- `data/repository/FakeExerciseRepository.kt` nuevo + interfaz `ExerciseRepository { fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>>; fun getById(id: String): Exercise? }` (misma firma que el futuro repositorio real). Dataset fake de ~14 ejercicios con `gifUrl` reales y resolubles del upstream `JahelCuadrado/ExerciseGymGifsDB@v1.1.0` (el fork `gadetru` aún no tiene tags en jsDelivr —ver Preguntas abiertas—; al taggear el fork se sustituye el prefijo por `https://cdn.jsdelivr.net/gh/gadetru/ExerciseGymGifsDB@<tag>` sin cambiar forma): 7 de pierna de la referencia (`Sentadilla con barra`, `Prensa de piernas`, `Extensión de cuádriceps`, `Curl femoral`, `Búlgara con mancuernas`, `Peso muerto rumano`, `Elevación de talones`) + `Press banca` (pectorals), `Dominadas` (lats), `Curl con barra` (`biceps/barbell-curl`), `Press militar` (delts), `Plancha` (abs) y 1-2 extra para que los filtros de equipamiento demuestren (`barbell/dumbbell/machine/bodyweight/cable`).
- `feature/exercises/ExercisesViewModel.kt` nuevo (fake Fase A): `StateFlow<ExercisesUiState>`; eventos `onQueryChange`, `onMuscleSelected(slug|null)`, `onEquipmentSelected(equipment|null)`, `onToggleSelected(id)`, `onClearSelection()`; filtrado en memoria (texto normalizado sin acentos + intersección músculo/equipamiento); sin Hilt (instanciación directa o `viewModel()`).
- `feature/exercises/ExercisePickerSheet.kt` nuevo: `ExercisePickerSheet(viewModel, onConfirm, onDismiss)` stateful + `ExercisePickerContent` stateless; `ModalBottomSheet` (~88vh) con `SheetHeader` (drag-handle + título + cerrar), `SearchBar`, `MuscleChipsRow`, `EquipmentChipsRow`, `ResultsCount`, `ExerciseRow` (`AsyncImage` Coil 56dp + nombre + meta + botón `Añadir/Añadido`), `ConfirmFooter` (`Listo (N)`). Tokens locales como en 001/002; tipografías de sistema; iconos `material-icons-core` (verificar `Search` en el sources.jar como se hizo en `HomeScreen.kt:67-82`; fallback listo si falta).
- `HomeScreen.kt`: `onCreate` abre el sheet (estado hoisted en `HomeScreen` o en el llamante); `onConfirm` = TODO visible (el constructor de rutina que consumirá los ids llega en el spec `editar-rutina`).
- Room/DAO/Migration/mapper/DTOs/`ExerciseApi`/catálogo/`CATALOG_TAG`: NO se tocan. Sets (`weightKg` signo, `loadNote`, `restSeconds`, propagación/prefill): NO se tocan.
- Qué NO se toca (gotchas `AGENTS.md`): no cambiar `applicationId`, no `applicationIdSuffix ".debug"`, misma keystore fuera del repo + bump `versionCode` solo en release, `gradle-wrapper.jar` ausente (abrir en Android Studio primero), sin emulador, `dynamicColor=false` para fidelidad.
## 7. Plan de tareas
1. Dominio en memoria `domain/model/Exercise.kt` (`Exercise` espejo del JSON real + `ExerciseSource` + `ExerciseFilter` + `ExercisesUiState`).
2. Crear `data/repository/FakeExerciseRepository.kt` + interfaz `ExerciseRepository` de firma futura, con dataset ~14 ejercicios y `gifUrl` reales resolubles.
3. Crear `feature/exercises/ExercisesViewModel.kt` fake con `StateFlow<ExercisesUiState>` (query + filtros + selección, filtrado en memoria).
4. Crear `feature/exercises/ExercisePickerSheet.kt` (sheet + header + search + 2 filas de chips + filas Coil + pie `Listo (N)`) como copia Compose de `screen.png` / `code.html`.
5. Cablear apertura desde `Crear Rutina +` de la Home (`onCreate`); `onConfirm`/`onDismiss` como TODO visibles sin crash.
6. Verificar iconos (`Search` y los usados) contra el sources.jar de `material-icons-core`; aplicar fallbacks si falta alguno (precedente `HomeScreen.kt:67-82`).
7. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico (copia visual, filtros combinados, selección, confirmación, rotación, GIFs con y sin red).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo; el fake usa la forma del JSON pero sin DTOs ni Retrofit).
- Prueba en móvil físico (sin emulador): comparar con `screen.png`; abrir desde `Crear Rutina +`; filtrar por texto/músculo/equipamiento combinados; toggle `Añadir/Añadido`; `Listo (N)` sin crash; cerrar sin crash; GIFs visibles con red y placeholder/error sin red; rotar conserva query + filtros + selección en sesión.
## 9. Riesgos / No romper
- El fork `gadetru/ExerciseGymGifsDB` aún no tiene tags en jsDelivr (verificado: `tags: {}`); hasta taggearlo, las `gifUrl` del fake apuntan al upstream `@v1.1.0`. Al crear el tag, sustituir solo el prefijo de las URLs (la forma no cambia).
- Sin `coil-gif` (no aprobado aún), Coil muestra el primer frame estático del GIF; no interpretar eso como bug en la verificación.
- En Fase B/C: solo `@Upsert`, nunca `@Insert(REPLACE)`; `Migration` real + `exportSchema=true` con schemas en `app/schemas/`; `fallbackToDestructiveMigration()` solo en desarrollo; update de catálogo solo filas `source=CATALOG`.
- No cambiar `applicationId/com.mytrainingplan.app` ni añadir `.debug`; no mover keystore al repo; bump `versionCode` solo en release.
- UI solo ve `domain/model`; mapping en `data/mapper` cuando exista; no exponer Entity/DTO a la UI ni en el fake.
- No añadir dependencias Gradle en este spec (Coil y material3 ya están); si hiciera falta otra, proponer línea exacta y esperar (restricción `AGENTS.md:19-27`).
- Sheet modal: dejar padding inferior para no tapar el pie `Listo (N)`; hit targets >= 48dp.
- `MainActivity` sigue siendo el único punto de entrada; no romper el flujo `TrainingNav` de 002 (el sheet vive sobre `home`, sin ruta nueva).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- `detalle-ejercicio`: tap en la fila abre detalle fake (GIF grande + instrucciones `List<String>` del JSON real + secundarios); base MVP item 2.
- `rutina` (sesión en vivo) desde `references/plantilla-rutina`: acordeón de ejercicios + tabla de series + cronómetro de descanso + `Finalizar y Guardar` (ver `plantilla-rutina/code.html:139-468`).
- `editar-rutina` (constructor) desde `references/plantilla-editar-rutina`: título/duración/tags + chips de músculo + matriz de series con tipos (calentamiento/normal/al fallo) + footer fijo; consume los ids de `onConfirm` de este spec; aplica reglas de series `docs/MODELO_DE_DATOS.md:81-87` (copiar 1ª serie, propagación a no-editadas, sin copiar entre días, prefill última sesión).
- Fase B Room ejercicios: `ExerciseEntity` (misma forma + `source CATALOG/CUSTOM`) + `ExerciseDao @Upsert/observe/softDelete` + `AppDatabase Migration` + repositorio real + ejercicios propios (MVP item 3).
- Fase C catálogo: `ExerciseApi.kt` + DTOs camelCase (corregir asunción snake_case de 002) + `GET api/{lang}/exercises.json` + `CATALOG_TAG` + DataStore + `CatalogMappingTest` + Coil GIFs solo URL.
- `coil-gif` (`io.coil-kt:coil-gif:2.6.0`): proponer línea exacta y esperar aprobación Gradle para GIF animado real.
- Taggear el fork (`v1.1.0` + rebuild `api/` con `API_BASE_URL` del fork) para que `cdn.jsdelivr.net/gh/gadetru/ExerciseGymGifsDB@<tag>` resuelva; entonces sustituir el prefijo de las `gifUrl`.
- Tema: migrar tokens locales a `ui/theme/` y tipografías Outfit/Plus Jakarta Sans/Space Grotesk (pendiente desde 001/002).
## Preguntas abiertas
- ¿Las `gifUrl` del fake usan upstream `@v1.1.0` hasta taggear el fork (propuesta), o se taggea el fork primero y el spec ya apunta a `gh/gadetru`? (Bloquea solo el prefijo de las URLs, no la forma.)
- ¿Se aprueba añadir `io.coil-kt:coil-gif:2.6.0` para GIF animado, o primer frame estático en Fase A?
- ¿Los chips de músculo ES deben cubrir los 19 músculos API o basta el subconjunto principal (Pierna/Pecho/Espalda/Hombros/Core/Brazo)? Propuesta: subconjunto principal + `Todos`.
- ¿El sheet debe pre-seleccionar algo al abrir (p. ej. chip `Pierna` como en `code.html:187,194`) o abrir sin filtros? Propuesta: sin filtros (estado neutro reutilizable).
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Sheet copia `screen.png` verificada en móvil físico (header, search, chips, filas, pie)
- [ ] Buscador filtra en vivo sin crash
- [ ] Chip de músculo filtra por ese grupo (`Todos` restaura)
- [ ] Chip de equipamiento combina con músculo y texto
- [ ] `Añadir`/`Añadido` alterna por fila y actualiza el contador del pie
- [ ] `Listo (N)` emite `onConfirm(ids)` sin crash
- [ ] Cerrar/X descarta sin emitir y sin crash
- [ ] GIFs Coil visibles con red; placeholder/error local sin red, sin crash
- [ ] Rotación conserva query, filtros y selección en sesión
- [ ] Paquete `com.mytrainingplan.app`, sin `.debug`
- [ ] `./gradlew assembleDebug` OK
- [ ] Prueba en móvil físico OK
