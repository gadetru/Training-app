# Plantilla Usuario – Perfil de Atleta (copia imagen)
Estado: aprobado
Depende de: Ninguno
Fecha de creación: 2026-10-02
Descripción: Crear la pantalla de perfil de atleta como copia exacta de references/plantilla-usuario/screen.png (HTML en code.html) en Kotlin + Compose, maquetación UI-first en memoria sin persistencia aún (Fase A).

## 1. Objetivo
Replicar en Kotlin + Jetpack Compose la vista de `references/plantilla-usuario` como pantalla de configuración inicial del perfil de atleta, con avatar, datos biométricos, nivel, enfoque y vista previa live, en memoria y verificable en móvil físico. Sin Room ni navegación real en este spec (Fase A maquetación).

## 2. Alcance (entra / no entra)
Entra:
- Nueva pantalla `feature/profile` con `ProfileScreen` (stateful con `remember` + `ProfileContent` stateless) que replica `screen.png` y `code.html` (header 1/1, avatar con halo y badge cámara placeholder, campo nombre, grid Edad/Estatura/Peso, chips Nivel y Enfoque, card vista previa, CTA Guardar y Continuar).
- Dominio `domain/model/Profile` (Profile + TrainingLevel + TrainingGoal) con defecto `Carlos Mendoza / 28 / 178 / 78.5`, `Intermedio`, `Fuerza & Potencia`, más `firstName/greeting`.
- Mostrar `ProfileScreen` directo en `MainActivity` con `MyTrainingPlanTheme(dynamicColor=false)`.
- Vista previa `Hola, {nombre}` en tiempo real dentro de la misma pantalla.

No entra:
- Persistencia Room (`UserProfileEntity/DAO/Migration/Repository/mapper`), `ProfileViewModel` real, `NavHost` `profile/home`, picker real + Coil, DataStore.
- Sincronización Fase 2 ni cuentas de usuario (README fuera de MVP).
- Racha real calculada desde sesiones ni peso vivo del home.
- Conexión con la cabecera real de inicio `Hola, Atleta` ni botón `Crear Rutina`.
- Edición posterior del perfil fuera del flujo inicial, onboarding multi-paso, validaciones i18n en/en, recorte de foto o backup remoto.
- Cambios en catálogo, rutinas, `PlannedSet`, sesiones o `SetEntry`.

## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
- Flujo final exigido `UI feature/* → ViewModel StateFlow → Repository → Room`, Retrofit solo rellena DB, UI solo ve `domain/model`, mapping en `data/mapper` (`AGENTS.md`, `docs/ARQUITECTURA.md:11-17,25-30`). En este spec Fase A solo se hace `UI + dominio en memoria`, sin ViewModel/Repository/Room.
- Real hoy: solo existe `feature/profile/ProfileScreen.kt` + `domain/model/Profile.kt`, y `MainActivity.kt` muestra `ProfileScreen()` directo. No hay `exercises/routines/workout/history`, no hay Room/Hilt/Coil/DataStore/Retrofit.
- Paquete fijado `com.mytrainingplan.app` (`app/build.gradle.kts:7,13`), `minSdk 26`, `compileSdk/targetSdk 37`, Java 11, Kotlin `2.2.10`, AGP `9.4.1`, Compose BOM `2026.02.01`. Solo Compose/Material3/activity/core/lifecycle.
- Catálogo no se toca y sets no se tocan (`docs/MODELO_DE_DATOS.md:49-58` intacto).
- UI base: `ui/theme/Theme.kt` con `dynamicColor=false` en `MainActivity` para fidelidad, tokens `references/plantilla-usuario/DESIGN.md:146-155`.
- Referencia: `references/plantilla-usuario/screen.png` (copia visual exigida), `references/plantilla-usuario/code.html:115-293` (header, hero avatar, form, bento 3 métricas, chips, preview, footer CTA, script live preview `code.html:295-306`), `references/plantilla-usuario/DESIGN.md:138-155` (Dark Tactical Modernism, `#FF5E00`, `#CCFF00`, `#111316`).
- Toolchain vía `gradle/libs.versions.toml`; lote A (`navigation-compose, lifecycle-viewmodel-compose, material-icons, coil-compose`) va en el siguiente spec, no aquí.

## 4. Requisitos funcionales + no-funcionales
Funcionales:
- Mostrar header sticky con volver, título `Tu Perfil de Atleta`, subtítulo `CONFIGURACIÓN INICIAL` y pill `1/1` como en `code.html:119-132`.
- Mostrar avatar circular con halo degradado naranja, iniciales placeholder y badge cámara (picker + Coil reales en siguiente spec).
- Editar nombre con indicador de válido, y métricas Edad (años int), Estatura (cm int), Peso (kg decimal) con valores iniciales tipo `Carlos Mendoza / 28 / 178 / 78.5`.
- Elegir Nivel (`Inicial <1 año / Intermedio 1-3 años / Avanzado +3 años`, defecto Intermedio) y Enfoque (`Hipertrofia / Fuerza & Potencia / Resistencia`, defecto Fuerza) con selección única visible naranja.
- Mostrar card `Vista previa en cabecera de inicio / En tiempo real` con `Hola, {primerNombre}`, racha fija `4 días` y peso actual, actualizada al escribir.
- CTA inferior fijo `Guardar y Continuar` con callback `onSave(Profile)` sin persistencia aún.
- Estado en memoria con `remember`; restaurar tras reabrir y Room van en Fase B.

