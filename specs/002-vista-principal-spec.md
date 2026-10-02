# Vista Principal – Home Mis Rutinas (copia imagen + lote A)
Estado: implementado
Depende de: specs/001-plantilla-usuario-spec.md (reutiliza dominio Profile/saludo `Hola, {nombre}`, patrón visual `ProfileScreen` stateful+stateless y tokens; introduce el `NavHost` profile/home y ViewModels fake que 001 dejó como futuro explícito)
Fecha de creación: 2026-10-02
Descripción: Maquetar en Kotlin + Compose la vista `references/plantilla-vista-principal` como pantalla Home (header `Hola, Atleta`, 3 routine cards, dashboard Calendario+Progreso, bottom nav 4 tabs) en memoria UI-first (Fase A), incluyendo el lote A: `NavHost` condicional `¿hay perfil? home:profile`, ViewModels fake con `StateFlow` + repo fake misma firma, y `material-icons`.
## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-vista-principal` como pantalla Home de la app, con cabecera de atleta, feed de rutinas, widgets de calendario/progreso y dock inferior de navegación, todo en memoria y verificable en móvil físico. Incluir el lote A pendiente del spec 001: navegación `profile/home`, ViewModels fake con `StateFlow` y repo fake con la firma futura, sin Room ni red (Fase A maquetación).
## 2. Alcance (entra / no entra)
Entra:
- Nueva feature `feature/home` con `HomeScreen` (stateful + `HomeContent` stateless) que replica `screen.png` y `code.html`: TopAppBar (avatar + `Hola, {firstName}` + pill `Listo para entrenar` + CTA `Crear Rutina +`), sección `Tus Rutinas` con pill `3 activas` + botón sort, 3 routine cards (accent bar lateral, título, micro-chips, meta duración/ejercicios/última vez, bottom-bar con nota/PR + botón `Iniciar`, botón `···`), dashboard 2 columnas (Calendario heatmap L–D con `Racha 4 días` + `Ver mes`, Progreso con `3/4 objetivo`, barra 75%, `185 min`, `+12% vol`), bottom nav flotante con 4 tabs (`Rutinas`, `Calendario`, `Progreso`, `Perfil`).
- Dominio en memoria `domain/model/Home.kt` (`RoutineSummary`, `WeeklyProgress`) + `data/repository/FakeHomeRepository` (misma firma que el futuro repositorio real); la UI solo ve `domain/model`.
- Lote A: `NavHost` condicional `¿hay perfil? home:profile` en `MainActivity` (reutiliza `ProfileScreen` del spec 001), `HomeViewModel` (+ `ProfileViewModel` fake si hace falta para el saludo) con `StateFlow`, iconos con `material-icons`. Las dependencias ya están declaradas (`app/build.gradle.kts:42-45`); no se toca Gradle.
- Bottom nav shell: tab `Rutinas` funcional fake; `Calendario`/`Progreso`/`Perfil` como pantallas placeholder (el tab Perfil reutiliza `ProfileScreen`).
- Acciones (`Iniciar`, `···`, `Crear Rutina +`, sort, `Ver mes`) como callbacks `TODO` visibles sin crash ni navegación real.
- Saludo conectado al Profile en memoria (`Hola, {firstName}`, por defecto `Hola, Carlos`); resto de métricas fijas fake (`Racha 4 días`, `3/4 sesiones`, `185 min`, `+12% vol`) con `TODO` a Fase B.

