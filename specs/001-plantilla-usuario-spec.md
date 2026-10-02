# Plantilla Usuario – Perfil de Atleta (copia imagen)
Estado: aprobado
Depende de: Ninguno
Fecha de creación: 2026-10-02
Descripción: Crear la pantalla de perfil de atleta como copia exacta de references/plantilla-usuario/screen.png (HTML en code.html) en Kotlin + Compose, con persistencia local en Room y navegación propia.

## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-usuario` como pantalla de configuración inicial del perfil de atleta, con avatar, datos biométricos, nivel, enfoque y vista previa live, persistida en local y verificable en móvil físico.

## 2. Alcance (entra / no entra)
Entra:
- Nueva pantalla `feature/profile` con `ProfileScreen + ProfileViewModel` que replica `screen.png` y `code.html` (header 1/1, avatar con halo y badge cámara, campo nombre, grid Edad/Estatura/Peso, chips Nivel y Enfoque, card vista previa, CTA Guardar y Continuar).
- Persistencia local en Room con nueva entidad de perfil (campos nombre, edad, estatura, peso, nivel, enfoque, avatarUri) con `updatedAt + deleted` sync-ready, `Migration` de versión y `Repository + DAO + mapper dominio`.
- Avatar con picker del sistema y visualización con Coil (URI local guardada como String).
- Nueva ruta de navegación `profile` en `MainActivity` con botón volver y guardado.
- Vista previa `Hola, {nombre}` en tiempo real dentro de la misma pantalla.

No entra:
- Sincronización Fase 2 ni cuentas de usuario (README fuera de MVP).
- Racha real calculada desde sesiones ni peso vivo del home.
- Conexión con la cabecera real de inicio `Hola, Atleta` ni botón `Crear Rutina`.
- Edición posterior del perfil fuera del flujo inicial, onboarding multi-paso, validaciones i18n en/en, recorte de foto o backup remoto.
- Cambios en catálogo, rutinas, `PlannedSet`, sesiones o `SetEntry`.

## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`AGENTS.md:23`, `docs/ARQUITECTURA.md:11-17,25-30`).
- Hoy solo existen 4 features con `Screen + ViewModel` (`app/src/main/java/com/mytrainingplan/app/feature/exercises/ExercisesScreen.kt:20`, `ExercisesViewModel.kt:27`), y `MainActivity.kt:50-54,89-98` solo navega `exercises, routines, workout, history`. No hay perfil (`grep profile` sin resultados en `app/src`).
- Room v1 con 6 entidades y `exportSchema = true` (`data/local/AppDatabase.kt:20-31`), patrón `@Upsert` + `deleted = 0` + `softDelete` con `updatedAt` (`data/local/dao/RoutineDao.kt:14-15,23,38-39`), ejemplo entidad con `updatedAt/deleted` (`data/local/entity/RoutineEntity.kt:8-14`). Este spec añade la séptima entidad.
- Catálogo no se toca: `core/network/CatalogConfig.kt:12-13` sigue con placeholders `<usuario>/<tag>`, `core/datastore/CatalogPrefs.kt:20-33` guarda tag, DTOs snake_case fuera de alcance.
- Sets no se tocan: `weightKg` con signo y `restSeconds` por serie (`docs/MODELO_DE_DATOS.md:49-58`) quedan intactos.
- UI base: `TrainingApp.kt:16-28` configura Coil con GIF, `core/ui/theme/Theme.kt:9-10` usa `darkColorScheme()` por defecto a extender con tokens `references/plantilla-usuario/DESIGN.md:146-155`, `core/ui/Components.kt:11-16` ejemplo de componentes compartidos.
- Referencia: `references/plantilla-usuario/screen.png` (copia visual exigida), `references/plantilla-usuario/code.html:115-293` (header, hero avatar, form, bento 3 métricas, chips, preview, footer CTA, script live preview `code.html:295-306`), `references/plantilla-usuario/DESIGN.md:138-155` (Dark Tactical Modernism, `#FF5E00`, `#CCFF00`, `#111316`).
- Toolchain solo vía `gradle/libs.versions.toml` (`AGENTS.md:19`), `app/build.gradle.kts:11-19,28-37` (compileSdk/target 34, min 26, sin `.debug`), Coil 2.6.0 y DataStore ya declarados.

## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Mostrar header sticky con volver, título `Tu Perfil de Atleta`, subtítulo `CONFIGURACIÓN INICIAL` y pill `1/1` como en `code.html:119-132`.
- Mostrar avatar circular con halo degradado naranja, foto Coil y badge cámara que abre picker y actualiza preview.
- Editar nombre con indicador de válido, y métricas Edad (años int), Estatura (cm int), Peso (kg decimal) con valores iniciales tipo `Carlos Mendoza / 28 / 178 / 78.5`.
- Elegir Nivel (`Inicial <1 año / Intermedio 1-3 años / Avanzado +3 años`, defecto Intermedio) y Enfoque (`Hipertrofia / Fuerza & Potencia / Resistencia`, defecto Fuerza) con selección única visible naranja.
- Mostrar card `Vista previa en cabecera de inicio / En tiempo real` con `Hola, {primerNombre}`, racha fija `4 días` y peso actual, actualizada al escribir.
- CTA inferior fijo `Guardar y Continuar` que persiste y navega atrás o a inicio.
- Persistir todo en Room tras guardar y restaurar al reabrir.

No-funcionales:
- Offline-first: sin red funciona con SQLite.
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, radios y espaciados DESIGN.md).
- Sin jank en scroll, inputs con teclado numérico/decimal, hit targets >= 48dp, prueba en móvil físico sin emulador.
- Entidad sync-ready desde día uno (`updatedAt + deleted`, borrado lógico).

## 5. Criterios de aceptación verificables
- En móvil físico la pantalla es copia de `screen.png`: header, avatar con halo y badge, nombre, 3 cards biométricas, nivel, enfoque, preview y CTA coinciden en orden, color y jerarquía.
- Escribir en nombre actualiza `Hola, {primerNombre}` de la preview en tiempo real.
- Cambiar Edad/Estatura/Peso/Nivel/Enfoque se refleja en UI y se persiste tras cerrar y reabrir la app.
- Pulsar badge o `Cambiar foto` abre picker, la imagen elegida se ve en avatar y en preview, y persiste tras reinicio.
- Pulsar `Guardar y Continuar` guarda en Room y vuelve atrás sin crash ni pérdida de estado.
- Rotar el dispositivo conserva lo escrito sin duplicar ni borrar.
- Reinstalar manteniendo datos conserva el perfil (mismo `applicationId`, sin `.debug`).

## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- Nueva `feature/profile/` con `ProfileScreen` (stateless + stateful con `hiltViewModel`, `collectAsState`) y `ProfileViewModel` (`MutableStateFlow<ProfileUiState>`, `combine` con `observeProfile`, eventos por campo y `save` en `viewModelScope`), siguiendo patrón `feature/exercises`.
- Dominio nuevo `domain/model/Profile` con nivel y enfoque como strings/enums de dominio; UI nunca importa Entity ni DTO.
- Room: `data/local/entity/UserProfileEntity` tabla `user_profile` (id singleton, displayName, age, heightCm, weightKg Double, level, goal, avatarUri nullable, updatedAt, deleted), `data/local/dao/ProfileDao` con `@Upsert`, `observe` Flow y `getOnce`, sin hard delete.
- `AppDatabase` versión 2 con `Migration 1→2` (`CREATE TABLE`), `exportSchema = true`, schema en `app/schemas/`, `DatabaseModule` provee `ProfileDao`.
- `data/repository/ProfileRepository` única puerta (observe + upsert), `data/mapper` Entity⇄dominio.
- Navegación: nueva ruta `profile` en `MainActivity NavHost`, sin añadir tab al bottom nav en este spec.
- Catálogo: no toca `CATALOG_TAG`, `CatalogPrefs`, `GET api/{lang}/exercises.json`, DTOs ni GIFs remotos. Sets: no toca `weightKg` con signo, `loadNote`, `restSeconds` ni propagación/prefill de pantalla.
- Tema: extender `MyTrainingPlanTheme` con colores exactos de referencia para esta pantalla, tipografías del sistema con pesos que aproximen Outfit/Jakarta/Grotesk salvo que ya existan assets.
- Qué NO se toca: `applicationId`, no `applicationIdSuffix .debug`, misma keystore fuera del repo + bump `versionCode` solo en release, `gradle-wrapper.jar` ausente (abrir en Android Studio primero).

