# Fase B Room local
Estado: aprobado
Depende de: specs/001-plantilla-usuario-spec.md, specs/002-vista-principal-spec.md, specs/003-lista-ejercicios-spec.md, specs/004-editar-rutina-spec.md, specs/005-rutina-spec.md (mantienen la forma de los repos falsos y el arranque ¿hay perfil? home:profile)
Fecha de creación: 2026-10-04
Descripción: Guardar perfil, ejercicios, rutinas y sesiones en la base local del móvil. La red solo rellena esa base. Por fuera la app se ve igual.
## 1. Objetivo
Persistir todo lo de Fase A en Room v1 sin cambiar la UI por fuera.
Los repos reales mantienen la misma forma que los falsos actuales.
El arranque usa la fila de perfil guardada, no un valor en memoria.
Sin red la app sigue con lo guardado.
La descarga del catálogo solo actualiza filas de catálogo.
## 2. Alcance (entra / no entra)
Entra:
- Base Room v1 con perfil, ejercicios, rutinas, series planificadas, sesiones y series hechas en `data/local/`.
- Repos reales con la misma forma que los falsos (`data/repository/`).
- Criterio de arranque real con fila de perfil en `MainActivity.kt`.
- Guardado de tag de catálogo con DataStore en `core/datastore/`.
- Descarga `GET api/{lang}/exercises.json` con tag fijo jsDelivr en `data/remote/`.
- Conversión DTO/Entidad a modelo limpio en `data/mapper/` + prueba `CatalogMappingTest`.
- Inyección con Hilt en `TrainingApp.kt` + `core/di/` (proponer dependencia y esperar).
No entra:
- Pulidos finos de UI pendientes de Fase A (se hacen con datos reales después).
- Cambio a `material-icons-extended` (requiere propuesta Gradle aparte).
- Backend Spring/MySQL ni sincronización (Fase D).
- Tabla propia de accesorios reutilizables (idea de `docs/MODELO_DE_DATOS.md`).
- Tipografías Outfit/Plus Jakarta/Space Grotesk.
- Migración v2 (solo se deja la estrategia lista).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca las 5 vistas: perfil, home, ejercicios, constructor de rutina y sesión en vivo (ver `MainActivity.kt:61-81`).
Flujo nuevo: pantalla Compose → ViewModel con estado observable → repositorio → Room; la red nunca pinta, solo rellena Room (ver `docs/ARQUITECTURA.md:5-17`).
Misma forma que el real: las interfaces `ExerciseRepository`, `RoutineRepository` y `WorkoutRepository` que los falsos ya cumplen (ver `FakeExerciseRepository.kt:16-19`, `FakeRoutineRepository.kt:39-79`, `FakeWorkoutRepository.kt:41-63`).
Borrado lógico: no se borra la fila, se marca con `updatedAt` + `deleted` para futura sincronización (ver `domain/model/Routine.kt:23-31`).
Relleno con la última sesión: al abrir una sesión se copian los valores de la última vez de ese ejercicio en ese día, si no los planificados (ver `docs/MODELO_DE_DATOS.md:81-87`).
Catálogo: fork por jsDelivr con tag fijo, solo URLs de GIF en base (Coil cachea), actualización solo toca filas `source=CATALOG` (ver `FakeExerciseRepository.kt:43-46`).
DTOs espejo del JSON con nombres con guion bajo (`body_part`, `gif_url`) en `data/remote/dto/`.
## 4. Requisitos funcionales + no-funcionales
- Perfil único `me` se guarda y sobrevive a reinicios.
- Ejercicios de catálogo conservan su `id` `músculo/slug`; los propios usan UUID.
- Rutinas y series planificadas guardan posición, peso con signo, nota libre y descanso por serie.
- Sesiones guardan lo hecho con enlace a la serie planificada si venía de rutina.
- Estado de pantalla (`expanded`, celdas tocadas a mano, pausa del cronómetro) no se guarda.
- Descarga de catálogo solo si el tag cambia; sin red se usa lo local.
- Solo SQLite es fuente de la UI; GIFs solo URL.
- Base v1 con esquema exportado; borrado destructivo solo en desarrollo.
- Siempre guardar con `@Upsert`, nunca con borrado-y-recreado.
- Sin Hilt manual en ViewModels: misma firma, solo cambia la inyección.
## 5. Criterios de aceptación verificables
- Perfil editado sigue ahí tras cerrar y abrir la app en el móvil.
- Rutina creada con 2 ejercicios y 3 series sigue intacta tras reinicio.
- Sesión terminada aparece en el historial tras reinicio.
- Próxima sesión del mismo día rellena con lo hecho la última vez, no con el plan.
- Sin red la lista de ejercicios abre con lo guardado y no se cae.
- Con red y tag nuevo el catálogo se actualiza sin duplicar propios.
- Propios nunca se pisan por una actualización del catálogo.
- `./gradlew assembleDebug` termina OK.
- Prueba `CatalogMappingTest` en verde si toca conversión.
- En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/profile/`: `ProfileScreen` igual por fuera, ViewModel lee/escribe fila `me` vía repo.
- `feature/home/`: `HomeScreen` + dock iguales, feed desde rutinas guardadas en `MainActivity.kt:68-82`.
- `feature/exercises/`: `ExercisePickerSheet` + `ExercisesViewModel.kt` igual por fuera, repo real filtra en Room.
- `feature/routines/`: `RoutineEditScreen` + `RoutineEditViewModel` iguales, borrador/guardada ahora en Room con UUID.
- `feature/workout/`: `WorkoutScreen` + `WorkoutViewModel` iguales, inicio con relleno con la última sesión y cierre sin sesiones fantasma.
- Room `data/local/entity/`: 7 tablas espejo del dominio (`Profile`, `Exercise`, `Routine`, `RoutineExercise`, `PlannedSet`, `WorkoutSession`, `SetEntry`) con `updatedAt` + `deleted`.
- Room `data/local/dao/`: un acceso por tabla con `@Upsert` y observaciones por flujo.
- Room `AppDatabase.kt` v1 + `Converters.kt` + carpeta `app/schemas/` con esquema exportado.
- Repos `data/repository/`: 4 reales con la misma forma que los falsos, conversión en `data/mapper/`.
- Catálogo `core/network/CatalogConfig.kt`: tag fijo + base jsDelivr, nunca rama.
- Catálogo `core/datastore/`: guarda último tag aplicado.
- Catálogo `data/remote/`: `ExerciseApi.kt` + DTOs con `body_part` y `gif_url`.
- Sets: `weightKg` con signo (+ lastre, 0 corporal, - ayuda), `loadNote` libre y `restSeconds` por serie se guardan tal cual; la copia de la 1ª y la propagación a no tocadas siguen solo en pantalla.
## 7. Plan de tareas
1. Crear entidades espejo del dominio en `data/local/entity/`.
2. Crear accesos con `@Upsert` en `data/local/dao/`.
3. Crear `AppDatabase.kt` v1 + `Converters.kt` + `app/schemas/`.
4. Crear conversiones Entidad a modelo en `data/mapper/`.
5. Crear repos reales con la misma forma que los falsos en `data/repository/`.
6. Crear DTOs espejo del JSON en `data/remote/dto/` + `ExerciseApi.kt`.
7. Crear tag fijo y prefs de tag en `core/network/CatalogConfig.kt` + `core/datastore/`.
8. Crear sincronización que solo actualiza filas `CATALOG` en `data/repository/`.
9. Añadir prueba de conversión en `CatalogMappingTest`.
10. Crear app Hilt en `TrainingApp.kt` + módulos en `core/di/` (proponer dependencia y esperar).
11. Pasar los 5 ViewModels a inyección en `feature/*/`.
12. Cambiar arranque a fila de perfil real en `MainActivity.kt`.
13. Verificación = `./gradlew assembleDebug` OK (+ `CatalogMappingTest` + prueba en móvil físico).
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK sin dependencias nuevas sin aprobar.
- `./gradlew testDebugUnitTest --tests "*CatalogMappingTest*"` en verde.
- En móvil físico: perfil, rutina, sesión y catálogo sobreviven a reinicio; sin red abre con lo local.
- En móvil con gestos y con 3 botones: cabeceras con `statusBarsPadding`, docks con `navigationBarsPadding`, sin dp fijos extra.
## 9. Riesgos / No romper
- No crear ni editar `gradlew`, `settings.gradle.kts`, `gradle.properties`, `libs.versions.toml` ni `build.gradle.kts`; proponer líneas Room/Hilt/DataStore/Retrofit y esperar.
- No cambiar `applicationId`; misma keystore fuera del repo + subir `versionCode`.
- No usar borrado-y-recreado en Room; cambio de esquema exige subir versión + migración real.
- La UI solo ve `domain/model`, nunca entidades ni DTOs.
- `gradle-wrapper.jar` ausente: abrir primero en Android Studio.
- Spec grande: si se atasca, partir en `007-catalogo-remote` y dejar aquí solo Room local con semilla.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Pulidos finos de UI de Fase A con datos reales (detectado en debate 2026-10-04).
- `material-icons-extended` para iconos exactos del diseño en `HomeScreen.kt:69-78`.
- Partir catálogo remoto a `specs/007-catalogo-remote-spec.md` si `006` se vuelve gigante.
- Tabla propia de accesorios reutilizables en `docs/MODELO_DE_DATOS.md:105`.
- Tipografías Outfit/Plus Jakarta/Space Grotesk (aplazadas en `HomeScreen.kt:57-58`).
- Calendario e historial reales tras sesiones persistidas (`PlaceholderTabs` actual).
- Sincronización Fase 2 con Spring/MySQL usando `updatedAt` + `deleted`.
## Preguntas abiertas
- ¿Partimos catálogo a `007` o va todo en este `006`?
- ¿Versiones exactas Room/Hilt/DataStore/Retrofit a proponer según `gradle/libs.versions.toml:1-12`?
- ¿Semilla inicial de ejercicios si no hay red en primer arranque?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Perfil editado sigue ahí tras cerrar y abrir la app en el móvil.
- [ ] Rutina creada con 2 ejercicios y 3 series sigue intacta tras reinicio.
- [ ] Sesión terminada aparece en el historial tras reinicio.
- [ ] Próxima sesión del mismo día rellena con lo hecho la última vez, no con el plan.
- [ ] Sin red la lista de ejercicios abre con lo guardado y no se cae.
- [ ] Con red y tag nuevo el catálogo se actualiza sin duplicar propios.
- [ ] Propios nunca se pisan por una actualización del catálogo.
- [ ] `./gradlew assembleDebug` termina OK.
- [ ] Prueba `CatalogMappingTest` en verde.
- [ ] En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
