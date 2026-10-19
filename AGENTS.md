# AGENTS.md

## Estado (resumen)

- Hechas: Fase A (UI-first, 5/5 vistas), Fase B (Room local) y specs `007` a `022`, todos mergeados en `main` (último: 022 en PR #30). DB Room versión **5**.
- Estado intermedio: `023-duracion-rutina` implementado y verificado, **pendiente de merge**; `024-logo-launcher` creado (Borrador).
- Siguiente: `024-logo-launcher` (Borrador, pendiente de aprobación). Más tarde: CUSTOM (MVP-3) y Fase D Spring/MySQL.
- Detalle por spec, deuda, desviaciones y puntos de control: `docs/estado.md`. Léelo solo si la tarea lo necesita. Lo mantiene `/spec-impl` al cerrar cada spec (entradas "implementado, pendiente de merge"; el usuario las pasa a "mergeado"). Si trabajas fuera de un spec, actualízalo tú al terminar.
- `docs/`, `specs/` y `references/` son guía de objetivo/diseño, no código hecho. Antes de editar, comprueba con `Glob`/`Grep` que el fichero existe: los specs pueden citar rutas futuras.
- Deuda: los `Fake*Repository` de Fase A ya no se inyectan (Hilt usa los Room). No borrarlos sin confirmar.
- Iconos: `material-icons-core` solo trae 49. Para fidelidad exacta haría falta `material-icons-extended`: proponer la línea exacta y esperar.

Fuente de verdad: `README.md` (principios + restricción Gradle), `docs/ARQUITECTURA.md` (capas y carpetas), `docs/MODELO_DE_DATOS.md` (entidades y series). Si un spec contradice a estos o al código, avisa y sigue al código.

## Restricción Gradle (obligatoria)

El esqueleto Gradle lo crea y mantiene Android Studio, **no el agente**. No crear, regenerar ni editar:
`gradlew`, `gradlew.bat`, `gradle/wrapper/`, `settings.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `build.gradle.kts` (raíz y `app/`).

Dependencias ya declaradas y en uso (lotes A–D consumidos): `navigation-compose`, `lifecycle-viewmodel-compose`, `material-icons-core`, `coil-compose`, `coil-gif` (misma versión que `coil`), `room`, `hilt`, `datastore-preferences`, `retrofit` + `converter-gson`, `roborazzi`, `roborazzi-compose`, `robolectric` (más el plugin `io.github.takahirom.roborazzi`).
**No añadir más.** Si hace falta una nueva, proponer la línea exacta y esperar. Versiones solo vía catálogo `gradle/libs.versions.toml`.

Excepción permanente: `.github/workflows/` sí lo mantiene el agente (usa `setup-android@v4`; `gradlew` debe conservar el bit ejecutable).

El agente trabaja Kotlin en `app/src/main/`, recursos y documentación.

## Ejecución de Gradle (obligatoria)

1. **Un solo Gradle a la vez.** Si Android Studio está sincronizando o compilando, espera o avisa. No lances builds en paralelo.
2. **Salida a fichero, no a la tubería.** Lanza siempre con `--console=plain` y redirige:
   `mkdir -p build && ./gradlew <tarea> --console=plain > build/agent.log 2>&1`
   y lee solo el final (`tail -n 40 build/agent.log`) o filtra con `grep -nE "error|FAILED|BUILD"`.
3. **Timeout generoso y explícito.** Pasa el parámetro de timeout de la herramienta bash a **600000 ms (10 min)** para `assembleDebug` y `testDebugUnitTest`. No cortes antes: un build en frío, o el primero tras tocar dependencias, Room o KSP, puede tardar varios minutos.
4. **Si se supera el timeout: no reintentes la misma tarea.** Ejecuta `./gradlew --stop`, anota `bloqueado: build > 10 min`, informa y deja que el usuario compile desde Android Studio.
5. Nada de `gradle` global ni de regenerar el wrapper.

## Comandos

- Compilar: `./gradlew assembleDebug` (requiere Android SDK; `local.properties` con `sdk.dir`, no se versiona).
  Comprobación rápida por paso: `./gradlew :app:compileDebugKotlin`.
- Tests JVM: `./gradlew testDebugUnitTest`. Cubre botes `AppColors/AppDimens`, `CatalogMappingTest`, reglas `RoutineRules/WorkoutPrefill` con fakes, filtro ES del catálogo, fotos Roborazzi (Home y tarjeta) y `WorkoutLiveEditTest`. Debe quedar todo en verde.
- Puerta CI: `.github/workflows/blindaje.yml` (`assembleDebug` + `testDebugUnitTest` en cada PR/push a `main`; el check se exige en Settings > Branches > main).
- Tests instrumentados: `connectedDebugAndroidTest` (DAO, migraciones `12/23/34/45`, smokes `feature/home|routines|workout|history`, tests de sesión `Workout013/016/019/021/022`, `PickerSearchTest`, `RoutineEdit023DurationTest`), con `testTag` en raíces/CTAs (`profileRoot/profileSave`, `homeRoot/homeDock/routineCard:<id>`, `routineEditRoot/routineEditAddExercise/routineEditSave`, `workoutRoot/workoutFinish/restOverlay`). Smoke de detalle pendiente.
- Verificación: **emulador Pixel 6 API 34** (`emulator-5554`) **+ dispositivo físico, de forma automática**; se testea en ambos con `adb` + MCP `mobile-mcp` (`list_available_devices` → `list_elements` por `ref` → `click`/`screenshot`/`logs`) y `connectedDebugAndroidTest` como red de seguridad. Si un spec dice explícitamente solo uno, manda el spec y se anota.
  **Antes de lanzar nada contra dispositivo, comprueba `adb devices`.** Si no hay ninguno en estado `device`, no lances `connectedDebugAndroidTest`: anota `bloqueado: sin dispositivo` y sigue.
  En físico Xiaomi/MIUI: activar "Instalar vía USB" (si no, `INSTALL_FAILED_USER_RESTRICTED`). AGP instala el APK de test con `-g` y MIUI lo rechaza (`SecurityException`) → fallback: `adb install` manual + `am instrument`.
- Proyecto: `applicationId`/`namespace` `com.mytrainingplan.app`, `minSdk 26`, `compileSdk/targetSdk 37`, Java 17.

## Arquitectura

- Un solo módulo `app`. Flujo: `feature/* Screen → ViewModel(StateFlow) → Repository → Room`. Retrofit solo rellena la DB.
- Fases: A maquetación UI-first (hecha), B Room local (hecha), catálogo y specs 007–017 (hechos), Fase D Spring/MySQL (pendiente).
- Carpetas: `core/{di,network,datastore,ui}`, `data/{local/{entity,dao},remote/{dto},mapper,repository}`, `domain/model`, `feature/{profile,home,exercises,routines,workout,history}`, y `ui/theme/` (`Color/Dimensions/Theme/Type`). Cada `feature/` = `Screen` + `ViewModel` (`exercises` tiene Picker + Detail; `history` tiene Calendar + Progress).
- Mapeo `DTO/Entity ⇄ dominio` en `data/mapper`. La UI solo ve `domain/model`, nunca Entity ni DTO.
- Sin casos de uso ni módulos extra hasta que duelan.
- Catálogo: fork ExerciseGymGifsDB por jsDelivr con **tag fijo** (`CATALOG_TAG` + DataStore), nunca rama. Solo URLs de GIF en DB (Coil cachea). Update por `upsert` y solo filas `source=CATALOG`.

## Datos Room

- IDs de usuario = **UUID** en cliente. Los ejercicios de catálogo conservan su `id` (`músculo/slug`).
- Toda entidad de usuario lleva `updatedAt` + `deleted` (borrado lógico, sync-ready Fase 2) desde el día uno.
- Catálogo y propios comparten tabla con `source` (`CATALOG`/`CUSTOM`).
- **Siempre `@Upsert`, nunca `@Insert(onConflict = REPLACE)`** (REPLACE borra la fila y arrastra hijas).
- `exportSchema = true` (schemas en `app/schemas/`). Cada cambio de esquema sube versión + `Migration` real. `fallbackToDestructiveMigration()` solo en desarrollo. Versión actual y migraciones: míralas en `AppDatabase` y `app/schemas/`.
- `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `loadNote` libre. `restSeconds` vive en **cada serie**, no en el ejercicio.
- Comportamiento de series (`docs/MODELO_DE_DATOS.md`): al crear N series se copian valores de la 1ª. Editar propaga solo a siguientes **no editadas a mano** (estado solo de pantalla). Día nuevo = valores por defecto de ajustes, sin copiar de otros días. Al iniciar sesión, prefill con **última sesión** de ese ejercicio en ese día, y si no existe, con lo planificado.

## Distribución

Mismo `applicationId` siempre (no cambiarlo ni añadir `applicationIdSuffix ".debug"`). Misma keystore (fuera del repo, con copia externa). Subir `versionCode` por versión. Cambio de esquema Room = subir versión DB + migración o se pierden datos.

## Edge-to-edge (obligatorio en cada vista)

La app usa `enableEdgeToEdge()` (`MainActivity.kt`): el contenido dibuja por debajo de las barras del sistema, que son una capa del sistema por encima (verificado contra docs oficiales vía Context7). Reglas:

- Toda cabecera superior lleva `statusBarsPadding()` (con el fondo extendido, para que la zona de estado quede del color de la vista).
- Todo dock/CTA inferior fijo lleva `navigationBarsPadding()` (conservando su margen visual de 16dp).
- Prohibido compensar con dp fijos extra que imiten la altura de las barras.
- Excepciones que ya gestionan insets solas (no tocar): `ModalBottomSheet` y diálogos de Material3; contenido centrado sin elementos pegados a los bordes.
- Checklist para futuras vistas: probar en emulador Pixel 6 API 34 **y en físico, de forma automática**, con navegación por gestos y (si se puede) con 3 botones. Arriba nada bajo hora/cobertura/batería, abajo nada bajo la barra del sistema.
