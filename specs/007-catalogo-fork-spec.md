git# Catálogo desde el fork + `getById` async
Estado: aprobado

Depende de: specs/006-fase-b-room-local-spec.md (usa su Room, DAOs, sync y DTO ya implementados en rama `006-fase-b-room-local`)
Fecha de creación: 2026-10-05
Descripción: El picker abre vacío porque el fork no tiene tag publicado (jsDelivr 404) y no hay semilla. Este spec pine el fork con su tag, distingue 404 de sin-red y deja `getById` en suspendido. Sin cambios visibles salvo que los ejercicios aparecen.
## 1. Objetivo
Que el picker liste el catálogo del fork con tag fijo, sin red use lo local, y la firma de lectura puntual sea suspendida en fakes y reales.
## 2. Alcance (entra / no entra)
Entra:
- Pineado de `CatalogConfig` al fork `gadetru` con el tag publicado por ti.
- `CatalogSync` distingue tag inexistente (404) de sin red (reintento al próximo arranque).
- `ExerciseRepository.getById` pasa a `suspend`; se actualizan fake y real.
- Ampliación de `CatalogMappingTest` con una entrada real del JSON.
No entra:
- Semilla inicial (decidido: sin semilla).
- Ejercicios propios (`CUSTOM`).
- Idioma `en` (solo `es`).
- Reintento manual en UI (solo automático al abrir).
- Cambios de esquema Room (sin migración).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/exercises` (picker + `ExercisesViewModel`, solo lectura) y la capa de datos (`CatalogConfig`, `CatalogSync`, `ExerciseApi` + DTO, `ExerciseDao`, repos). Flujo `UI → ViewModel (StateFlow) → Repository → Room`; Retrofit solo rellena (ver `data/repository/CatalogSync.kt:28-32`). Fuente de datos: Room. Causa del vacío: `core/network/CatalogConfig.kt:18` apunta a un tag inexistente → 404 tragado en silencio. El único uso del `getById` síncrono está en `data/repository/FakeRoutineRepository.kt:135` (contexto suspendido ya).
## 4. Requisitos funcionales + no-funcionales
- Con red y tag publicado, el picker lista el catálogo del fork.
- Sin red, el picker abre con lo guardado y no se cae.
- Un 404/tag inexistente no guarda tag y queda diagnosticable (no silencio total).
- Solo se tocan filas `CATALOG`; las `CUSTOM` nunca se pisan.
- `getById` es `suspend` en la interfaz, el fake y el real (misma forma).
- GIFs solo URL (Coil cachea); tag fijo, nunca rama.
## 5. Criterios de aceptación verificables
- Con red el picker muestra los ejercicios del fork tras abrir la app.
- Sin red el picker abre con lo guardado y no se cae.
- Con tag inexistente no se guarda tag y el fallo es visible en logs.
- `getById` no lanza excepción en el repo real.
- `./gradlew assembleDebug` termina OK.
- `CatalogMappingTest` en verde con entrada real del JSON.
- En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/exercises/`: sin cambios visuales ni de firma; solo recibe datos.
- `core/network/CatalogConfig.kt`: repo del fork + tag publicado por ti (único fichero a cambiar al retaggear).
- `data/repository/CatalogSync.kt`: misma firma; separa sin-red (reintenta luego) de HTTP 404 (avisa por log, no guarda tag).
- `data/repository/ExerciseRepository`: `getById` a `suspend`; `FakeExerciseRepository` igual cuerpo; `RoomExerciseRepository` usa el DAO y pierde la excepción + `getByIdSuspend`.
- Sets: no se toca nada (`weightKg` con signo, `loadNote`, `restSeconds` intactos).
## 7. Plan de tareas
1. Publicar tag en el fork y verificar URL jsDelivr (tú, manual).
2. Pineado en `core/network/CatalogConfig.kt`.
3. Diagnóstico 404 vs sin red en `data/repository/CatalogSync.kt`.
4. `getById` a `suspend` en `data/repository/FakeExerciseRepository.kt` + `RoomExerciseRepository.kt`.
5. Entrada real en `CatalogMappingTest`.
6. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"` + prueba en móvil físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK.
- `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"` en verde.
- En móvil físico: con red el picker se llena; sin red abre con lo local.
- Edge-to-edge sin cambios (picker ya lo cumple).
## 9. Riesgos / No romper
- No cambiar `applicationId`; misma keystore + subir `versionCode`.
- Siempre `@Upsert`, nunca borrado-y-recreado.
- La UI solo ve `domain/model`, nunca DTOs.
- `gradle-wrapper.jar` ausente: abrir primero en Android Studio.
- Si el JSON real trae claves distintas a `ExerciseDto`, adaptar el DTO en este spec (con test).
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Ejercicios propios `CUSTOM` (crear/editar desde la app).
- Idioma `en` del catálogo.
- Reintento manual en picker vacío.
- Pulido feed/historial tras sesiones (specs 008/009).
## Preguntas abiertas
- ¿Tag definitivo del fork (v1.1.0 u otro)?
- ¿Las `gif_url` del JSON son absolutas o relativas al tag?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Con red el picker muestra los ejercicios del fork tras abrir la app.
- [x] Sin red el picker abre con lo guardado y no se cae.
- [x] Con tag inexistente no se guarda tag y el fallo es visible en logs.
- [x] `getById` no lanza excepción en el repo real.
- [x] `./gradlew assembleDebug` termina OK.
- [x] Prueba `CatalogMappingTest` en verde.
- [ ] En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
