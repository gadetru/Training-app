# Blindaje visual + tests espejo + auto-reparación
Estado: aprobado
Depende de: specs/010-detalle-ejercicio-spec.md (asume ficha + DB v2 + ruta exerciseDetail ya en rama 010)
Fecha de creación: 2026-10-08
Descripción: Pasar toda la UI a colores y medidas con nombre, reordenar los tests en espejo por paquete y regla, añadir foto de referencia solo para Home y su tarjeta, y dejar una puerta que impide mezclar cambios rotos más un agente que repara solo hasta dejar todo en verde.
## 1. Objetivo
Que nadie cambie sin querer un color o un tamaño.
Que cada regla tenga su prueba en su carpeta espejo.
Que Home y su tarjeta tengan foto de referencia.
Que un fallo bloquee el merge y el agente lo repare solo.
## 2. Alcance (entra / no entra)
Entra:
- Pasar toda la UI a botes con nombre en `ui/theme/` (colores y medidas actuales como verdad).
- Crear pruebas espejo en `src/test/` (pintura con JVM + receta con falsos en memoria) para theme, home, routines y workout.
- Pruebas de base en memoria para ejercicios y migración en `src/androidTest/`.
- Foto de referencia con Roborazzi/Shot solo para Home y tarjeta de rutina.
- Puerta en cada merge y borrador de revisión futura por la tarde.
- Regla de auto-reparación del agente hasta dejar todo en verde.
No entra:
- Ejercicios propios CUSTOM ni historial real.
- Iconos extended ni fuentes Outfit/Jakarta.
- Catálogo en inglés ni reintento manual del picker.
- Foto de referencia de editar rutina, sesión o detalle.
- Cambiar proveedor del catálogo, JSON ni reglas de series.
- Añadir dependencias Gradle por cuenta propia (solo proponer).
## 3. Contexto arquitectura (features, capas, Room, catálogo, DTOs)
Toca `ui/theme/` y `feature/home, routines, workout, exercises` (cada pantalla con su ViewModel de caja observable).
Flujo que no cambia: pantalla → ViewModel con datos observables → repositorio → base local; la red solo rellena (ver `data/repository/CatalogSync.kt:28`).
La base es Room v2 con esquema exportado y migración 1→2 (ver `data/local/AppDatabase.kt:30-42,59-62`).
El tema actual es plantilla morada (ver `ui/theme/Color.kt:5-11`, `ui/theme/Theme.kt:14-34`) y cada pantalla lleva sus colores sueltos (ver `feature/home/HomeScreen.kt:67-75`, `feature/home/PlaceholderTabs.kt:29-32`).
Las pruebas actuales están amontonadas en la raíz (ver `app/src/test/.../CatalogMappingTest.kt:20-165`, `app/src/androidTest/.../HomeNavTest.kt:25-33`).
Fuente de datos: la base local; los GIF son solo direcciones que Coil guarda en caché (ver `core/network/CatalogConfig.kt:14-25`).
## 4. Requisitos funcionales + no-funcionales
- Toda la UI usa botes con nombre; no queda color ni medida suelta en home, routines, workout ni exercises.
- Cada prueba vive en su carpeta espejo por paquete y regla.
- Las medidas y colores tienen prueba rápida en JVM.
- Las reglas de posición, descarte seguro y relleno con la última sesión tienen prueba en memoria.
- La base de ejercicios y la migración tienen prueba en memoria en dispositivo.
- Home y tarjeta tienen foto de referencia que falla si cambia un píxel.
- Un test en rojo bloquea el merge.
- El agente repara solo hasta verde o se detiene tras dos intentos y reporta.
- La puerta corre en emulador y en físico de forma automática.
- Solo la base local manda en la UI; la red solo rellena.
- La UI solo ve el modelo de dominio, nunca tablas ni descargas.
## 5. Criterios de aceptación verificables
- Abrir Home muestra los colores y medidas de los botes con nombre.
- Cambiar un color a mano hace fallar su prueba JVM.
- Cambiar un tamaño a mano hace fallar su prueba JVM.
- Romper la posición hace fallar su prueba de reglas.
- Romper el descarte hace fallar su prueba de reglas.
- Romper el relleno con la última sesión hace fallar su prueba.
- Cambiar Home o su tarjeta hace fallar su foto de referencia.
- Un fallo bloquea el merge hasta repararse.
- El agente deja todo en verde o reporta tras dos intentos.
- `./gradlew assembleDebug` termina OK.
- `./gradlew testDebugUnitTest` termina en verde.
- En emulador y en físico nada queda bajo la hora ni la barra inferior.
## 6. Diseño (features, ViewModels, Repository, Room, catálogo, sets)
- `ui/theme/`: botes de colores con los hex actuales, fichero nuevo de medidas, objeto `Theme` que expone colores y medidas; las pantallas dejan de crear colores sueltos.
- `feature/home, routines, workout, exercises`: misma cara por fuera, solo cambian a usar los botes; ViewModels igual con caja observable.
- `src/test/.../ui/theme/`: pruebas de colores y medidas.
- `src/test/.../data/mapper/`: la prueba actual de conversión movida a su espejo sin cambiar lógica.
- `src/test/.../data/repository/`: reglas de posición y descarte, y de relleno con la última sesión y cierre, con falsos en memoria.
- `src/androidTest/.../data/local/dao/`: ejercicios y migración con base en memoria hermética y corrutinas de prueba.
- `src/androidTest/.../feature/`: smokes actuales movidos a su espejo más dos fotos Roborazzi/Shot (Home y tarjeta).
- Catálogo: tag fijo nunca rama, solo filas CATALOG, GIF solo dirección.
- Series: no se toca peso con signo, nota libre, descanso por serie ni su copia en pantalla.
## 7. Plan de tareas
1. Crear botes de colores y medidas en `ui/theme/Color.kt` y `ui/theme/Dimensions.kt`.
2. Exponer botes en `ui/theme/Theme.kt`.
3. Pasar `feature/home/HomeScreen.kt` y `feature/home/PlaceholderTabs.kt` a los botes.
4. Pasar `feature/routines/RoutineEditScreen.kt` a los botes.
5. Pasar `feature/workout/WorkoutScreen.kt` a los botes.
6. Pasar `feature/exercises/ExercisePickerSheet.kt` y `feature/exercises/ExerciseDetailScreen.kt` a los botes.
7. Crear pruebas de botes en `src/test/.../ui/theme/AppColorsTest.kt` y `AppDimensTest.kt`.
8. Mover la prueba de conversión a `src/test/.../data/mapper/`.
9. Crear reglas de posición y descarte en `src/test/.../data/repository/RoutineRulesTest.kt`.
10. Crear relleno y cierre en `src/test/.../data/repository/WorkoutPrefillTest.kt`.
11. Crear base en memoria de ejercicios en `src/androidTest/.../data/local/dao/ExerciseDaoTest.kt`.
12. Crear prueba de migración en `src/androidTest/.../data/local/Migration12Test.kt`.
13. Mover smokes a `src/androidTest/.../feature/home|routines|workout/`.
14. Añadir fotos Roborazzi/Shot de Home y tarjeta más doc de regeneración.
15. Dejar puerta de merge y borrador de revisión de tarde con hora pendiente.
16. Verificación = `./gradlew assembleDebug` OK + `./gradlew testDebugUnitTest` verde + `connectedDebugAndroidTest` en ambos dispositivos si toca UI o base.
## 8. Verificación (assembleDebug + CatalogMappingTest si aplica + prueba en móvil físico)
- `./gradlew assembleDebug` OK sin dependencias nuevas sin aprobar.
- `./gradlew testDebugUnitTest` en verde (incluye la prueba de conversión movida).
- `./gradlew connectedDebugAndroidTest` en emulador Pixel 6 API 34 y en físico de forma automática si toca UI o base.
- En ambos: Home y tarjeta iguales a su foto; cabeceras con `statusBarsPadding`, docks con `navigationBarsPadding`, sin dp fijos extra.
- Proponer líneas Roborazzi/Shot, room-testing y corrutinas de prueba y esperar (las confirma Android Studio).
## 9. Riesgos / No romper
- No crear ni editar `gradlew`, `settings.gradle.kts`, `gradle.properties`, `libs.versions.toml` ni `build.gradle.kts`; proponer líneas y esperar.
- No cambiar `applicationId`; misma keystore fuera del repo + subir `versionCode` al distribuir.
- Siempre `@Upsert`, nunca borrado-y-recreado; cambio de esquema exige subir versión + migración real.
- La UI solo ve `domain/model`, nunca tablas ni DTOs; conversiones en `data/mapper`.
- No borrar los `Fake*Repository` aún: son deuda hasta que dejen de estar en uso.
- `gradle-wrapper.jar` ausente: abrir primero en Android Studio.
- Las fotos son frágiles (modo oscuro, fuentes): cambio legal obliga a regenerar la foto.
- Auto-reparación con tope de dos intentos; sin verde se para y reporta, no toca el spec.
## 10. Futuros specs (mejoras detectadas, errores extra, ideas — no entran aquí)
- Revisión programada de tarde con hora y qué corre en cada sitio (pendiente de este spec).
- Fotos de editar rutina, sesión y detalle (hoy solo Home y tarjeta).
- Ejercicios propios CUSTOM con formulario (ver `README.md:156`).
- Historial y progreso reales (ver `README.md:157`).
- `material-icons-extended` o mantener fallbacks (ver `feature/home/HomeScreen.kt:67-82`).
- Tipografías Outfit/Jakarta/Space Grotesk (ver `README.md:167`).
- Catálogo `en` + reintento manual del picker vacío.
- Borrar `Fake*Repository` cuando dejen de estar en uso (ver `AGENTS.md:28-29`).
- Sincronización Fase 2 con Spring/MySQL usando `updatedAt` + `deleted`.
## Preguntas abiertas
- ¿Los hex actuales son la verdad o hay paleta nueva?
- ¿Hora exacta de la revisión de tarde y qué corre en cada sitio?
- ¿Tope de auto-reparación en dos intentos o más?
- ¿Fotos solo en oscuro (`MainActivity.kt:71` fuerza oscuro) o también en claro?
## 11. Checklist verificación (última, checkboxes listos para /verifier)
- [ ] Abrir Home muestra los colores y medidas de los botes con nombre.
- [ ] Cambiar un color a mano hace fallar su prueba JVM.
- [ ] Cambiar un tamaño a mano hace fallar su prueba JVM.
- [ ] Romper la posición hace fallar su prueba de reglas.
- [ ] Romper el descarte hace fallar su prueba de reglas.
- [ ] Romper el relleno con la última sesión hace fallar su prueba.
- [ ] Cambiar Home o su tarjeta hace fallar su foto de referencia.
- [ ] Un fallo bloquea el merge hasta repararse.
- [ ] El agente deja todo en verde o reporta tras dos intentos.
- [ ] `./gradlew assembleDebug` termina OK.
- [ ] `./gradlew testDebugUnitTest` termina en verde.
- [ ] En emulador y en físico nada queda bajo la hora ni la barra inferior.
