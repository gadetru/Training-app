# Training App (nombre provisional)

App Android para planificar rutinas y registrar entrenos de fuerza. Funciona sin conexión, no lleva anuncios y está pensada para controlar al detalle cada serie.

> **Estado:** Fase A hecha (5/5 vistas UI-first en memoria) + Fase B Room local (PR #7) mergeada en `main`,
> catálogo (PR #8, fork `gadetru` tag `v1.1.0`) y specs `007` a `022` todos mergeados. Historial: rutinas-feed
> (#9), sesión persistida (#10), detalle-ejercicio + `MIGRATION_1_2` (#13), blindaje visual + tests espejo +
> autofix (#14/#15/#16, puerta CI `blindaje.yml` con `setup-android@v4`), buscador-GIF + `coil-gif` (#17),
> sesión-vivo-fixes (#18), perfil-enfoque (#19), calendario-progreso DB v3 (PR #22), edición en sesión (PR #23),
> picker a alto completo (PR #24), barra auto-oculta (PR #25), sesión colapsada (PR #26), ceros numéricos
> (PR #27), sustituir ejercicio (PR #28) y sesión en marcha DB v4 (PR #30).
> En rama `023-duracion-rutina`, verificado y **pendiente de merge**: duración vacía/aproximada/real,
> DB v5 + `MIGRATION_4_5`. Siguiente spec: `024-logo-launcher` (Borrador).
> Puerta de merge: `assembleDebug` + `testDebugUnitTest` en cada PR/push a `main`.
> Este README y `docs/` fijan las decisiones de partida; `specs/` recoge lo ya implementado.

## Principios

- **Offline-first:** todo lo que ve y guarda el usuario vive en SQLite en el móvil. La red es un extra, no una dependencia.
- **Tecnología gratuita y open source** 
- **Plan por serie:** cada serie de un ejercicio tiene sus propias repeticiones, peso y descanso, de forma independiente en cada día de entrenamiento.
- **Lastre y gomas:** el peso admite signo (positivo = lastre, negativo = ayuda) y una nota libre (por ejemplo, "goma amarilla").
- **Edge-to-edge:** la app dibuja a pantalla completa por debajo de las barras del sistema (`enableEdgeToEdge`), así que toda cabecera lleva `statusBarsPadding()` y todo dock/CTA inferior `navigationBarsPadding()` (verificado en emulador Pixel 6 API 34); prohibido compensar con dp fijos extra.

## Stack y herramientas

### App Android (Fase 1)

| Pieza | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose |
| Navegación | Navigation Compose |
| Base de datos local | Room (sobre SQLite) |
| Inyección de dependencias | Hilt |
| Red | Retrofit + Gson (`converter-gson`, solo rellena Room vía `CatalogSync`) |
| Imágenes y GIFs | Coil + `coil-gif` (lote D: `ImageLoaderFactory` global en `TrainingApp` con `GifDecoder`; hero animado en ficha, thumbs quietos en lista) |
| Ajustes | DataStore |
| Tareas en segundo plano | WorkManager (más adelante) |
| Build | Gradle (Kotlin DSL + catálogo de versiones) |

### Catálogo de ejercicios

- Fork propio del repositorio **ExerciseGymGifsDB** (`gadetru`, tag fijo **`v1.1.0`**, verificado):
  API estática (JSON + GIFs) servida por el CDN jsDelivr.
- URL base: `https://cdn.jsdelivr.net/gh/gadetru/ExerciseGymGifsDB@v1.1.0`
- Siempre se apunta a un **tag fijo**, nunca a una rama.
- El `exercises.json` real es un objeto `{"count", "exercises"}` con claves camelCase
  (`bodyPart`, `secondaryMuscles`, `gifUrl` absolutas al upstream); la app solo usa `es`.
- Solo se tocan filas `source=CATALOG` (vía `upsert`); los propios (`CUSTOM`) nunca se pisan.

### Backend (Fase 2)

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Java (OpenJDK) + Spring Boot |
| Acceso a datos | Spring Data JPA |
| Base de datos | MySQL |
| Migraciones | Flyway |
| Documentación de la API | springdoc-openapi |
| Seguridad | Spring Security + JWT |

### Herramientas de desarrollo

- Android Studio + SDK + **emulador Pixel 6 API 34** (`emulator-5554`).
  El usuario crea/enciende el AVD; el agente lo consume vía `adb` + `connectedDebugAndroidTest`.
- Git y GitHub.
- Gradle Wrapper (`gradlew`), que fija la versión de Gradle del proyecto.

## Estado actual (Fases A + B, 007–022 mergeados, 023 pendiente de merge)

Fase A: cada pantalla es `Screen` (stateful + stateless) + `ViewModel` con `StateFlow` + repositorio fake
con la misma firma que el real. Fase B (mergeada): los fakes conviven con repositorios Room con la misma
firma; Retrofit solo rellena la DB. La UI solo ve `domain/model`. Spec 011: toda la UI usa botes con nombre
(`ui/theme/Color, Dimensions, Theme, Type`) y los tests viven en espejo por paquete.

| Vista / pieza | Estado |
|---|---|
| Perfil de atleta (`feature/profile`, `domain/model/Profile`) | Hecha (`specs/001-plantilla-usuario-spec.md`) |
| Home Mis Rutinas (`feature/home`, `domain/model/Home.kt`, `FakeHomeRepository`) + `NavHost` condicional + dock de 4 tabs | Hecha y verificada (`specs/002-vista-principal-spec.md`) |
| Lista de ejercicios (`ExercisePickerSheet` + `FakeExerciseRepository`) | Hecha (`specs/003`) |
| Editar rutina (`feature/routines`, borradores separados de guardadas) | Hecha (`specs/004`) |
| Sesión en vivo (`feature/workout`, prefill última-vs-plan) | Hecha (`specs/005`, verificada en emulador y en físico) |
| Room local (entidades + DAOs `@Upsert`, Hilt, DataStore, Retrofit + Gson, `CatalogSync`, `CatalogMappingTest`) | Hecha y mergeada (`specs/006-fase-b-room-local-spec.md`, PR #7) |
| Catálogo desde el fork + `getById` async | Hecha y mergeada (PR #8, `specs/007-catalogo-fork-spec.md`, tag `v1.1.0`, 1323 entradas) |
| Rutinas-feed (feed único + descarte seguro + `position` sin huecos) | Hecha y mergeada (PR #9, `specs/008-rutinas-feed-spec.md`) |
| Sesión persistida (prefill persistido al iniciar + cierre sin fantasmas) | Hecha y mergeada (PR #10, `specs/009-sesion-persistida-spec.md`) |
| Detalle ejercicio (ficha + `instructions` en modelo/entidad/mapper/DTO, DB v2 + `MIGRATION_1_2`) | Hecha y mergeada (PR #13, `specs/010-detalle-ejercicio-spec.md`) |
| Blindaje visual + tests espejo + autofix (botes, `AppColors/AppDimens`, reglas, DAO/migración, fotos Roborazzi Home y tarjeta, puerta `blindaje.yml`) | Hecha y mergeada (PR #14/#15/#16, `specs/011-blindaje-visual-tests-autofix-spec.md`; fix CI `setup-android@v4` + `gradlew` ejecutable) |
| Buscador escribible + filtro ES + GIF animado en ficha (hero, thumbs quietos, `coil-gif` lote D, test JVM del filtro) | Hecha y mergeada (PR #17, `specs/012-buscador-gifs-spec.md`) |
| Sesión-vivo-fixes (modal numérico KG/REPS/PAUSA + cronómetro detenido con Comenzar/Pausar/Reanudar + tap único para expandir) | Hecha y mergeada (PR #18, `specs/013-sesion-vivo-fixes-spec.md`) |
| Perfil-enfoque-crear-rutina (chips legibles a 360dp + preview que navega a `routineEdit` nuevo) | Hecha y mergeada (PR #19, `specs/014-perfil-enfoque-crear-rutina-spec.md`) |
| Calendario-progreso (mes + progreso reales, `scheduled_days` + DAO, DB v3 + `MIGRATION_2_3`, `PlaceholderTabs` borrado) | Hecha y mergeada (PR #22, `specs/015`; 36/36 instrumentados en emulador y físico) |
| Sesión: edición en vivo (⋮ "Editar rutina"/"Salir del entrenamiento", "+ Añadir serie" con doble escritura transaccional, swipe solo en pendientes, auto-reemplazo del apunte tras finalizar) | Hecha y mergeada (PR #23, `specs/016`) |
| Picker a alto completo (`fillMaxHeight(0.88f)` → `1f` en `ExercisePickerSheet`) | Hecha y mergeada (PR #24, `specs/017`) |
| Guardar rutina con barra auto-oculta (pie se oculta al bajar y reaparece al subir) | Hecha y mergeada (PR #25, `specs/018`) |
| Sesión colapsada + avance automático (`WorkoutViewModel` + `WorkoutExpandRules`; colapso al cerrar descanso, tap manual re-expande) | Hecha y mergeada (PR #26, `specs/019`) |
| Ceros numéricos ("012" → "12" mientras se teclea, regla única en modal, constructor y perfil) | Hecha y mergeada (PR #27, `specs/020`) |
| Sustituir ejercicio en sesión (swipe en colapsado → picker; queda guardado en la rutina de forma permanente) | Hecha y mergeada (PR #28, `specs/021`) |
| Sesión en marcha (cronómetro persistente, card EN MARCHA en Home con Reanudar/Terminar/Descartar, guardia anti-doble sesión, DB v4 + `MIGRATION_3_4`) | Hecha y mergeada (PR #30, `specs/022`; verificación completa en `docs/estado.md`) |
| Duración de rutina (vacía `—` / aproximada AUTO / real MANUAL-aplicada, DB v5 + `MIGRATION_4_5`) | En rama `023-duracion-rutina`, verificado, **pendiente de merge** (`specs/023`) |

Desviación conocida: `material-icons-core` solo trae 49 iconos, así que el dock usa fallbacks
(`List`/`DateRange`/`Star`/`Person`); la fidelidad exacta a los iconos del diseño exigiría `material-icons-extended`.

### Apéndice — Reparación bugs post-014 (PR #20, sin spec)

Retoques en `HomeScreen.kt` (+11) + `RoutineEditScreen.kt` (+68/-10), commit `589a594 bugs retocados`.
Sin cambio de esquema ni dependencias. Riesgo: puede haber movido los dorados Home/tarjeta → re-verificar
fotos (`testDebugUnitTest` + revisión visual) antes de arrancar CUSTOM.

## MVP (Fase 1)

1. Ver el catálogo de ejercicios y filtrarlo por músculo y equipamiento.
2. Ver el detalle de un ejercicio (GIF e instrucciones).
3. Crear ejercicios propios que no estén en el catálogo.
4. Crear una rutina (un día de entrenamiento) con ejercicios y, para cada uno, sus series planificadas: repeticiones, peso y descanso por serie.
5. Iniciar una sesión desde una rutina y registrar cada serie (repeticiones, peso con signo y nota).
6. Cronómetro de descanso dentro de la pantalla, con el tiempo de esa serie.
7. Historial de sesiones.

Quedan **fuera del MVP**: cuentas de usuario, sincronización, gráficas de progreso, cronómetro con la pantalla bloqueada, accesorios reutilizables (gomas predefinidas).

Del MVP, la Home ya cubre parcial: ver rutinas y punto de entrada a sesión (registrar series va en `rutina`/`editar-rutina`).

## Fases

| Fase | Contenido |
|---|---|
| **1** | App Android local con catálogo descargado de la API estática. Sin cuentas ni servidor propio. |
| **2** | Backend Spring Boot + MySQL, cuentas de usuario y sincronización (copia de seguridad y multidispositivo). |
| **3** | Cronómetro en segundo plano, gráficas de progreso, accesorios reutilizables y, si se decide publicar, revisión de licencia de los medios. |

## Documentación

- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md): capas, estructura de carpetas, flujos y reglas.
- [`docs/MODELO_DE_DATOS.md`](docs/MODELO_DE_DATOS.md): entidades de Room y comportamiento de las series.
- [`docs/fotos-referencia.md`](docs/fotos-referencia.md): dorados Roborazzi (solo Home + tarjeta) y regeneración.
- [`docs/revision-tarde.md`](docs/revision-tarde.md): borrador de revisión de tarde (hora pendiente) + regla de auto-reparación.

## Compilar y ejecutar

Requisitos: Android Studio, Android SDK y **emulador Pixel 6 API 34** (`emulator-5554`).
El SDK se apunta en `local.properties` con `sdk.dir` (no se versiona).

- Compilar: `./gradlew assembleDebug`
- Tests: `./gradlew testDebugUnitTest` (actualmente 110/110: botes `AppColors/AppDimens` + `CatalogMappingTest` + reglas `RoutineRules/WorkoutPrefill` con falsos + timer/`HomeLive`/`StartSessionGuard` del 022 + reglas y flujo de duración del 023 + filtro ES del catálogo + fotos Roborazzi Home y tarjeta. Debe quedar todo en verde)
- Tests vistas (emulador encendido): `./gradlew connectedDebugAndroidTest` (DAO + migraciones `12/23/34/45` + smokes `feature/home|routines|workout|history` + sesiones `Workout013/016/019/021/022` + `PickerSearchTest` + `RoutineEdit023DurationTest`, con `testTag` en raíces/CTAs; en físico Xiaomi activar "Instalar vía USB" y usar `adb install` + `am instrument` si AGP falla con `-g`)
- CI (puerta de merge `.github/workflows/blindaje.yml`, `setup-android@v4`): `assembleDebug` + `testDebugUnitTest` en cada PR/push a `main`; exigir el check en Settings > Branches > main
- Fotos de referencia: solo Home y tarjeta (`docs/fotos-referencia.md`); regenerar con `./gradlew recordRoborazziDebug` solo ante cambio visual legal
- Verificación: **emulador Pixel 6 API 34 + móvil físico, de forma automática** (`adb` + `mobile-mcp`).
- Proyecto: `applicationId`/`namespace` `com.mytrainingplan.app`, `minSdk 26`, `compileSdk`/`targetSdk 37`, Java 17.

## Importante!

**Archivos de configuración de Gradle:** el esqueleto del proyecto (Gradle, el wrapper y las versiones de 
plugins y librerías) lo crea y mantiene Android Studio, no el agente. No crear, regenerar ni modificar por tu cuenta 
`gradlew`, `gradlew.bat`, la carpeta `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml` ni los `build.gradle.kts` (el de la raíz y el de `app`). Excepción ya consumida
(lote A): `navigation-compose, lifecycle-viewmodel-compose, material-icons-core, coil-compose` están declarados
(`app/build.gradle.kts`) y en uso (navigation, viewmodel, icons; Coil vía `AsyncImage` en picker/detalle/editar
para thumbs + GIF grande; avatar picker pendiente). Si una tarea necesita otra dependencia
o plugin nuevo, indícalo y propón la línea exacta a añadir, pero no la escribas en esos archivos: las versiones las confirma
el desarrollador desde la documentación oficial o desde Android Studio. Excepción lote B ya consumida
(spec 006, mergeado): `room, hilt, datastore-preferences, retrofit + converter-gson` están declarados y en uso.
Excepción lote C ya consumida (spec 011, mergeado PR #14-16): `roborazzi, roborazzi-compose, robolectric`
(`testImplementation`) + plugin `io.github.takahirom.roborazzi`, con `testOptions.unitTests.isIncludeAndroidResources=true`
y dorados en `app/src/test/screenshots` (solo Home + tarjeta).
Excepción lote D ya consumida (spec 012, mergeado PR #17, autorizada 2026-10-09): `coil-gif` misma versión que
`coil` (2.6.0) en `libs.versions.toml:41` + `app/build.gradle.kts`, con `ImageLoaderFactory` global en
`TrainingApp.kt` (`GifDecoder.Factory()`); hero animado en ficha, thumbs quietos en lista.
Excepción puntual CI autorizada por el usuario (2026-10-09, PR #16, ya aplicada): `setup-android@v3` → `@v4`
en `.github/workflows/blindaje.yml` (v3 pedía el paquete obsoleto `tools` → `Failed to find package 'tools'`)
+ bit ejecutable en `gradlew` (`100644` → `100755`, contenido sin cambios).
El agente trabaja únicamente sobre el código Kotlin, los recursos y la documentación.

## Distribución

Para que una versión actualice a la anterior (y no borre datos):

- Mismo `applicationId` siempre.
- Firmar siempre con la misma keystore (con copia de seguridad fuera del repositorio).
- Subir `versionCode` en cada versión.
- Si cambia el esquema de Room, subir la versión de la base de datos y escribir su migración.

## Próximos pasos (TODO vivo)

> Referencia de detalles que faltan. Marcar `[x]` solo con evidencia (`assembleDebug` + `connectedDebugAndroidTest` en emulador Pixel 6 API 34). Los `specs/` cerrados no se reescriben.

### P0 — Rutinas (menú ···)
- [x] `···` de cada card abre menú `Editar / Eliminar` (`HomeScreen RoutineCard + DropdownMenu`, `MainActivity` pasa `onOptions` real).
- [x] `Eliminar` pide confirmación (`AlertDialog`: `¿Eliminar "X"?`) y al `Sí` borra lógico (`RoomRoutineRepository.deleteRoutine` + espejo en `Fake` + `renumber` sin huecos, `@Upsert`, sesiones históricas intactas).
- [x] `Editar` navega a `routineEdit?routineId=id` y permite cambiar título/duración, añadir/quitar ejercicios y series (reutiliza constructor `004`).
- [ ] Verificado en emulador Pixel 6 API 34: menú, cancelar, eliminar, editar, rotación, gestos/3 botones (el diálogo gestiona insets solo).

### P1 — MVP bloqueantes
- [x] Detalle ejercicio (MVP-2): hecho y mergeado (PR #13, `specs/010-detalle-ejercicio-spec.md`): tap fila → GIF grande + instrucciones + secundarios, `instructions` rescatadas (`DTO→Entity→dominio`, DB v2 + `MIGRATION_1_2`) + ruta detalle. Animación del hero + buscador ES van en PR #17 (spec 012).
- [ ] Ejercicios propios CUSTOM (MVP-3, siguiente tras 024): crear/editar desde app (`insert` en `ExerciseRepository` + UI formulario); `CatalogSync` ya protege `CUSTOM`.
- [x] Historial + `home-post-sesion` (MVP-7, `specs/015`, hecho y mergeado en PR #22): tabs `calendar`/`progress` reales, lista de hechos por día, `lastDoneLabel`/racha/progreso reales; `Ver mes` cableado.

### P2 — Sesión en vivo
- [x] Modal numérico KG/REPS/PAUSA + cronómetro detenido con Comenzar/Pausar/Reanudar + tap único para expandir (PR #18, spec 013).
- [x] Sesión colapsada al abrir + colapso auto al cerrar descanso y avance al siguiente pendiente; tap manual re-expande (spec 019, rama `019-sesion-colapsado`, 8/8 verificado en emulador y físico, pendiente merge).
- [ ] `Añadir Ejercicio/Serie en vivo` (picker `003` reutilizado).
- [ ] Notas/`loadNote` + RPE/DROP en vivo (modelo `Workout.kt:45` lo tiene, UI no).
- [ ] Descanso avanzado: sonido/vibración, presets (hoy bloqueante + `±10s`).
- [x] Minimizar-background: cronómetro persistente en DB (`timerBaseSec`/`runningSince`, sync kill-safe cada 15 s; verificado force-stop → relanzar en 022).

### P3 — Deuda UX/técnica
- [ ] Avatar picker + Coil (`PickVisualMedia` + URI persistente; hoy placeholder `ProfileScreen.kt:212`).
- [x] Tema: tokens locales migrados a botes con nombre en `ui/theme/` (`Color/Dimensions/Theme/Type`, spec 011 mergeado).
  Pendiente solo tipografías Outfit/Jakarta/Space Grotesk (hoy sistema).
- [ ] `material-icons-extended` (proponer línea, esperar) o mantener fallbacks `List/DateRange/Star/Person`.
- [ ] Catálogo `en` + reintento manual picker vacío (hoy solo `es` + auto al abrir).
- [ ] Fuera MVP (no tocar): accesorios tabla propia, gráficas, Spring/MySQL Fase 2.