No entra:
- Persistencia Room (`RoutineEntity/RoutineExerciseEntity/PlannedSetEntity/WorkoutSessionEntity/SetEntryEntity`, DAOs, `AppDatabase`, `Migration`, `exportSchema`, `schemas/`), repositorio real, Hilt (`TrainingApp.kt`, módulos DI), DataStore (`CATALOG_TAG`, `CatalogPrefs`), Retrofit (`ExerciseApi.kt`, DTOs snake_case `body_part/gif_url`, `GET api/{lang}/exercises.json`), `CatalogMappingTest`.
- Lógica de series (`weightKg` con signo, `loadNote`, `restSeconds` por serie, propagación a siguientes no editadas, prefill última sesión) — intacto según `docs/MODELO_DE_DATOS.md:49-87`; eso va en specs de `rutina`/`editar-rutina`.
- Catálogo de ejercicios (filtro por músculo/equipamiento, detalle GIF, ejercicios propios) — va en spec `lista-ejercicios`.
- Picker real de foto + Coil (`PickVisualMedia`, URI persistente) salvo reutilizar el placeholder ya existente; avatar del header con iniciales como en 001.
- Sincronización Fase 2, cuentas de usuario, gráficas, cronómetro, accesorios reutilizables (fuera del MVP según `README.md:65`).
- Cambios en Gradle (`gradlew`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` raíz y `app/`): prohibidos salvo que ya están las deps del lote A; este spec no añade dependencias.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo final exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`AGENTS.md:30`, `docs/ARQUITECTURA.md:11-17,25-30`). En este spec Fase A solo se hace `UI + ViewModel fake + repo fake en memoria`, sin Room.
- Real hoy: `MainActivity.kt:21-23` muestra `ProfileScreen()` directo sin `NavHost`; no hay `TrainingApp.kt`/Hilt, ni Room/DAO/Repository/mapper, ni `feature/home|routines|exercises` (glob `app/src` solo tiene `feature/profile` + `domain/model/Profile.kt` + `ui/theme/`). Patrón a reutilizar: `ProfileScreen.kt:60-108` (stateful con `remember` + `ProfileContent` stateless) y `Profile.kt:43-48` (`firstName/greeting`).
- Paquete fijado `com.mytrainingplan.app` (`app/build.gradle.kts:7,13`), `minSdk 26`, `compileSdk/targetSdk 37`, Java 11, Kotlin `2.2.10`, AGP `9.4.1`, Compose BOM `2026.02.01`. Deps lote A ya declaradas pero sin usar: `app/build.gradle.kts:42-45` (`lifecycle-viewmodel-compose`, `navigation-compose`, `coil-compose`, `material-icons-core`), `gradle/libs.versions.toml:9-12` (`navigationCompose 2.7.7`, `coil 2.6.0`).
- Referencia: `references/plantilla-vista-principal/screen.png` (copia visual exigida), `references/plantilla-vista-principal/code.html:119-141` (header avatar+saludo+CTA), `code.html:156-287` (3 cards con accent bar/chips/meta/bottom-bar), `code.html:290-375` (dashboard Calendario+Progreso), `code.html:381-406` (bottom nav 4 tabs, activa `Rutinas`), `references/plantilla-vista-principal/DESIGN.md:146-155` (tokens `#FF5E00`, `#CCFF00`, `#111316`, Dark Tactical Modernism) y `DESIGN.md:202-226` (routine cards, widgets, dock flotante, hit targets 48px).
- Catálogo no se toca y sets no se tocan (`docs/MODELO_DE_DATOS.md:49-87` intacto). MVP cubierto parcial: ver rutinas y punto de entrada a sesión (sin registrar aún) (`README.md:55-64`).
## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Mostrar TopAppBar sticky con avatar (iniciales, borde táctico, punto verde), `Hola, {firstName}` (del Profile en memoria), pill `Listo para entrenar` y CTA `Crear Rutina +` con callback TODO sin crash, como en `code.html:119-141`.
- Mostrar sección `Tus Rutinas` con pill `3 activas` y botón sort (TODO sin crash).
- Mostrar 3 routine cards fake en memoria (`Torso - Fuerza & Hipertrofia`: Pecho/Espalda/Hombros, 45 min, 6 ejercicios, Hace 2 días, `Récord en Press Banca`; `Pierna & Core Explosivo`: Cuádriceps/Isquios/Abdomen, 55 min, 7 ejercicios, Ayer, `Enfoque: Sentadilla profunda`; `Full Body Funcional`: Fuerza/Cardio, 40 min, 5 ejercicios, `Recuperación activa`), cada una con accent bar de color (`#FF5E00`/`#CCFF00`/`#00E5FF`), botón `···` (TODO) y botón `Iniciar` (callback `onStart(routineId)` TODO sin crash), como en `code.html:156-287`.
- Mostrar dashboard 2 columnas: Calendario (heatmap L–D con 4 días ✓ L–J, `Racha 4 días`, `Ver mes` TODO) y Progreso (`Sesiones 3/4 objetivo`, barra 75%, `Tiempo 185 min`, `Carga +12% vol`), como en `code.html:290-375`.
- Mostrar dock inferior flotante con 4 tabs (`fitness_center`/`calendar_today`/`insights`/`person` vía `material-icons`); tab activo `Rutinas` en naranja con micro-dot; los otros 3 muestran placeholder sin crash.
- Navegación: `NavHost` con rutas `profile` y `home` (+ placeholders de tabs); arranque condicional `¿hay perfil? home:profile` (criterio simple en memoria: nombre no vacío, como en 001).
- Estado vía `HomeViewModel` fake (`StateFlow<HomeUiState>`) alimentado por repo fake; mantener patrón stateful+stateless de 001.

No-funcionales:
- Sin red funciona (todo local en memoria, sin SQLite aún).
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, radios y espaciados `DESIGN.md:119-194`).
- Sin jank en scroll, hit targets >= 48dp, bottom padding 80–96dp para que el contenido no quede bajo el dock, prueba en móvil físico sin emulador.
- Dominio y repo fake listos para Fase B: entidades futuras llevarán `id` UUID, `updatedAt + deleted` (borrado lógico sync-ready) y `@Upsert` (nunca `@Insert(REPLACE)`) desde el día uno.
## 5. Criterios de aceptación verificables
- En móvil físico la Home es copia de `screen.png`: header (avatar, saludo, CTA), sección `Tus Rutinas` con 3 cards en orden y colores, dashboard 2 columnas y dock inferior coinciden en orden, color y jerarquía.
- El saludo muestra `Hola, {firstName}` del Profile en memoria (p. ej. `Hola, Carlos` con el defecto de 001) en vez del literal `Hola, Atleta`.
- Pulsar `Iniciar` en cualquier card emite `onStart(routineId)` sin crash ni navegación rota.
- Pulsar `···`, `Crear Rutina +`, sort y `Ver mes` son TODO visibles sin crash.
- Cambiar de tab en el dock muestra la pantalla correspondiente (Rutinas funcional; las otras 3 placeholder) y el tab activo se tiñe de naranja con micro-dot.
- El arranque condicional funciona: con perfil en memoria arranca en `home`; (en este spec el perfil siempre existe por el defecto de 001; el caso `profile` se verifica navegando al tab Perfil).
- Rotar el dispositivo conserva la lista y el tab activo en sesión (`StateFlow` en `ViewModel` fake).
- Paquete `com.mytrainingplan.app`, sin `.debug`.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/home/HomeScreen.kt` nueva: `HomeScreen(onStart/onCreate/onOptions)` stateful (colecciona `HomeViewModel.uiState`) + `HomeContent` stateless; secciones `TopBar`, `RoutineFeed` (`RoutineCard`: accent bar + chips + meta + bottom-bar), `DashboardRow` (`CalendarWidget`, `ProgressWidget`), `BottomDock`. Tokens locales como en 001 (`ProfileScreen.kt:48-57`) extendidos con cyan `#00E5FF`; tipografías de sistema.
- `feature/home/HomeViewModel.kt` nuevo (fake Fase A): `StateFlow<HomeUiState>` con 3 rutinas + progreso fijos; sin Hilt todavía (instanciación directa o `viewModel()` de `lifecycle-viewmodel-compose`); `ProfileViewModel` fake mínimo solo si hace falta para el saludo.
- Dominio nuevo `domain/model/Home.kt` (`RoutineSummary`: `id`, `title`, `tags`, `durationMin`, `exerciseCount`, `lastDoneLabel`, `accent: AccentColor`, `footNote`, `footKind`; `WeeklyProgress`: `streakDays`, `sessionsDone/Goal`, `minutes`, `volumeDeltaPct`, `weekChecks: Boolean[7]`); UI solo ve dominio.
- Repo fake nuevo `data/repository/FakeHomeRepository.kt` (misma firma que el futuro `HomeRepository`: `observeHome(): Flow<HomeUiState>` / `getHome()`); sin Room, sin Hilt, sin red.
- `MainActivity.kt:20-24`: envolver en `NavHost` (`profile` → `ProfileScreen`, `home` → `HomeScreen`, + placeholders tabs); `startDestination` condicional en memoria. Sin `TrainingApp.kt`/Hilt en este spec.
- Room/DAO/Migration/mapper/DTOs/catálogo: NO se tocan. Sets (`weightKg` signo, `loadNote`, `restSeconds`, propagación/prefill pantalla): NO se tocan.
- Qué NO se toca (gotchas `AGENTS.md`): no cambiar `applicationId`, no `applicationIdSuffix ".debug"`, misma keystore fuera del repo + bump `versionCode` solo en release, `gradle-wrapper.jar` ausente (abrir en Android Studio primero), sin emulador, `dynamicColor=false` en `MainActivity` para fidelidad.
## 7. Plan de tareas
1. Dominio en memoria `domain/model/Home.kt` (`RoutineSummary`, `WeeklyProgress`, `AccentColor`) + repo fake `data/repository/FakeHomeRepository.kt` con la firma futura.
2. Crear `feature/home/HomeViewModel.kt` fake con `StateFlow<HomeUiState>` (3 rutinas + progreso fijos de la referencia).
3. Crear `feature/home/HomeScreen.kt` (`HomeScreen` + `HomeContent` + `RoutineCard` + `CalendarWidget` + `ProgressWidget` + `BottomDock`) como copia Compose de `screen.png` / `code.html`.
4. Crear placeholders de tabs (`Calendario`, `Progreso`) reutilizando `ProfileScreen` para el tab `Perfil`.
5. Cablear `NavHost` condicional `¿hay perfil? home:profile` en `MainActivity.kt` (solo `navigation-compose` ya declarado; sin cambios Gradle).
6. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico (copia visual, clicks sin crash, tabs, rotación).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo).
- Prueba en móvil físico (sin emulador): comparar con `screen.png` (header, 3 cards, dashboard, dock); saludo con nombre del perfil; pulsar `Iniciar`/`···`/`Crear Rutina +`/sort/`Ver mes` sin crash; navegar los 4 tabs con activo naranja; rotar conserva estado en sesión.
## 9. Riesgos / No romper
- En Fase B: solo `@Upsert`, nunca `@Insert(REPLACE)`; `Migration` real + `exportSchema=true` con schemas en `app/schemas/`; `fallbackToDestructiveMigration()` solo en desarrollo.
- No cambiar `applicationId/com.mytrainingplan.app` ni añadir `.debug`; no mover keystore al repo; bump `versionCode` solo en release.
- UI solo ve `domain/model`; mapping en `data/mapper` cuando exista; no exponer Entity/DTO a la UI ni en el fake.
- No añadir dependencias Gradle en este spec (las del lote A ya están); si hiciera falta otra, proponer línea exacta y esperar (restricción `AGENTS.md:12-19`).
- Dock flotante: dejar bottom padding 80–96dp para no tapar contenido; hit targets >= 48dp.
- `MainActivity` sigue siendo el único punto de entrada; no romper el flujo de `ProfileScreen` de 001 (el tab Perfil la reutiliza).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Fase B Room rutinas: `RoutineEntity/RoutineExerciseEntity/PlannedSetEntity + DAOs @Upsert + AppDatabase Migration + repositorio real` (base: `docs/MODELO_DE_DATOS.md:39-58`).
- Fase B sesiones/historial: `WorkoutSessionEntity/SetEntryEntity` + racha/progreso reales desde sesiones en vez de fijos (`code.html:299,353-373`).
- `lista-ejercicios`: catálogo (`CATALOG_TAG` + DataStore, `GET api/{lang}/exercises.json`, DTOs snake_case, `CatalogMappingTest`, Coil GIFs solo URL) + filtros por músculo/equipamiento + detalle + propios (`source CATALOG/CUSTOM`).
- `rutina` y `editar-rutina` desde `references/plantilla-rutina` y `references/plantilla-editar-rutina`: series con `weightKg` signo/`loadNote`/`restSeconds` por serie y reglas de propagación/prefill de pantalla (`docs/MODELO_DE_DATOS.md:81-87`).
- Picker real + Coil (`PickVisualMedia`, URI persistente) para avatar del header (placeholder con iniciales en este spec, como en `ProfileScreen.kt:209-234`).
- Hilt (`TrainingApp.kt` + módulos DI) y DataStore de ajustes/valores por defecto de series.
- Tema: migrar tokens locales a `ui/theme/Color.kt`+`Theme.kt` (hoy solo plantilla morada `Theme.kt:14-34`) y tipografías Outfit/Plus Jakarta Sans/Space Grotesk (`DESIGN.md:51-118`).
## Preguntas abiertas
- ¿Criterio definitivo de `¿hay perfil?` para el `startDestination` en Fase B (perfil en Room vs flag DataStore)? Propuesta: existe fila de perfil no borrada en Room.
- ¿Los tabs `Calendario`/`Progreso` serán destinos `feature/history` + dashboard futuro o una sola `feature/home` con subpantallas? Propuesta: `history` separado según `docs/ARQUITECTURA.md:58-61`, placeholders aquí.
- ¿El botón `Iniciar` debe crear la `WorkoutSession` ya en el siguiente spec de `workout`, o sigue fake hasta Fase B? Propuesta: fake hasta el spec de sesión en vivo.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Home copia `screen.png` verificada en móvil físico (header, 3 cards, dashboard, dock)
- [x] Saludo muestra `Hola, {firstName}` del Profile en memoria
- [x] `Iniciar` emite `onStart(routineId)` sin crash
- [x] `···`, `Crear Rutina +`, sort y `Ver mes` visibles sin crash (TODO)
- [x] Los 4 tabs navegan (Rutinas funcional, otros placeholder) con activo naranja + micro-dot
- [x] Arranque condicional `¿hay perfil? home:profile` funciona
- [x] Rotación conserva lista y tab activo en sesión
- [x] Paquete `com.mytrainingplan.app`, sin `.debug`
- [x] `./gradlew assembleDebug` OK
- [x] Prueba en móvil físico OK