## 7. Plan de tareas
1. Dominio perfil en `domain/model/` (Profile + Nivel + Enfoque).
2. Entidad `data/local/entity/UserProfileEntity.kt` + `data/local/dao/ProfileDao.kt` con `@Upsert` y borrado lógico.
3. Subir `data/local/AppDatabase.kt` a versión 2 + `Migration(1,2)` y schema en `app/schemas/`.
4. Exponer `ProfileDao` en `core/di/DatabaseModule.kt`.
5. Crear `data/repository/ProfileRepository.kt` (observe Flow + upsert).
6. Añadir mapeo perfil en `data/mapper/`.
7. Crear `feature/profile/ProfileViewModel.kt` con `StateFlow`, validación de rangos y preview `Hola, {nombre}`.
8. Crear `feature/profile/ProfileScreen.kt` como copia Compose de `screen.png` / `code.html` (header, avatar Coil + halo, form, bento, chips, preview, CTA).
9. Integrar ruta `profile` en `MainActivity.kt` (NavHost + back + save).
10. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico.

## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (abrir en Android Studio primero porque `gradlew` es stub sin `gradle-wrapper.jar`).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo).
- Prueba en móvil físico: comparar con `screen.png`, editar todos los campos, picker de foto, preview live, guardar, cerrar/reabrir, rotar, reinstalar sin borrar datos.

## 9. Riesgos / No romper
- No usar `@Insert(onConflict = REPLACE)` (borra filas y cascadas); solo `@Upsert`.
- No hacer `fallbackToDestructiveMigration` en release (comentado en `DatabaseModule.kt:30-31`); exigir `Migration` real.
- No cambiar `applicationId` ni añadir `.debug`; no mover keystore al repo.
- No exponer Entity/DTO a UI; mapping solo en `data/mapper`.
- No apuntar catálogo a rama (tag pineado) ni tocar rows `source=CATALOG`.
- Foto: pedir permiso/URI persistente correctamente para que sobreviva a reinicio; placeholder si se deniega.
- Teclados numéricos con `inputmode` correcto para no romper validación de edad/altura/peso.

## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Conectar preview con cabecera real de inicio `Hola, Atleta` (`MainActivity.kt:56-99` detectado al analizar tabs).
- Racha real desde `WorkoutSessionEntity` en vez de `4 días` fijo (`code.html:273`).
- Validación avanzada con mensajes de error y rangos médicos (`ProfileScreen` futuro).
- Recorte de avatar y normalización de URI (`TrainingApp.kt:16-28` Coil).
- Sync Fase 2 del perfil con backend Spring + MySQL usando `updatedAt/deleted` (`docs/ARQUITECTURA.md:16`).
- Accesorios reutilizables tipo gomas (`docs/MODELO_DE_DATOS.md:105`).
- Soporte `es/en` e i18n de Nivel/Enfoque (`core/network/CatalogConfig.kt:15-17` idiomas).
- Edición posterior del perfil fuera del onboarding 1/1 (`references/plantilla-usuario/code.html:128-130`).

## Preguntas abiertas
- ¿ID singleton fijo (p. ej. `me`) o UUID aleatorio con `SELECT LIMIT 1`? Propuesta: singleton `me` por ser perfil único local.
- ¿La ruta `profile` debe ser `startDestination` en primer arranque o solo accesible bajo demanda en este spec?

## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Pantalla copia `screen.png` verificada en móvil físico (header, avatar, form, chips, preview, CTA)
- [ ] Nombre actualiza `Hola, {primerNombre}` en tiempo real
- [ ] Edad/Estatura/Peso/Nivel/Enfoque editables y persistidos tras reabrir
- [ ] Picker de foto actualiza avatar y preview y persiste tras reinicio
- [ ] `Guardar y Continuar` guarda en Room y navega sin crash
- [ ] Rotación conserva estado sin duplicar
- [ ] Reinstalación conserva perfil (mismo applicationId, sin .debug)
- [ ] `./gradlew assembleDebug` OK
- [ ] Prueba en móvil físico OK
