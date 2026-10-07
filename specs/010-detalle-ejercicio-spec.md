# Detalle de ejercicio con instrucciones
Estado: aprobado
Depende de: specs/007-catalogo-fork-spec.md, specs/008-rutinas-feed-spec.md, specs/009-sesion-persistida-spec.md (asume catálogo y rutinas y sesiones estables en main)
Fecha de creación: 2026-10-06
Descripción: Al pulsar un ejercicio se abre su ficha con GIF grande, secundarios e instrucciones paso a paso. Rescata las instrucciones que hoy se pierden entre la descarga y la pantalla.
## 1. Objetivo
Que cada ejercicio tenga ficha propia que abra sin fallar.
La ficha muestra lo guardado en el móvil: imagen, datos e instrucciones.
Sin red abre con lo local y nunca se cae.
## 2. Alcance (entra / no entra)
Entra:
- Pantalla nueva en la ruta exerciseDetail con el id del ejercicio.
- Abrir la ficha desde el buscador, desde editar rutina y desde la sesión.
- GIF grande con Coil más nombre, músculo, equipo y secundarios.
- Instrucciones numeradas en español con lo guardado en la base.
- Guardar instrucciones en el modelo, la tabla y las conversiones.
- Subir la base de v1 a v2 con migración real y esquema exportado.
- Volver a descargar el catálogo con el mismo tag para rellenar instrucciones.
- Ampliar la prueba de conversión con una entrada real con instrucciones.
No entra:
- Crear o editar ejercicios propios CUSTOM.
- Idioma inglés del catálogo.
- Botón de reintento manual cuando la lista sale vacía.
- Editar instrucciones desde la app.
- Cambiar series, pesos, descansos, notas o su comportamiento en pantalla.
- Añadir librerías nuevas en Gradle.
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca la lista de ejercicios y la capa de datos.
La lista actual solo muestra filas sin ficha (ver ExercisePickerSheet.kt:218-224) y su ViewModel solo filtra (ver ExercisesViewModel.kt:52-73).
El flujo es pantalla → ViewModel con datos observables → repositorio → base local; la red solo rellena (ver CatalogSync.kt:30-58).
La descarga trae instrucciones (ver ExerciseDto.kt:22) pero el modelo de la app las pierde (ver Exercise.kt:19-38) y la tabla también (ver ExerciseEntity.kt:15-28).
La lectura por id ya existe y espera (ver ExerciseDao.kt:19-20, RoomExerciseRepository.kt:21-22).
La navegación aún no tiene ficha de ejercicio (ver MainActivity.kt:82-176).
Fuente de datos: la base local; los GIF son solo direcciones que Coil guarda en caché.
## 4. Requisitos funcionales + no-funcionales
- Pulsar una fila de ejercicio abre la ficha de ese ejercicio.
- La ficha muestra GIF grande, nombre, músculo, equipo y secundarios.
- La ficha muestra las instrucciones numeradas en español.
- Sin instrucciones guardadas muestra aviso corto y no se cae.
- Sin red la ficha abre con lo guardado en el móvil.
- Volver atrás cierra la ficha sin perder ni duplicar nada.
- Sin red funciona todo salvo el GIF, que usa la caché de Coil.
- La pantalla solo usa el modelo de dominio, nunca tablas ni descargas.
- La cabecera deja libre la zona de hora y el pie la barra del sistema.
## 5. Criterios de aceptación verificables
- Pulsar un ejercicio del buscador abre su ficha sin fallar.
- La ficha muestra el GIF grande del ejercicio.
- La ficha muestra secundarios más músculo y equipo.
- La ficha muestra las instrucciones en orden y completas.
- Sin red la ficha abre con lo guardado y no se cae.
- Volver atrás y rotar conserva la ficha sin duplicar ni perder.
- Tras actualizar, los ejercicios viejos ya muestran instrucciones.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- feature/exercises: pantalla nueva ExerciseDetailScreen más ExerciseDetailViewModel que observa un ejercicio por id.
- MainActivity: ruta nueva exerciseDetail con el id; pulsaciones desde buscador, editar rutina y sesión.
- domain/model/Exercise: lista nueva de instrucciones en español.
- data/local/entity/ExerciseEntity: columna nueva de instrucciones con el conversor JSON actual.
- data/mapper más DTO: pasan las instrucciones de la descarga al modelo y a la tabla.
- AppDatabase: versión 2 con migración 1 a 2 y esquema exportado en app/schemas.
- CatalogSync: misma forma; al migrar se olvida el tag guardado para descargar otra vez con el mismo tag fijo.
- RoomExerciseRepository.getById ya sirve sin cambios de forma; el falso añade instrucciones de ejemplo.
- Catálogo: tag fijo nunca rama, solo filas CATALOG, GIF solo dirección con caché de Coil.
- Series: no se toca peso con signo, nota libre, descanso por serie ni su copia en pantalla.
## 7. Plan de tareas
1. Añadir instrucciones al modelo en domain/model/Exercise.kt.
2. Añadir la columna en data/local/entity/ExerciseEntity.kt.
3. Pasar instrucciones en data/mapper/ExerciseMapper.kt y data/remote/dto/ExerciseDto.kt.
4. Subir la base a v2 con migración en data/local/AppDatabase.kt.
5. Olvidar el tag para re-descargar en core/datastore/CatalogTagStore.kt y data/repository/CatalogSync.kt.
6. Crear el ViewModel en feature/exercises/ExerciseDetailViewModel.kt.
7. Crear la pantalla en feature/exercises/ExerciseDetailScreen.kt.
8. Añadir ruta y pulsaciones en MainActivity.kt y ExercisePickerSheet.kt y RoutineEditScreen.kt y WorkoutScreen.kt.
9. Ampliar la prueba con instrucciones en CatalogMappingTest.
10. Verificación = ./gradlew assembleDebug OK más prueba de conversión más móvil físico.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- ./gradlew assembleDebug OK.
- ./gradlew testDebugUnitTest --tests "*CatalogMappingTest*" en verde.
- En móvil físico: pulsar abre la ficha, GIF e instrucciones visibles, secundarios correctos.
- En móvil físico: sin red abre con lo local; rotación, gestos y 3 botones sin solapes.
- Edge-to-edge sin cambios salvo la cabecera nueva con margen de estado.
## 9. Riesgos / No romper
- No cambiar applicationId; misma keystore fuera del repo más subir versionCode.
- Siempre actualizar con Upsert, nunca borrar y recrear.
- La pantalla solo ve el modelo de dominio; conversiones en data/mapper.
- Sin migración se pierden datos al subir la versión de la base.
- No pisar ejercicios propios CUSTOM en la re-descarga.
- gradle-wrapper.jar ausente: abrir primero en Android Studio.
- Sin emulador; probar solo en móvil físico.
- No añadir dependencias Gradle; Coil ya sirve para el GIF grande.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Ejercicios propios CUSTOM con formulario (ver README.md:154).
- Idioma inglés del catálogo (ver CatalogConfig.kt:22).
- Reintento manual cuando la lista sale vacía (ver ExercisePickerSheet.kt:197-209).
- GIF animado real con coil-gif (ver ExercisePickerSheet.kt:392-399).
- Abrir la ficha desde el historial cuando exista (ver README.md:155).
- Tema propio y tipografías Outfit y Jakarta (ver README.md:164).
## Preguntas abiertas
- ¿El olvido del tag se hace borrando el tag guardado o con una marca de esquema?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Pulsar un ejercicio del buscador abre su ficha sin fallar.
- [ ] La ficha muestra el GIF grande del ejercicio.
- [ ] La ficha muestra secundarios más músculo y equipo.
- [ ] La ficha muestra las instrucciones en orden y completas.
- [ ] Sin red la ficha abre con lo guardado y no se cae.
- [ ] Volver atrás y rotar conserva la ficha sin duplicar ni perder.
- [ ] Tras actualizar, los ejercicios viejos ya muestran instrucciones.
- [x] ./gradlew assembleDebug termina OK.
- [x] Prueba CatalogMappingTest en verde.
- [ ] En móvil con gestos y con 3 botones nada queda bajo hora ni barra inferior.
