# Calendario: asignar rutina al día + progreso real
Estado: aprobado
Depende de: specs/009-sesion-persistida-spec.md (las sesiones finalizadas con `endedAt` son la fuente de "hecho") y specs/008-rutinas-feed-spec.md (el feed de rutinas es lo que se asigna a cada día)
Fecha de creación: 2026-10-12
Descripción: El tab Calendario deja apuntar qué rutina toca cada día y marca sola la que se completó. El tab Progreso deja de inventar cifras y calcula racha, sesiones, minutos y semana real desde las sesiones guardadas.
## 1. Objetivo
Apuntar rutinas a días del mes y ver de un vistazo qué se hizo. Las cifras de progreso salen de la base local, no de fijos.
## 2. Alcance (entra / no entra)
Entra:
- Rejilla mensual: pulsar un día abre la lista de rutinas creadas y lo apunta; pulsar un día apuntado permite cambiar o quitar.
- Día marcado como hecho solo cuando hay sesión finalizada de esa rutina ese día (el cierre con Finalizar de la sesión es lo que marca, nada manual).
- Días pasados con sesión finalizada muestran su hecho aunque no se apuntaran antes (historial).
- Progreso real: racha de días seguidos, sesiones de la semana (lunes-domingo), minutos sumados y semana L–D con lo entrenado.
- Etiqueta "Hoy / Ayer / Hace X días" real en cada tarjeta de Home.
- Botón `Ver mes` de Home abre el tab Calendario.
No entra:
- Ejercicios propios CUSTOM (aparcado, va después).
- Horas de aviso ni notificaciones de "hoy toca X".
- Gráficas de volumen ni comparativa con semanas anteriores.
- Rutinas repetitivas automáticas (cada apunte es de un día concreto).
- Cambio de objetivo semanal (sigue fijo en 4).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `feature/home` (tarjetas + `Ver mes` en `HomeScreen.kt`, quita fijos de `RoomHomeRepository.kt:35,38-43`) y crea `feature/history/` (pantalla mes + pantalla progreso con su caja observable cada una, según `docs/ARQUITECTURA.md:58-61`); los marcadores `CalendarPlaceholder/ProgressPlaceholder` (`feature/home/PlaceholderTabs.kt:36-51`) se borran. Flujo sin cambios: pantalla → caja observable del ViewModel → repositorio → base local; la red no pinta aquí. Hace falta una tabla nueva de apuntes (día + rutina) con borrado lógico; lo "hecho" se deduce de las sesiones finalizadas (`endedAt` distinto de nulo, ver `data/repository/RoomWorkoutRepository.kt:158-166`), no se guarda aparte. Catálogo, DTOs y series no se tocan.
## 4. Requisitos funcionales + no-funcionales
- El mes muestra el nombre de la rutina apuntada en cada día; el día de hoy se distingue.
- Apuntar, cambiar y quitar apunte confirman en la misma vista sin salir del mes.
- Un día con sesión finalizada muestra hecho (✓ + nombre de la rutina hecha); si además había apunte de otra rutina, manda lo hecho.
- Sin rutinas creadas, el selector lo dice y lleva al constructor.
- Racha = días seguidos con al menos una sesión finalizada hasta hoy (si hoy aún no se entrenó, cuenta desde ayer).
- Semana = lunes-domingo natural; minutos = suma de fin menos inicio de cada sesión finalizada.
- Etiqueta por rutina: `Hoy`, `Ayer`, `Hace X días`, nada si nunca se hizo.
- Pantallas con botes de `ui/theme/` y reglas edge-to-edge (cabecera con `statusBarsPadding`, sin dp extra).
## 5. Criterios de aceptación verificables
- Apuntar "Torso" al día 15 muestra "Torso" en la casilla del 15.
- Cambiar el apunte del 15 a "Pierna" muestra "Pierna" y no duplica.
- Quitar el apunte deja la casilla vacía sin borrado de nada más.
- Finalizar la sesión de "Torso" hoy marca ✓ "Torso" en el día de hoy.
- Un día pasado con sesión finalizada y sin apunte muestra su ✓.
- La racha sube en 1 tras finalizar el primer entreno de un día nuevo.
- Progreso muestra sesiones, minutos y semana calculados (ningún 4/3/185 fijo).
- Cada tarjeta de Home muestra su "Hoy / Ayer / Hace X días" real.
- `Ver mes` abre el tab Calendario.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- `./gradlew connectedDebugAndroidTest` en verde en emulador Pixel 6 API 34 y en físico.
- Fotos Home y tarjeta iguales a sus dorados.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `feature/history/`: pantalla mes (rejilla + hoja de rutinas + quitar) y pantalla progreso (racha, sesiones, minutos, semana); cada una con ViewModel y caja observable; modelos nuevos de dominio en `domain/model/` (apunte del día, día del mes con estado).
- Repositorio nuevo de apuntes + historial (misma forma que los actuales: observar con Flow, guardar/borrar con lógico); los fakes lo imitan para los tests.
- Room: tabla `scheduled_days` (`id` UUID, `routineId`, `date` en `yyyy-MM-dd` local, `updatedAt`, `deleted`) con `@Upsert`; consultas de sesiones finalizadas por rango de fechas para el hecho, la racha y la semana. DB v2 → v3 con `MIGRATION_2_3` real (`exportSchema=true`, esquema en `app/schemas/`); `DatabaseModule.kt:32-35` registra la migración.
- `MainActivity.kt:172-187`: las rutas `calendar`/`progress` existentes muestran las pantallas nuevas en vez de los marcadores; sin rutas nuevas.
- No se toca: catálogo (`CATALOG_TAG`, DTOs, `CatalogSync`), series (`targetReps`, `weightKg`, `restSeconds`), `applicationId`, dependencias Gradle.
## 7. Plan de tareas
1. Revisión puerta: `connectedDebugAndroidTest` en emulador + físico sobre `main` (012/013/P0); en rojo se arregla antes, fuera de este spec.
2. Modelos de dominio del mes y el apunte en `domain/model/` (nuevo fichero, sin tocar `Home.kt` salvo lo necesario).
3. Entidad + DAO de `scheduled_days` + consultas de finalizadas por rango en `WorkoutSessionDao.kt`.
4. DB v3 + `MIGRATION_2_3` en `AppDatabase.kt` + registro en `DatabaseModule.kt` + esquema exportado.
5. Repositorio de apuntes/historial (+ espejo falso) y progreso real en `RoomHomeRepository.kt` (etiquetas y semana dejan de ser fijas).
6. Pantalla mes en `feature/history/` (rejilla, hoja de rutinas, cambiar/quitar, hecho ✓).
7. Pantalla progreso en `feature/history/` (racha, sesiones, minutos, semana L–D).
8. Cablear `calendar`/`progress` en `MainActivity.kt`, borrar marcadores, `Ver mes` → calendario.
9. Tests JVM del cálculo (racha, semana, etiquetas) en su espejo + DAO de apuntes + migración 2→3 en `androidTest`.
10. Smoke instrumentado del mes (apuntar, cambiar, quitar, hecho tras finalizar) en su espejo.
11. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico + dorados intactos.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK (incluye DB v3 + migración).
- `./gradlew testDebugUnitTest` verde (incluye tests nuevos de cálculo; `CatalogMappingTest` como regresión, no se toca mapper).
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico de forma automática (mes + progreso + migración).
- En ambos: apunte, hecho tras Finalizar, racha, `statusBarsPadding` sin dp extra, fotos iguales.
## 9. Riesgos / No romper
- No cambiar `applicationId`, ni keystore/`versionCode`, ni dependencias Gradle.
- Migración real v2→v3: sin `fallbackToDestructiveMigration`; las sesiones y rutinas existentes sobreviven.
- El hecho lo decide el cierre de sesión (`endedAt`), nunca un tap manual: no crear columna de "hecho" que pueda contradecir a las sesiones.
- Fechas en día local del dispositivo (`yyyy-MM-dd`); las sesiones guardan milisegundos: el rango día se calcula en repositorio, no en SQL con horas.
- Borrar una rutina no borra su historial (borrado lógico ya existente); sus apuntes futuros se ocultan.
- UI solo ve `domain/model`; conversiones en `data/mapper`.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Avisos del día ("hoy toca X") con WorkManager — idea del usuario, fuera de este spec.
- Rutinas semanales automáticas (p. ej. torso cada lunes) — detectado al fijar apunte por día concreto.
- Gráficas de volumen y comparativa entre semanas (el `volumeDeltaPct` fijo de `Home.kt:45` sigue sin calcularse) — Fase 3.
- Objetivo semanal configurable en perfil (hoy fijo en 4, `Home.kt:43`).
- Cuenta atrás para estáticos (plancha 90s) con ejercicios propios CUSTOM — aparcado por el usuario, spec posterior.
## Preguntas abiertas
- Supuesto por dismissal del interrogatorio: selector de rutina con la lista del feed ordenada por `position`; si se confirma otro orden, se ajusta al aprobar.
- Supuesto: lunes como primer día de semana y de racha (coherente con el heatmap L–D actual, `Home.kt:47`); si se quiere domingo, decirlo antes de implementar.
- ¿El día futuro con apunte muestra cuenta atrás o texto ("En 3 días") o solo el nombre? Supuesto: solo el nombre.
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [x] Apuntar "Torso" al día 15 muestra "Torso" en la casilla del 15.
- [x] Cambiar el apunte del 15 a "Pierna" muestra "Pierna" y no duplica.
- [x] Quitar el apunte deja la casilla vacía sin borrado de nada más.
- [x] Finalizar la sesión de "Torso" hoy marca ✓ "Torso" en el día de hoy.
- [x] Un día pasado con sesión finalizada y sin apunte muestra su ✓.
- [x] La racha sube en 1 tras finalizar el primer entreno de un día nuevo.
- [x] Progreso muestra sesiones, minutos y semana calculados (ningún 4/3/185 fijo).
- [x] Cada tarjeta de Home muestra su "Hoy / Ayer / Hace X días" real.
- [x] `Ver mes` abre el tab Calendario.
- [x] `./gradlew assembleDebug` termina OK.
- [x] `./gradlew testDebugUnitTest` termina en verde.
- [x] `./gradlew connectedDebugAndroidTest` en verde en emulador y en físico.
- [x] Fotos Home y tarjeta iguales a sus dorados.