No-funcionales:
- Sin red funciona (todo local en memoria, sin SQLite aún).
- Copia visual de `screen.png` (fondo `#111316`, cards `#1A1C1F`, CTA `#FF5E00`, radios y espaciados DESIGN.md).
- Sin jank en scroll, inputs con teclado numérico/decimal, hit targets >= 48dp, prueba en móvil físico sin emulador.
- Dominio lleva `updatedAt + deleted` + `id="me"` singleton listos para Room sync-ready (Fase B).

## 5. Criterios de aceptación verificables
- En móvil físico la pantalla es copia de `screen.png`: header, avatar con halo y badge, nombre, 3 cards biométricas, nivel, enfoque, preview y CTA coinciden en orden, color y jerarquía.
- Escribir en nombre actualiza `Hola, {primerNombre}` de la preview en tiempo real.
- Cambiar Edad/Estatura/Peso/Nivel/Enfoque se refleja en UI en la misma sesión (persistencia tras reabrir va en Fase B).
- Pulsar badge o `Cambiar foto` es TODO visible sin crash (picker + Coil + persistencia van en siguiente spec).
- Pulsar `Guardar y Continuar` emite `onSave(Profile)` sin crash ni pérdida de estado en sesión.
- Rotar el dispositivo conserva lo escrito en sesión con `remember` (endurecer con `rememberSaveable/ViewModel` en siguiente spec).
- Paquete `com.mytrainingplan.app`, sin `.debug`.

## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/profile/ProfileScreen` hecha: `ProfileScreen(onBack/onSave)` stateful con `remember` + `ProfileContent` stateless; bento, chips y preview como en `screen.png/code.html`.
- Dominio hecho `domain/model/Profile` (`id="me"` singleton local, `displayName/age/heightCm/weightKg/level/goal/avatarUri`, `updatedAt/deleted`, `firstName/greeting`); UI solo ve dominio.
- Room/DAO/Migration/Repository/mapper/`ProfileViewModel` real/`NavHost`/`Coil` real: NO en este spec, van en Fase B y lote A siguiente.
- Catálogo: no se toca. Sets: no se tocan (`weightKg` signo, `loadNote`, `restSeconds` intactos).
- Tema: `MyTrainingPlanTheme(dynamicColor=false, darkTheme=true)` en `MainActivity` + colores locales de referencia; tipografías sistema.
- Qué NO se toca: `applicationId/com.mytrainingplan.app`, no `.debug`, misma keystore fuera del repo + bump `versionCode` solo en release.

## 7. Plan de tareas
1. Dominio perfil en `domain/model/` (Profile + Nivel + Enfoque). Hecho.
2. Crear `feature/profile/ProfileScreen.kt` como copia Compose de `screen.png` / `code.html` (header, avatar placeholder + halo, form, bento, chips, preview, CTA). Hecho.
3. Mostrar `ProfileScreen` directo en `MainActivity.kt`. Hecho.
4. Verificación: `./gradlew assembleDebug` OK + prueba en móvil físico. Hecho assemble; móvil pendiente a mano.

## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (hecho tras rename a `com.mytrainingplan.app`).
- `CatalogMappingTest` no aplica (no toca mapper/DTO de catálogo).
- Prueba en móvil físico: comparar con `screen.png`, editar campos, preview live, guardar en sesión, rotar. Cerrar/reabrir persistente y picker real van en Fase B/lote A.

## 9. Riesgos / No romper
- En Fase B: solo `@Upsert`, nunca `@Insert(REPLACE)`; `Migration` real, no `fallbackToDestructiveMigration` en release.
- No cambiar `applicationId/com.mytrainingplan.app` ni añadir `.debug`; no mover keystore al repo.
- UI solo ve `domain/model`; mapping en `data/mapper` cuando exista.
- Foto placeholder ahora; en lote A pedir URI persistente o se pierde al reiniciar.
- Teclados numéricos/decimal correctos para no romper edad/altura/peso.

## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Lote A: `navigation-compose` (`NavHost` `profile/home` condicional `¿hay perfil?`), `lifecycle-viewmodel-compose` (`ProfileViewModel` fake `StateFlow` + repo fake misma firma), `material-icons`, `coil-compose` (picker `PickVisualMedia` + URI persistente + avatar/preview reales).
- Fase B Room perfil: `UserProfileEntity(user_profile, id="me") + ProfileDao @Upsert/observe/softDelete + AppDatabase Migration + DatabaseModule + ProfileRepository + mapper`, reabrir/rotar/reinstalar reales.
- Otras 4 vistas UI-first: `vista-principal` (home `Hola Atleta`), `lista-ejercicios`, `rutina`, `editar-rutina` desde `references/*/screen.png+code.html+DESIGN.md`.
- Conectar preview con cabecera real de inicio `Hola, Atleta`.
- Racha real desde sesiones en vez de `4 días` fijo (`code.html:273`).
- Sync Fase 2 Spring + MySQL con `updatedAt/deleted`.

## Preguntas abiertas
- ¿ID singleton fijo (p. ej. `me`) o UUID aleatorio con `SELECT LIMIT 1`? Propuesta: singleton `me` por ser perfil único local.
- ¿La ruta `profile` debe ser `startDestination` en primer arranque o solo accesible bajo demanda en este spec?

## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Pantalla copia `screen.png` verificada en móvil físico (header, avatar, form, chips, preview, CTA)
- [ ] Nombre actualiza `Hola, {primerNombre}` en tiempo real
- [ ] Edad/Estatura/Peso/Nivel/Enfoque editables en sesión
- [ ] Badge `Cambiar foto` visible sin crash (picker real en siguiente spec)
- [ ] `Guardar y Continuar` emite `onSave` sin crash
- [ ] Rotación conserva estado en sesión
- [ ] Paquete `com.mytrainingplan.app`, sin `.debug`
- [ ] `./gradlew assembleDebug` OK
- [ ] Prueba en móvil físico OK
